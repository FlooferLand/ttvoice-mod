package com.flooferland.ttvoice.speech

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioInputStream
import com.flooferland.ttvoice.TextToVoiceClient
import com.flooferland.ttvoice.VcPlugin
import com.flooferland.ttvoice.data.ModState
import com.flooferland.ttvoice.data.TextToVoiceConfig
import javax.sound.sampled.AudioSystem

object NativeSpeaker : ISpeaker {
    public override fun speak(text: String) {
        val audioStream = when (ModState.config.audio.ttsBackend) {
            TextToVoiceConfig.TTSBackend.Native -> {
                TODO()
            }
            TextToVoiceConfig.TTSBackend.Python -> {
                throw Exception("Python is not allowed here")
            }
        }

        // Playing the audio
        if (ModState.config.general.useSimpleVoiceChat) {
            val audio = audioStreamToShortArray(audioStream)
            if (audio.isEmpty()) {
                throw Exception("Missing audio data")
            }
            VcPlugin.channel.play(audio)
        }
        if (ModState.config.general.broadcastToAudioMixer) {
            val clip = AudioSystem.getClip()
            clip.open(audioStream)
            clip.start()

            TextToVoiceClient.LOGGER.info("Should've said '${text}'!")
        }
    }

    override fun shutUp() {

    }

    fun audioStreamToShortArray(inputStream: AudioInputStream): ShortArray {
        val byteList = mutableListOf<Byte>()
        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            for (i in 0 until bytesRead) {
                byteList.add(buffer[i])
            }
        }

        val byteArray = byteList.toByteArray()
        val shortBuffer = ByteBuffer.wrap(byteArray)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()

        val shortArray = ShortArray(shortBuffer.limit())
        shortBuffer.get(shortArray)
        return shortArray
    }
}