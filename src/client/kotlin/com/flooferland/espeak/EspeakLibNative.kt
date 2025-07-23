@file:Suppress("unused", "ClassName", "FunctionName", "LocalVariableName")

package com.flooferland.espeak

import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.sun.jna.IntegerType
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * Manually (and very painfully) written native Kotlin bindings for libespeak <br/>
 * https://github.com/espeak-ng/espeak-ng/blob/master/src/include/espeak-ng/speak_lib.h
 * https://java-native-access.github.io/jna/5.17.0/javadoc/index.html
**/
interface EspeakLibNative : Library {
    /** Must be called before any synthesis functions are called.
     * @param output The audio data can either be played by eSpeak or passed back by the SynthCallback function.
     *
     * @param buflength The length in mS of sound buffers passed to the SynthCallback function.
     *          Value=0 gives a default of 60mS.
     *          This parameter is only used for AUDIO_OUTPUT_RETRIEVAL and AUDIO_OUTPUT_SYNCHRONOUS modes.
     *
     * @param path The directory which contains the espeak-ng-data directory, or `null` for the default location.
     *
     * @param options ```
     *           bit 0:  1=allow espeakEVENT_PHONEME events.
     *           bit 1:  1=espeakEVENT_PHONEME events give IPA phoneme names, not eSpeak phoneme names
     *           bit 15: 1=don't exit if espeak_data is not found (used for --help)
     * ```
     *
     * @return sample rate in Hz, or -1 (EE_INTERNAL_ERROR).
    */
    fun espeak_Initialize(
        output: Int,
        buflength: Int,
        path: String?,
        options: Int
    ): Int

    /** Synthesize speech for the specified text.  The speech sound data is passed to the calling
     *  program in buffers by means of the callback function specified by espeak_SetSynthCallback(). The command is asynchronous: it is internally buffered and returns as soon as possible. If espeak_Initialize was previously called with AUDIO_OUTPUT_PLAYBACK as argument, the sound data are played by eSpeak.
     *
     *  @param text The text to be spoken, terminated by a zero character. It may be either 8-bit characters,
     *     wide characters (wchar_t), or UTF8 encoding.  Which of these is determined by the "flags"
     *     parameter.
     *
     *  @param size Equal to (or greatrer than) the size of the text data, in bytes.  This is used in order
     *     to allocate internal storage space for the text.  This value is not used for
     *     AUDIO_OUTPUT_SYNCHRONOUS mode.
     *
     *  @param position The position in the text where speaking starts. Zero indicates speak from the
     *     start of the text.
     *
     *  @param position_type Determines whether "position" is a number of characters, words, or sentences.
     *
     *  @param end_position If set, this gives a character position at which speaking will stop.  A value
     *     of zero indicates no end position.
     *
     *  @param flags These may be OR'd together:
     *     Type of character codes, one of:
     *        espeakCHARS_UTF8     UTF8 encoding
     *        espeakCHARS_8BIT     The 8 bit ISO-8859 character set for the particular language.
     *        espeakCHARS_AUTO     8 bit or UTF8  (this is the default)
     *        espeakCHARS_WCHAR    Wide characters (wchar_t)
     *        espeakCHARS_16BIT    16 bit characters.
     *
     *     espeakSSML   Elements within < > are treated as SSML elements, or if not recognised are ignored.
     *
     *     espeakPHONEMES  Text within [[ ]] is treated as phonemes codes (in espeak's Kirshenbaum encoding).
     *
     *     espeakENDPAUSE  If set then a sentence pause is added at the end of the text.  If not set then
     *        this pause is suppressed.
     *
     *  @param unique_identifier This must be either NULL, or point to an integer variable to
     *      which eSpeak writes a message identifier number.
     *      eSpeak includes this number in espeak_EVENT messages which are the result of
     *      this call of espeak_Synth().
     *
     *  @param user_data: a pointer (or NULL) which will be passed to the callback function in
     *      espeak_EVENT messages.
     *
     *  @return EE_OK: operation achieved
     *          EE_BUFFER_FULL: the command can not be buffered;
     *            you may try after a while to call the function again.
     *      EE_INTERNAL_ERROR.
    */
    fun espeak_Synth(
        text: String,
        size: Int,
        position: UnsignedInt,
        position_type: Int,
        end_position: UnsignedInt,
        flags: UnsignedInt,
        unique_identifier: IntArray?,
        user_data: Pointer?
    ): Int

    /** Returns the espeak_VOICE data for the currently selected voice.
     *  This is not affected by temporary voice changes caused by SSML elements such as <voice> and <s>
    */
    fun espeak_Cancel(): Int

    /** Returns 1 if audio is played, 0 otherwise. */
    fun espeak_IsPlaying(): Int

    /** last function to be called.
     *  Return: EE_OK: operation achieved
     *      EE_INTERNAL_ERROR.
    */
    fun espeak_Terminate(): Int

    // Custom types for JNA
    class UnsignedInt: IntegerType {
        val value: Long

        constructor() : super(4, 0, true) {
            this.value = 0
        }
        constructor(value: Long) : super(4, value, true) {
            this.value = value
        }
        constructor(value: UInt) : this(value.toLong())

        override fun toShort(): Short {
            return value.toShort()
        }
        override fun toByte(): Byte {
            return value.toByte()
        }
    }

    // Loaded native instance :3
    companion object {
        // Loading the library
        init { System.setProperty("jna.library.path", "src/main/resources/assets/$MOD_ID/native") }
        val instance: EspeakLibNative = Native.load("libespeak-ng", EspeakLibNative::class.java)

        // Other thingies
        init { Native.setProtected(true) }
    }
}