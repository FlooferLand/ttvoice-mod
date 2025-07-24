@file:Suppress("unused")

package com.flooferland.espeak

import com.sun.jna.Pointer

// TODO: Thoroughly check everything over and make sure it's safe

/** Safe nearly 1:1  Kotlin wrapping around the native lib, so the JVM won't go KABOOM */
object Espeak {
    private val lib: EspeakLibNative = EspeakLibNative.instance

    /** @see EspeakLibNative.espeak_Initialize */
    fun initialize(output: AudioOutput = AudioOutput.Playback, bufferSize: Int = 500, path: String? = null, options: Int = 0): Result<Int> {
        val sampleRate = lib.espeak_Initialize(output.int, bufferSize, path, options)
        if (sampleRate == ErrorType.InternalError.int) {
            return Result.Failure(ErrorType.InternalError, "eSpeak initialization encountered an internal error")
        }
        return Result.Success(sampleRate)
    }

    /** @see EspeakLibNative.espeak_Synth */
    fun synth(text: String, position: Espeak.Position = Espeak.Position(), flags: UInt = 0u, uniqueId: IntArray? = null, userData: Pointer? = null): Result<Unit> {
        val status = lib.espeak_Synth(
            text, text.toByteArray().size,
            EspeakLibNative.UnsignedInt(position.start), position.type.int, EspeakLibNative.UnsignedInt(position.end), EspeakLibNative.UnsignedInt(flags),
            uniqueId, userData
        )
        when (status) {
            ErrorType.ok -> Result.EmptySuccess()
            ErrorType.BufferFull.int -> Result.Failure(ErrorType.BufferFull, "The command can not be buffered; You may try after a while to call the function again")
        }
        return Result.Failure(ErrorType.InternalError, "Internal error during synthesis")
    }

    /** @see EspeakLibNative.espeak_Cancel */
    fun cancel(): Result<Unit> {
        val status = lib.espeak_Cancel()
        if (status == ErrorType.InternalError.int) {
            return Result.Failure(ErrorType.InternalError, "eSpeak failed to cancel current playback")
        }
        return Result.Success(Unit)
    }

    /** @see EspeakLibNative.espeak_IsPlaying */
    fun isPlaying(): Boolean {
        return lib.espeak_IsPlaying() == 1
    }

    /** @see EspeakLibNative.espeak_Terminate */
    fun terminate(): Result<Unit> {
        val status = lib.espeak_Terminate()
        if (status == ErrorType.InternalError.int) {
            return Result.Failure(ErrorType.InternalError, "eSpeak failed to terminate")
        }
        return Result.Success(Unit)
    }

    /** Check the "see" section for the callback
     * @see EspeakLibNative.EspeakSynthCallback.callback */
    fun setSynthCallback(callback: (waveData: Pointer?, numberOfSamples: Int, events: Pointer?, userData: Pointer?) -> Int) {
        lib.espeak_SetSynthCallback(object : EspeakLibNative.EspeakSynthCallback {
            override fun callback(waveData: Pointer?, numberOfSamples: Int, events: Pointer?, userData: Pointer?): Int {
                return callback.invoke(waveData, numberOfSamples, events, userData)
            }
        })
    }

    /** Get the version number */
    fun getVersion(): String {
        val version = lib.espeak_Info() ?: "Unknown"
        return version
    }

    // Custom types
    data class Position(val type: PositionType = PositionType.Word, val start: UInt = 0u, val end: UInt = 0u)
    sealed interface Result<out V> {
        data class Success<out V>(val data: V) : Result<V>
        data class Failure(val error: ErrorType, val context: String? = null) : Result<Nothing>
        class EmptySuccess : Result<Unit>
    }

    // Built-in types
    enum class PositionType {
        Character,
        Word,
        Sentence;

        val int: Int
            get() = ordinal + 1
    }
    enum class AudioOutput {
        /** PLAYBACK mode: plays the audio data, supplies events to the calling program*/
        Playback,

        /** RETRIEVAL mode: supplies audio data and events to the calling program */
        Retrieval,

        /** SYNCHRONOUS mode: as RETRIEVAL but doesn't return until synthesis is completed */
        Synchronous,

        /** Synchronous playback */
        SyncPlayback;

        val int: Int
            get() = ordinal
    }
    enum class ErrorType {
        InternalError,
        BufferFull,
        NotFound;

        val int: Int
            get() = ordinal

        companion object {
            val ok: Int
                get() = 0
        }
    }
}