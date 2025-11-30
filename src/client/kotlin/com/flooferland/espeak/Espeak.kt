@file:Suppress("unused")

package com.flooferland.espeak

import com.flooferland.ttvoice.TextToVoiceClient.LOGGER
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlin.io.path.pathString

// TODO: Thoroughly check everything over and make sure it's safe

/** Safe nearly 1:1  Kotlin wrapping around the native lib, so the JVM won't go KABOOM */
object Espeak {
    private val lib: EspeakLibNative = EspeakLibNative.instance!!

    /** @see EspeakLibNative.espeak_Initialize */
    fun initialize(output: AudioOutput = AudioOutput.Playback, bufferSize: Int = 300, path: String? = EspeakLibNative.dataDir.pathString, options: Int = 0): Result<Int> {
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

    /** Lists the voices
     *
     * See [speak_lib.h](https://github.com/espeak-ng/espeak-ng/blob/master/src/include/espeak-ng/speak_lib.h) under "voice table"
     *
     * @see EspeakLibNative.espeak_ListVoices */
    fun listVoices(): List<Voice> {
        val voices = mutableListOf<Voice>()
        val arrayPointer = lib.espeak_ListVoices(null)

        val ptrSize = Native.POINTER_SIZE.toLong()
        var id = 0
        while (true) {
            val result = runCatching {
                val pointer = arrayPointer?.getPointer(id * ptrSize) ?: return@listVoices voices
                if (pointer == Pointer.NULL) return@listVoices voices
                Voice.fromPointer(pointer).getOrThrow()
            }
            result.onSuccess {
                voices.add(it)
            }
            result.onFailure { err ->
                LOGGER.error("Memory access error inside listVoices: $err")
            }
            id += 1
        }
    }

    /** Sets the voice to a known voice's name
     * @see EspeakLibNative.espeak_SetVoiceByName */
    fun setVoice(name: String): Error? {
        val status = lib.espeak_SetVoiceByName(name)
        if (status == ErrorType.InternalError.int) {
            return Error("eSpeak failed to set the voice (Internal error)")
        }
        return null
    }

    /** Gets the current voice
     * @see EspeakLibNative.espeak_GetCurrentVoice */
    fun getVoice(): Voice? {
        val pointer = lib.espeak_GetCurrentVoice()
        if (pointer == null || pointer == Pointer.NULL) return null
        return Voice.fromPointer(pointer).getOrNull()
    }

    // Custom types
    data class Position(val type: PositionType = PositionType.Word, val start: UInt = 0u, val end: UInt = 0u)
    class Voice(
        val name: String,
        val language: Language?,
        val identifier: String,
        val gender: Gender,
        val age: UByte,
        val variant: Byte,

    ) {
        companion object {
            fun fromPointer(pointer: Pointer): Result<Voice> {
                val ptrSize = Native.POINTER_SIZE.toLong()

                // Getting all the properties; JNA's Structure type sucks and did not work, so I have to do this manually
                val name = pointer.getPointer(0)?.getString(0, "UTF-8")
                val languagePointer = pointer.getPointer(ptrSize)
                val identifier = pointer.getPointer(ptrSize * 2)?.getString(0, "UTF-8")
                val gender = pointer.getByte((ptrSize * 3)).toUByte()
                val age = pointer.getByte((ptrSize * 3) + 1).toUByte()

                // Getting the language, failing if this fails; Not sure if it should continue regardless?
                val lang = runCatching {
                    if (languagePointer == null) {
                        return@runCatching null
                    }
                    val priority = languagePointer.getByte(0).toUByte()
                    val language = languagePointer.getString(1) ?: ""
                    if (language.isEmpty()) error("Language code is an empty string")
                    Language(priority, language)
                }
                lang.onFailure { err ->
                    LOGGER.error("Failed to get language for voice '$name'. Excluding the voice. Error: $err")
                    return Result.failure(err)
                }

                // Constructing a voice struct for convenience
                val voice = Voice(
                    name = name ?: "",
                    language = lang.getOrNull(),
                    identifier = identifier ?: "",
                    gender = Gender.entries.getOrNull(gender.toInt()) ?: Gender.None,
                    age = age,
                    variant = 0
                )
                return Result.success(voice)
            }
        }
    }
    enum class Gender {
        None, Male, Female
    }
    data class Language(
        val priority: UByte = 0u,
        val language: String = ""
    )

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