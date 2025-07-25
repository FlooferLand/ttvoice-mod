package com.flooferland.espeak

import io.kotest.core.spec.style.FunSpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class EspeakLibTest : FunSpec({
    val expectedChunks = 11

    test("initial") {
        val result = Espeak.initialize()
        assertTrue((result.getOrNull() ?: -1) > 1)
        Espeak.terminate()
    }

    test("speak") {
        var chunkCount = 0
        Espeak.initialize(Espeak.AudioOutput.Retrieval, 300)
        Espeak.setSynthCallback { wav, numberOfSamples, events ->
            chunkCount += 1
            if (chunkCount != expectedChunks) {
                println("CHUNK[$chunkCount]:\t(wav[1]=${wav?.getShortArray(0, numberOfSamples)[0]},\tnumberOfSamples=$numberOfSamples,\tevents=$events)")
            } else {
                println("CHUNK: end")
            }
            return@setSynthCallback 0
        }
        Espeak.synth("Hello guys and welcome back to Minecraft!")
        assertTrue(chunkCount > 0, "No chunks")
        assertEquals(chunkCount, expectedChunks, "Wrong number of chunks")
        println("Total: $chunkCount chunks")
        Espeak.terminate()
    }
})