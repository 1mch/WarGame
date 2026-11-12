package cdev.wargame.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(context: Context? = null) {

    var isMuted: Boolean = false
    private val executor = Executors.newFixedThreadPool(2)
    private val sampleRate = 22050

    private val soundBuffers = mutableMapOf<String, ByteArray>()

    init {
        // Pre-generate procedural sound waves
        executor.execute {
            soundBuffers["drag_select"] = generateTone(880f, 1100f, 50, WaveType.SINE)
            soundBuffers["dispatched"] = generateSweep(1200f, 320f, 100)
            soundBuffers["clash"] = generateNoise(45, 0.7f)
            soundBuffers["reinforce"] = generateChime(listOf(523f, 659f), 70)
            soundBuffers["hit"] = generateSquareBurst(180f, 60)
            soundBuffers["captured"] = generateFanfare(listOf(440f, 554f, 659f, 880f), 220)
            soundBuffers["lost"] = generateSweep(260f, 110f, 280)
            soundBuffers["victory"] = generateFanfare(listOf(523f, 659f, 784f, 1046f), 450)
            soundBuffers["defeat"] = generateSweep(400f, 90f, 500)
        }
    }

    private enum class WaveType { SINE, SQUARE, NOISE }

    private fun generateTone(startFreq: Float, endFreq: Float, durationMs: Int, type: WaveType): ByteArray {
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val buffer = ByteArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            val t = i.toDouble() / sampleRate
            val envelope = (1f - progress) // fade out
            val sampleVal: Float = when (type) {
                WaveType.SINE -> sin(2.0 * PI * freq * t).toFloat()
                WaveType.SQUARE -> if (sin(2.0 * PI * freq * t) > 0) 0.8f else -0.8f
                WaveType.NOISE -> (Random.nextFloat() * 2f - 1f)
            }
            buffer[i] = (sampleVal * envelope * 120f).toInt().coerceIn(-128, 127).toByte()
        }
        return buffer
    }

    private fun generateSweep(startFreq: Float, endFreq: Float, durationMs: Int): ByteArray {
        return generateTone(startFreq, endFreq, durationMs, WaveType.SINE)
    }

    private fun generateNoise(durationMs: Int, intensity: Float): ByteArray {
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val buffer = ByteArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val envelope = (1f - progress) * (1f - progress)
            val n = (Random.nextFloat() * 2f - 1f) * intensity * envelope
            buffer[i] = (n * 127f).toInt().coerceIn(-128, 127).toByte()
        }
        return buffer
    }

    private fun generateChime(freqs: List<Float>, durationMs: Int): ByteArray {
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val buffer = ByteArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val t = i.toDouble() / sampleRate
            val envelope = 1f - progress
            var sum = 0.0
            for (f in freqs) {
                sum += sin(2.0 * PI * f * t)
            }
            sum /= freqs.size
            buffer[i] = (sum * envelope * 115.0).toInt().coerceIn(-128, 127).toByte()
        }
        return buffer
    }

    private fun generateSquareBurst(freq: Float, durationMs: Int): ByteArray {
        return generateTone(freq, freq * 0.6f, durationMs, WaveType.SQUARE)
    }

    private fun generateFanfare(notes: List<Float>, totalDurationMs: Int): ByteArray {
        val noteCount = notes.size
        val noteDuration = totalDurationMs / noteCount
        val totalSamples = (sampleRate * (totalDurationMs / 1000f)).toInt().coerceAtLeast(1)
        val buffer = ByteArray(totalSamples)
        var cursor = 0
        for (f in notes) {
            val segSamples = (sampleRate * (noteDuration / 1000f)).toInt()
            for (i in 0 until segSamples) {
                if (cursor >= totalSamples) break
                val prog = i.toFloat() / segSamples
                val env = 1f - (prog * 0.4f)
                val t = i.toDouble() / sampleRate
                val s = sin(2.0 * PI * f * t).toFloat() * env
                buffer[cursor++] = (s * 110f).toInt().coerceIn(-128, 127).toByte()
            }
        }
        return buffer
    }

    private fun playPcm(key: String) {
        if (isMuted) return
        executor.execute {
            val bytes = soundBuffers[key] ?: return@execute
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bytes.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(bytes, 0, bytes.size)
                track.play()
                // Auto-release after playing
                Thread.sleep((bytes.size.toLong() * 1000L / sampleRate) + 20)
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    fun playDragSelect() = playPcm("drag_select")
    fun playUnitsDispatched() = playPcm("dispatched")
    fun playUnitClash() = playPcm("clash")
    fun playBaseReinforce() = playPcm("reinforce")
    fun playBaseHit() = playPcm("hit")
    fun playBaseCaptured() = playPcm("captured")
    fun playBaseLost() = playPcm("lost")
    fun playVictory() = playPcm("victory")
    fun playDefeat() = playPcm("defeat")

    fun release() {
        executor.shutdownNow()
    }
}
