package cdev.wargame.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(context: Context? = null) {

    var isMuted: Boolean = false
    private val executor = Executors.newFixedThreadPool(2)
    private val sampleRate = 32000

    private val soundBuffers = mutableMapOf<String, ByteArray>()

    init {
        // Pre-generate soft, warm 16-bit PCM procedural sounds
        executor.execute {
            soundBuffers["drag_select"] = generateSoftBlip(587f, 75)       // Warm D5 bell blip
            soundBuffers["dispatched"] = generateSoftWhoosh(520f, 220f, 130) // Gentle downward air-glide
            soundBuffers["clash"] = generateSoftClash(150f, 50)             // Low soft acoustic thud
            soundBuffers["reinforce"] = generateSoftChime(listOf(523f, 659f), 100) // Warm C-E harmonic
            soundBuffers["hit"] = generateSoftTap(120f, 45)                // Mellow shield absorption tap
            soundBuffers["captured"] = generateWarmChord(listOf(330f, 392f, 523f), 300) // Lush E-G-C major triad
            soundBuffers["lost"] = generateWarmChord(listOf(311f, 261f, 196f), 320)     // Soft mellow low descent
            soundBuffers["victory"] = generateWarmFanfare(listOf(392f, 523f, 659f, 784f), 550) // Triumphant yet soft
            soundBuffers["defeat"] = generateWarmChord(listOf(220f, 196f, 146f), 500)
        }
    }

    /**
     * Converts a list of float audio samples (-1.0f to 1.0f) to 16-bit PCM byte array (little-endian).
     */
    private fun toPcm16(samples: FloatArray, masterVolume: Float = 0.38f): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val clamped = (samples[i] * masterVolume).coerceIn(-1.0f, 1.0f)
            val shortVal = (clamped * 32767f).toInt().coerceIn(-32768, 32767)
            bytes[i * 2] = (shortVal and 0xFF).toByte()
            bytes[i * 2 + 1] = ((shortVal shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Soft bell blip with raised-cosine attack (no clicks) and exponential warm decay.
     */
    private fun generateSoftBlip(freq: Float, durationMs: Int): ByteArray {
        val count = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val samples = FloatArray(count)
        val attackSamples = (sampleRate * 0.008f).toInt().coerceAtLeast(1) // 8ms soft attack

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val attack = if (i < attackSamples) {
                0.5f * (1f - kotlin.math.cos((i.toFloat() / attackSamples) * PI.toFloat()))
            } else 1.0f
            val decay = exp(-5.5 * (i.toDouble() / count)).toFloat()
            // Pure fundamental sine with soft overtone
            val wave = sin(2.0 * PI * freq * t).toFloat() + 0.25f * sin(2.0 * PI * (freq * 2f) * t).toFloat()
            samples[i] = wave * attack * decay
        }
        return toPcm16(samples, 0.35f)
    }

    /**
     * Gentle whoosh down-sweep for troop dispatch (smooth, warm, non-piercing).
     */
    private fun generateSoftWhoosh(startFreq: Float, endFreq: Float, durationMs: Int): ByteArray {
        val count = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val samples = FloatArray(count)
        val attackSamples = (sampleRate * 0.015f).toInt().coerceAtLeast(1)

        var phase = 0.0
        for (i in 0 until count) {
            val prog = i.toFloat() / count
            val curFreq = startFreq + (endFreq - startFreq) * (prog * prog)
            phase += 2.0 * PI * curFreq / sampleRate

            val attack = if (i < attackSamples) {
                0.5f * (1f - kotlin.math.cos((i.toFloat() / attackSamples) * PI.toFloat()))
            } else 1.0f
            val decay = (1f - prog) * (1f - prog)
            val wave = sin(phase).toFloat()
            samples[i] = wave * attack * decay
        }
        return toPcm16(samples, 0.30f)
    }

    /**
     * Low soft muffled acoustic pop for unit clash (replaces harsh static noise).
     */
    private fun generateSoftClash(freq: Float, durationMs: Int): ByteArray {
        val count = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val samples = FloatArray(count)
        val attackSamples = (sampleRate * 0.004f).toInt().coerceAtLeast(1)

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val prog = i.toFloat() / count
            val attack = if (i < attackSamples) (i.toFloat() / attackSamples) else 1f
            val decay = exp(-12.0 * prog).toFloat()
            val wave = sin(2.0 * PI * freq * t).toFloat()
            samples[i] = wave * attack * decay
        }
        return toPcm16(samples, 0.32f)
    }

    /**
     * Soft warm harmonic chime for friendly base reinforcement.
     */
    private fun generateSoftChime(freqs: List<Float>, durationMs: Int): ByteArray {
        val count = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val samples = FloatArray(count)
        val attackSamples = (sampleRate * 0.012f).toInt().coerceAtLeast(1)

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val prog = i.toFloat() / count
            val attack = if (i < attackSamples) {
                0.5f * (1f - kotlin.math.cos((i.toFloat() / attackSamples) * PI.toFloat()))
            } else 1.0f
            val decay = exp(-4.5 * prog).toFloat()

            var sum = 0f
            for (f in freqs) {
                sum += sin(2.0 * PI * f * t).toFloat()
            }
            sum /= freqs.size
            samples[i] = sum * attack * decay
        }
        return toPcm16(samples, 0.32f)
    }

    /**
     * Soft low-frequency damped tap for base hit.
     */
    private fun generateSoftTap(freq: Float, durationMs: Int): ByteArray {
        val count = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val samples = FloatArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val prog = i.toFloat() / count
            val decay = exp(-10.0 * prog).toFloat()
            val wave = sin(2.0 * PI * (freq * (1f - 0.3f * prog)) * t).toFloat()
            samples[i] = wave * decay
        }
        return toPcm16(samples, 0.28f)
    }

    /**
     * Warm lush ambient chord for base capture / loss.
     */
    private fun generateWarmChord(freqs: List<Float>, durationMs: Int): ByteArray {
        val count = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val samples = FloatArray(count)
        val attackSamples = (sampleRate * 0.035f).toInt().coerceAtLeast(1) // 35ms warm attack

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val prog = i.toFloat() / count
            val attack = if (i < attackSamples) {
                0.5f * (1f - kotlin.math.cos((i.toFloat() / attackSamples) * PI.toFloat()))
            } else 1.0f
            val decay = (1f - prog)

            var sum = 0f
            for (f in freqs) {
                sum += sin(2.0 * PI * f * t).toFloat()
            }
            sum /= freqs.size
            samples[i] = sum * attack * decay
        }
        return toPcm16(samples, 0.35f)
    }

    /**
     * Soft ambient fanfare for victory.
     */
    private fun generateWarmFanfare(notes: List<Float>, totalDurationMs: Int): ByteArray {
        val noteCount = notes.size
        val noteDurationMs = totalDurationMs / noteCount
        val totalSamples = (sampleRate * (totalDurationMs / 1000f)).toInt().coerceAtLeast(1)
        val samples = FloatArray(totalSamples)

        var cursor = 0
        for (f in notes) {
            val segSamples = (sampleRate * (noteDurationMs / 1000f)).toInt()
            val segAttack = (segSamples * 0.15f).toInt().coerceAtLeast(1)
            for (i in 0 until segSamples) {
                if (cursor >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val prog = i.toFloat() / segSamples
                val attack = if (i < segAttack) (i.toFloat() / segAttack) else 1f
                val env = (1f - prog * 0.5f) * attack
                val s = sin(2.0 * PI * f * t).toFloat() * env
                samples[cursor++] = s
            }
        }
        return toPcm16(samples, 0.35f)
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
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bytes.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(bytes, 0, bytes.size)
                track.play()
                // Auto-cleanup after audio ends
                Thread.sleep((bytes.size.toLong() * 1000L / (sampleRate * 2)) + 30)
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
