@file:Suppress("unused", "ClassName", "FunctionName", "LocalVariableName")

package com.flooferland.espeak

import com.flooferland.ttvoice.TextToVoiceClient
import com.flooferland.ttvoice.TextToVoiceClient.Companion.LOGGER
import com.flooferland.ttvoice.TextToVoiceClient.Companion.MOD_ID
import com.sun.jna.*
import net.minecraft.client.MinecraftClient
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString


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
        flags: Int,
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
     *  @return EE_OK: operation achieved
     *      EE_INTERNAL_ERROR.
    */
    fun espeak_Terminate(): Int

    /** Must be called before any synthesis functions are called.
     *  This specifies a function in the calling program which is called when a buffer of
     *  speech sound data has been produced.
     *
     *  The callback function is of the form:
     *     int SynthCallback(short *wav, int numsamples, espeak_EVENT *events);
    */
    fun espeak_SetSynthCallback(SynthCallback: SynthCallback)

    /** Returns the version number string.
     *  @param pathData The path to espeak_data
     */
    fun espeak_Info(pathData: Pointer? = null): String?

    /** Reads the voice files from espeak-ng-data/voices and creates an array of espeak_VOICE pointers.
     *  The list is terminated by a NULL pointer
     *
     *  If voice_spec is NULL then all voices are listed.
     *  If voice spec is given, then only the voices which are compatible with the voice_spec
     *  are listed, and they are listed in preference order.
     */
    fun espeak_ListVoices(voice_spec: Pointer?): Pointer?

    /** Searches for a voice with a matching "name" field.  Language is not considered.
     *  "name" is a UTF8 string.
     *
     *  Return: EE_OK: operation achieved
     *          EE_BUFFER_FULL: the command can not be buffered;
     *            you may try after a while to call the function again.
     *      EE_INTERNAL_ERROR.
    */
    fun espeak_SetVoiceByName(name: String?): Int;

    /** Returns the espeak_VOICE data for the currently selected voice.
     *  This is not affected by temporary voice changes caused by SSML elements such as <voice> and <s>
     */
    fun espeak_GetCurrentVoice(): Pointer?

    // Espeak types
    fun interface SynthCallback : Callback {
        /** int SynthCallback(short *wav, int numsamples, espeak_EVENT *events);
         *   @param wav is the speech sound data which has been produced.
         *     NULL indicates that the synthesis has been completed.
         *
         *   @param numSamples is the number of entries in wav.  This number may vary, may be less than
         *     the value implied by the buflength parameter given in espeak_Initialize, and may
         *     sometimes be zero (which does NOT indicate end of synthesis).
         *
         *  @param events is an array of espeak_EVENT items which indicate word and sentence events, and
         *     also the occurrence if <mark> and <audio> elements within the text.  The list of
         *     events is terminated by an event of type = 0.
         *
         *  @returns: 0=continue synthesis, 1=abort synthesis.
         */
        fun callback(wav: Pointer?, numSamples: Int, events: Pointer?): Int
    }

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
        var instance: EspeakLibNative? = null

        val targetNativesDir: Path = if (MinecraftClient.getInstance() != null)
            TextToVoiceClient.dataDir.resolve("native")
        else
            Path.of("src/client/resources/native")

        val dataDir: Path = targetNativesDir.resolve("espeak-ng-data")

        init {
            val libName = if ("windows" in System.getProperty("os.name").lowercase())
                "libespeak-ng"
            else
                "espeak-ng"

            Files.createDirectories(targetNativesDir)

            // Copying out all the files
            val copyResult = runCatching {
                val index = this::class.java.getResourceAsStream("/_natives_index.txt")
                    ?: error("No index file found")

                val files = String(index.readAllBytes(), StandardCharsets.UTF_8)
                    .lines().toList()
                for (file in files) {
                    var stream = this::class.java.getResourceAsStream("/native/$file")
                    if (stream == null) {
                        LOGGER.error("Failed to read stream for file '$file'")
                        continue
                    }

                    val path = targetNativesDir.resolve(file)
                    Files.createDirectories(path.parent)
                    Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING)
                }
                index.close()
            }
            copyResult.onFailure { err ->
                error("$MOD_ID: Failed to load native library, unable to copy to '$targetNativesDir' ($err)")
            }

            // Loading the library
            System.setProperty("jna.library.path", targetNativesDir.absolutePathString())
            val result = runCatching {
                instance = Native.load(libName, EspeakLibNative::class.java)
            }
            result.onSuccess {
                Native.setProtected(true)
            }
            result.onFailure {
                error("$MOD_ID: Failed to load $libName from '$targetNativesDir'. Please try installing espeak-ng manually")
            }
        }
    }
}