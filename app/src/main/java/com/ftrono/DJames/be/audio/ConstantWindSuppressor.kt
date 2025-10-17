package com.ftrono.DJames.be.audio

import kotlin.math.*
import kotlin.math.log10


/**
 * Constant wind suppressor for continuous broadband wind noise (e.g. open car windows).
 *
 * Works by maintaining a running average noise floor in the frequency domain,
 * and attenuating bins that stay constantly high (wind) but do not fluctuate like speech.
 *
 * Pure Kotlin, lightweight, uses naive FFT (Cooley–Tukey) for small frames.
 */
class ConstantWindSuppressor(
    private val sampleRate: Int,
    private val frameSize: Int = 512,
    private val alpha: Double = 0.95,      // noise floor smoothing
    private val suppressionDb: Double = -12.0  // attenuation applied to steady bins
) {
    private val noiseFloor = DoubleArray(frameSize / 2) { 1e-6 }

    // --- Naive FFT implementation (magnitude only) ---
    private fun fftMag(samples: DoubleArray): DoubleArray {
        val n = samples.size
        val real = samples.copyOf()
        val imag = DoubleArray(n)
        var step = 1
        while (step < n) {
            val jump = step * 2
            val delta = (-Math.PI / step)
            val sine = sin(0.5 * delta)
            val mult = -2.0 * sine * sine
            val phase = sin(delta)
            var wr = 1.0
            var wi = 0.0
            for (m in 0 until step) {
                for (i in m until n step jump) {
                    val j = i + step
                    val tr = wr * real[j] - wi * imag[j]
                    val ti = wr * imag[j] + wi * real[j]
                    real[j] = real[i] - tr
                    imag[j] = imag[i] - ti
                    real[i] += tr
                    imag[i] += ti
                }
                val tmp = wr
                wr += tmp * mult - wi * phase
                wi += wi * mult + tmp * phase
            }
            step = jump
        }
        val mags = DoubleArray(n / 2)
        for (i in mags.indices) mags[i] = sqrt(real[i].pow(2) + imag[i].pow(2))
        return mags
    }

    /**
     * Process a PCM16 mono frame. Returns a modified copy.
     * Works best on 20–30 ms frames (e.g. 480 samples @ 16 kHz).
     */
    fun process(frame: ByteArray): ByteArray {
        val n = frame.size / 2
        val samples = DoubleArray(n)
        var i = 0
        var k = 0
        while (i < frame.size) {
            val lo = frame[i].toInt() and 0xFF
            val hi = frame[i + 1].toInt()
            val value = (hi shl 8) or lo
            samples[k++] = value / 32768.0
            i += 2
        }

        // 1. Compute FFT magnitudes
        val mags = fftMag(samples.copyOf(frameSize))

        // 2. Update noise floor (EMA)
        for (b in mags.indices) {
            noiseFloor[b] = alpha * noiseFloor[b] + (1 - alpha) * mags[b]
        }

        // 3. Suppress bins near the noise floor
        for (b in mags.indices) {
            if (mags[b] < noiseFloor[b] * 1.2) {
                mags[b] *= 10.0.pow(suppressionDb / 20.0)  // attenuate steady bins
            }
        }

        // 4. Very light global gain compensation to keep level stable
        val avgMag = mags.filter { it.isFinite() && it > 0 }.average().takeIf { it.isFinite() && it > 0 } ?: 1e-6
        val avgNoise = noiseFloor.filter { it.isFinite() && it > 0 }.average().takeIf { it.isFinite() && it > 0 } ?: 1e-6
        val avgGain = avgMag / avgNoise
        val gain = if (avgGain.isFinite() && avgGain > 0) min(1.0 / avgGain, 4.0) else 1.0

        // 5. Apply gain and sanitize samples
        for (j in samples.indices) {
            var v = samples[j] * gain
            if (!v.isFinite()) v = 0.0
            samples[j] = v.coerceIn(-1.0, 1.0)
        }

        // 6. Convert back to PCM16 safely
        val out = ByteArray(frame.size)
        var p = 0
        for (s in samples) {
            val safe = if (s.isFinite()) s else 0.0
            val v = (safe * 32767.0).roundToInt().coerceIn(-32768, 32767)
            out[p++] = (v and 0xFF).toByte()
            out[p++] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }
}