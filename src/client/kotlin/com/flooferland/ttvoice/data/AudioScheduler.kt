package com.flooferland.ttvoice.data

import com.flooferland.ttvoice.speech.EspeakSpeaker
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

class AudioScheduler {
    data class TimedFrame(val frame: ShortArray, val timestampNs: Long, var consumed: AtomicBoolean = AtomicBoolean(false))
    var queue = ConcurrentLinkedDeque<TimedFrame>()
    var lastFrameTime = 0L
    var frameIndex = 0L

    @Synchronized
    fun pushFrame(frame: ShortArray) {
        val paddedFrame = if (frame.size != EspeakSpeaker.BUFFER_SIZE) {
            padFrame(frame)
        } else {
            frame
        }

        queue.add(TimedFrame(paddedFrame, System.nanoTime()))

        // Limit queue so the CPU can't blow up
        while (queue.size > 50) {
            queue.pollFirst()
        }
    }

    @Synchronized
    fun next(): ShortArray? {
        val now = System.nanoTime()
        val expectedTime = lastFrameTime + EspeakSpeaker.let { it.FRAME_MS - it.FRAME_MS_STITCH }
        if (lastFrameTime > 0 && now < expectedTime) {
            return null
        }

        val frame = queue.firstOrNull() { !it.consumed.get() }
        if (frame != null) {
            frame.consumed.set(true)
            lastFrameTime = now
            frameIndex++

            // Clean up old consumed frames
            while (queue.isNotEmpty() && queue.peekFirst()?.consumed?.get() == true) {
                queue.pollFirst()
            }

            return frame.frame
        }
        return null
    }

    private fun padFrame(frame: ShortArray): ShortArray {
        if (frame.size == EspeakSpeaker.BUFFER_SIZE) return frame

        val output = ShortArray(EspeakSpeaker.BUFFER_SIZE)
        val ratio = frame.size.toDouble() / EspeakSpeaker.BUFFER_SIZE.toDouble()

        for (i in output.indices) {
            val sourceIndex = (i * ratio).toInt()
            output[i] = if (sourceIndex < frame.size) frame[sourceIndex] else 0
        }

        return output
    }
}