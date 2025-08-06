@file:Suppress("unused")

package com.flooferland.espeak

import com.sun.jna.Pointer

// TODO: Thoroughly check everything over and make sure it's safe

/** Safe nearly 1:1  Kotlin wrapping around the native lib, so the JVM won't go KABOOM */
object Espeak {
    private val lib: EspeakLibNative = EspeakLibNative.instance!!

    /** @see EspeakLibNative.espeak_Initialize */
    fun initialize(output: AudioOutput = AudioOutput.Playback, bufferSize: Int = 300, path: String? = null, options: Int = 0): Result<Int> {
        val sampleRate = lib.espeak_Initialize(output.int, bufferSize, path, options)
        if (sampleRate == ErrorType.InternalError.int) {
            return Result.failure(Error("eSpeak initialization encountered an internal error"))
        }
        return Result.success(sampleRate)
    }

    /** @see EspeakLibNative.espeak_Synth */
    fun synth(text: String, position: Espeak.Position = Espeak.Position(), flags: Int = CharFlags.Auto.b or Flags.EndPause.b, uniqueId: IntArray? = null, userData: Pointer? = null): Error? {
        val status = lib.espeak_Synth(
            text, text.toByteArray().size,
            EspeakLibNative.UnsignedInt(position.start), position.type.int, EspeakLibNative.UnsignedInt(position.end), flags,
            uniqueId, userData
        )
        return when (status) {
            ErrorType.BufferFull.int -> Error("The command can not be buffered; You may try after a while to call the function again")
            else -> null
        }
    }

    /** @see EspeakLibNative.espeak_Cancel */
    fun cancel(): Error? {
        val status = lib.espeak_Cancel()
        if (status == ErrorType.InternalError.int) {
            return Error("eSpeak failed to cancel current playback")
        }
        return null
    }

    /**
     * **NOTE:** This function **never** works for some reason; eSpeak-NG oversight?
     * @see EspeakLibNative.espeak_IsPlaying
     */
    fun isPlaying(): Boolean {
        return lib.espeak_IsPlaying() == 1
    }

    /** @see EspeakLibNative.espeak_Terminate */
    fun terminate(): Error? {
        val status = lib.espeak_Terminate()
        if (status == ErrorType.InternalError.int) {
            return Error("eSpeak failed to terminate")
        }
        return null
    }

    /** Check the "see" section for the callback
     * @see EspeakLibNative.SynthCallback.callback */
    fun setSynthCallback(callback: (wav: Pointer?, numSamples: Int, events: Pointer?) -> Int) {
        lib.espeak_SetSynthCallback(callback)
    }

    /** Get the version number */
    fun getVersion(): String {
        val version = lib.espeak_Info() ?: "Unknown"
        return version
    }

    // Custom types
    data class Position(val type: PositionType = PositionType.Word, val start: UInt = 0u, val end: UInt = 0u)

    // Built-in types
    enum class PositionType {
        Character,
        Word,
        Sentence;

        val int: Int
            get() = ordinal + 1
    }
    enum class CharFlags(val b: Int) {
        Auto(0),
        Utf8(1),
        Bit8(2),
        Wchar(3),
        Bit16(4);
    }
    enum class Flags(val b: Int) {
        SSML(0x10),
        Phonemes(0x100),
        EndPause(0x1000),
        KeepNameData(0x2000),
    }
    enum class AudioOutput {
        /** PLAYBACK mode: plays the audio data, supplies events to the calling program, and blocks the thread */
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