package com.ftrono.DJames.be.audio

import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recSamplingRate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


/**
 * NOISE CLEANING BAND-PASS FILTER:
 * Second-order Biquad band-pass filter (RBJ cookbook implementation).
 * Keeps internal state across calls (no per-frame restart).
 *
 * Input/Output: PCM16 little-endian mono ByteArray frames.
 */
class NoiseBPFilter() {
    // Type: Bi-Quad Band-Pass filter

    // Init coefficients:
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    // State (Direct Form I):
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    init {
        val minFreqHz: Int = prefs.recMinFreq.toInt()
        val maxFreqHz: Int = prefs.recMaxFreq.toInt()
        setBand(minFreqHz, maxFreqHz)
    }

    /**
     * Configure the bandpass using min & max frequency.
     * Uses geometric center f0 = sqrt(f1 * f2) and Q = f0 / (f2 - f1).
     *
     * In setBand() we define constants (b0, b1, b2, a0, a1, a2) that describe how strongly to weight current and past samples.
     *
     * They come from these DSP equations for a biquad filter (a 2-pole, 2-zero filter):
     *
     * [ y[n] = b_0 x[n] + b_1 x[n-1] + b_2 x[n-2] - a_1 y[n-1] - a_2 y[n-2] ]
     *
     * where:
     *
     * x[n] is the input sample,
     * y[n] is the output sample,
     * b0, b1, b2 control the feed-forward part (how input affects output),
     * a1, a2 control the feedback part (how previous outputs affect new one).
     * The coefficients depend on:
     *
     * f0: center frequency (roughly where the band is centered),
     * Q: quality factor (how wide/narrow the band is),
     * sample_rate: defines the Nyquist limit (half of sample rate = max representable frequency).
     */
    fun setBand(minFreqHz: Int, maxFreqHz: Int) {
        val nyquist = recSamplingRate / 2.0
        val f1 = minFreqHz.coerceAtLeast(1).coerceAtMost(nyquist.toInt() - 1)
        val f2 = maxFreqHz.coerceAtMost(nyquist.toInt()).coerceAtLeast(2).toDouble()

        val f0 = kotlin.math.sqrt(f1 * f2) // geometric mean
        val bw = (f2 - f1).coerceAtLeast(1.0)
        val Q = (f0 / bw).coerceAtLeast(0.3) // avoid too small Q

        val w0 = 2.0 * PI * f0 / recSamplingRate.toDouble()
        val alpha = sin(w0) / (2.0 * Q)

        val sinw0 = sin(w0)
        val cosw0 = cos(w0)

        // RBJ bandpass (constant skirt gain, peak = Q)
        val B0 = sinw0 / 2.0
        val B1 = 0.0
        val B2 = -B0
        val A0 = 1.0 + alpha
        val A1 = -2.0 * cosw0
        val A2 = 1.0 - alpha

        b0 = B0 / A0
        b1 = B1 / A0
        b2 = B2 / A0
        a1 = A1 / A0
        a2 = A2 / A0
    }

    /**
     * Process one PCM16 sample (normalized to [-1,1]). Returns filtered sample.
     *
     * For each incoming sample x, we compute y using the DSP formula and remember the last two inputs/outputs (x1, x2, y1, y2) for the next step:
     * y = b0*x + b1*x1 + b2*x2 - a1*y1 - a2*y2
     *
     * This recurrence creates a smooth filter response — it’s not a hard cutoff, but a gradual one.
     */
    private fun processSample(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        // shift delay lines
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }

    /**
     * Process bytes from input (little-endian 16-bit PCM mono) starting at inOffset,
     * writing to out (must be same length). This avoids allocations and works in-place if you pass same array.
     *
     * - inBytes: source buffer (may be your recording buffer)
     * - inOffset: byte offset inside inBytes where frame starts
     * - outBytes: destination buffer (preallocated) - same length as frame
     */
    fun processInto(inBytes: ByteArray, inOffset: Int, outBytes: ByteArray) {
        val n = outBytes.size / 2
        var j = inOffset
        var k = 0
        for (i in 0 until n) {
            val lo = inBytes[j].toInt() and 0xFF
            val hi = inBytes[j + 1].toInt() // signed shift will produce correct signed 16-bit
            val value = (hi shl 8) or lo // signed 16-bit in Int range -32768..32767
            // convert to -1..1
            val inSample = value.toDouble() / 32768.0

            val outSample = processSample(inSample)

            // convert back to 16-bit
            val s = (outSample * 32767.0).roundToInt().coerceIn(-32768, 32767)
            outBytes[k++] = (s and 0xFF).toByte()
            outBytes[k++] = ((s shr 8) and 0xFF).toByte()

            j += 2
        }
    }
}
