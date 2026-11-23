package com.lab3d.science.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.lab3d.science.model.LabProgress
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Простой процедурный синтезатор звуковых эффектов на AudioTrack (без внешних mp3/wav файлов).
 */
object SoundFx {
    private const val SR = 22050

    fun playClick() = playAsync(0.045) { t ->
        sin(2.0 * PI * (720.0 - t * 3000.0) * t) * exp(-t * 55.0) * 0.25
    }

    fun playPour() = playAsync(0.28) { t ->
        val noise = Random.nextDouble(-1.0, 1.0)
        val bubble = sin(2.0 * PI * (320.0 + 480.0 * t) * t)
        (noise * 0.18 + bubble * 0.16) * exp(-t * 6.0)
    }

    fun playEruption() = playAsync(0.85) { t ->
        val noise = Random.nextDouble(-1.0, 1.0)
        val rumble = sin(2.0 * PI * (65.0 + 30.0 * sin(t * 14.0)) * t)
        (noise * 0.35 + rumble * 0.45) * (1.0 - exp(-t * 15.0)) * exp(-t * 2.2)
    }

    fun playZap() = playAsync(0.22) { t ->
        val f = 140.0 + 520.0 * (1.0 - t / 0.22)
        val saw = ((t * f) % 1.0) * 2.0 - 1.0
        val crack = Random.nextDouble(-0.4, 0.4)
        (saw * 0.32 + crack * 0.25) * exp(-t * 10.0)
    }

    fun playRocket() = playAsync(0.95) { t ->
        val noise = Random.nextDouble(-1.0, 1.0)
        val thrust = sin(2.0 * PI * (90.0 + t * 55.0) * t)
        (noise * 0.42 + thrust * 0.30) * (1.0 - exp(-t * 12.0)) * exp(-t * 1.6)
    }

    fun playSuccess() = playAsync(0.48) { t ->
        val freq = when {
            t < 0.13 -> 523.25 // C5
            t < 0.26 -> 659.25 // E5
            else -> 783.99     // G5
        }
        val localT = when {
            t < 0.13 -> t
            t < 0.26 -> t - 0.13
            else -> t - 0.26
        }
        (sin(2.0 * PI * freq * t) + 0.35 * sin(4.0 * PI * freq * t)) * exp(-localT * 10.0) * 0.30
    }

    private fun playAsync(durationSec: Double, sample: (Double) -> Double) {
        if (!LabProgress.soundEnabled) return
        thread(isDaemon = true) {
            runCatching {
                val n = (SR * durationSec).toInt().coerceAtLeast(128)
                val pcm = ShortArray(n)
                for (i in 0 until n) {
                    val v = sample(i.toDouble() / SR).coerceIn(-0.95, 0.95)
                    pcm[i] = (v * 32767.0).toInt().toShort()
                }
                val bytes = n * 2
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SR)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(bytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, n)
                track.play()
                Thread.sleep((durationSec * 1000).toLong() + 60L)
                track.stop()
                track.release()
            }
        }
    }
}
