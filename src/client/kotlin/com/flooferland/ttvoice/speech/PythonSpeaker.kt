package com.flooferland.ttvoice.speech

import com.flooferland.ttvoice.data.ModState
import java.nio.file.Files

object PythonSpeaker : ISpeaker {
    val speaking = arrayListOf<Process>()

    public override fun speak(text: String) {
        val tempFile: java.nio.file.Path? = Files.createTempFile("script", ".py")
        if (tempFile == null || ModState.pythonScript == null) {
            return
        }
        Files.write(tempFile, ModState.pythonScript!!.toByteArray())
        tempFile.toFile().deleteOnExit()

        val process = ProcessBuilder(
            ModState.config.general.pythonPath, tempFile.toAbsolutePath().toString(),
            text, // Text
            ModState.config.audio.device.toString() // Device
        )
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();

        // Registering the process
        speaking.add(process)

        // Clearing existing processes
        speaking
            .filter { p -> !p.isAlive }
            .forEach { p -> speaking.remove(p) }
    }

    override fun shutUp() {
        speaking.forEach { it.destroy() }
        speaking.removeIf { p -> !p.isAlive }
    }
}