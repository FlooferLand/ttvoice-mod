package com.flooferland.ttvoice.speech

import com.flooferland.ttvoice.TextToVoiceClient
import com.flooferland.ttvoice.data.ModState
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

object PythonSpeaker : ISpeaker {
    var process: Process? = null
    var commandListener: Thread? = null
    val pythonDir = TextToVoiceClient.dataDir.resolve("python")
    lateinit var scriptFile: Path
    var speaking = AtomicBoolean(false)

    // TODO: Preload and unload the script and communicate with it via stdin to remove Python process startup delays
    override fun load() {
        Files.createDirectories(pythonDir)
        if (ModState.pythonScript == null) {
            return
        }

        // Resolve the script if it doesn't exist
        scriptFile = pythonDir.resolve("main.py")
        if (Files.exists(scriptFile)) {
            // Writing/parsing the script
            val read = Files.readString(scriptFile)
            val header = parseHeader(read)
            if (header.autoupdate) {
                Files.writeString(scriptFile, ModState.pythonScript!!)
            }
        } else {
            Files.createFile(scriptFile)
            Files.writeString(scriptFile, ModState.pythonScript!!)
        }

        // Creating a process for the script
        println("Creating process..")
        val process = ProcessBuilder(
            ModState.config.general.pythonPath, "-u", scriptFile.toAbsolutePath().toString(),
            ModState.config.audio.device.toString() // Device
        ).directory(pythonDir.toFile()).start();
        println("Created and launched process!")

        // Booting up a thread to listen to commands
        if (commandListener == null || !commandListener!!.isAlive) {
            commandListener = Thread {
                val reader = process.inputStream.bufferedReader()
                while (true) {
                    val line = reader.readLine() ?: break
                    println("commandListener line: $line")
                    when (line.trim()) {
                        "speak: begin" -> {
                            speaking.set(true)
                            println("received \"speak: begin\"")
                        }
                        "speak: end" -> {
                            speaking.set(false)
                            println("received \"speak: begin\"")
                        }
                    }
                }
                println("commandListener broke :(")
            }
            commandListener!!.start()
        }

        // Error thread
        Thread {
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { TextToVoiceClient.LOGGER.error(it) }
            }
        }.start()

        // Registering the process
        PythonSpeaker.process = process
    }
    override fun unload() {
        process?.destroyForcibly()
        process = null
    }

    public override fun speak(text: String) {
        val writer = process?.outputWriter()
        if (writer == null) return

        writer.write("speak: $text\n")
        writer.flush()
    }

    override fun shutUp() {
        val writer = process?.outputWriter()
        if (writer == null) return

        writer.write("shutup:\n")
        writer.flush()
    }

    override fun isSpeaking(): Boolean {
        return speaking.get()
    }

    class PythonFileHeader {
        var autoupdate = true
    }
    private fun parseHeader(text: String): PythonFileHeader {
        val header = PythonFileHeader()
        for (line in text.lines()) {
            if (line.startsWith('#')) break
            val line = line.substring(1).trim()
            val parts = line.split(':', limit = 2).map { it.trim() }
            if (parts.size != 2) continue

            when (parts[0]) {
                "autoupdate" -> {
                    header.autoupdate = (parts[1] == "enable")
                }
            }
        }
        return header
    }
}