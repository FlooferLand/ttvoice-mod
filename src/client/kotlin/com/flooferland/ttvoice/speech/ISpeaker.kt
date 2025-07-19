package com.flooferland.ttvoice.speech

interface ISpeaker {
    public fun speak(text: String)
    public fun shutUp()
    public fun isSpeaking(): Boolean
    public fun playTest() {
        speak("Audio test")
    }
}