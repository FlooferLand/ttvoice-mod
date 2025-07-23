package com.flooferland.ttvoice.speech

import com.flooferland.espeak.Espeak

object EspeakSpeaker : ISpeaker {
    override fun load() {
        Espeak.initialize()
    }

    override fun unload() {
        Espeak.terminate()
    }

    override fun speak(text: String) {
        Thread {
            Espeak.synth(text)
        }.start()
    }

    override fun shutUp() {
        Espeak.cancel()
    }

    override fun isSpeaking(): Boolean {
        return Espeak.isPlaying()
    }
}