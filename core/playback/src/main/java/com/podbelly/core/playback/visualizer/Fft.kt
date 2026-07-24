package com.podbelly.core.playback.visualizer

import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimal in-place iterative radix-2 Cooley–Tukey FFT, sized once at
 * construction. No external dependency — it runs on the audio thread on small
 * (1024-point) windows, which is a few microseconds per frame.
 *
 * [size] must be a power of two. Call [transform] with real/imaginary buffers of
 * length [size]; results are written back in place.
 */
internal class Fft(private val size: Int) {

    init {
        require(size > 0 && (size and (size - 1)) == 0) { "FFT size must be a power of two" }
    }

    // Precomputed bit-reversal permutation and twiddle factors.
    private val reversed = IntArray(size).also { table ->
        val bits = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size) {
            var x = i
            var r = 0
            for (b in 0 until bits) {
                r = (r shl 1) or (x and 1)
                x = x shr 1
            }
            table[i] = r
        }
    }
    private val cosTable = FloatArray(size / 2)
    private val sinTable = FloatArray(size / 2)

    init {
        for (i in 0 until size / 2) {
            val angle = -2.0 * Math.PI * i / size
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    /** In-place FFT of [real] + i·[imag] (both length [size]). */
    fun transform(real: FloatArray, imag: FloatArray) {
        // Reorder into bit-reversed index order.
        for (i in 0 until size) {
            val j = reversed[i]
            if (j > i) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }

        var len = 2
        while (len <= size) {
            val half = len / 2
            val step = size / len
            var i = 0
            while (i < size) {
                var k = 0
                for (j in i until i + half) {
                    val cosv = cosTable[k]
                    val sinv = sinTable[k]
                    val treR = real[j + half] * cosv - imag[j + half] * sinv
                    val treI = real[j + half] * sinv + imag[j + half] * cosv
                    real[j + half] = real[j] - treR
                    imag[j + half] = imag[j] - treI
                    real[j] += treR
                    imag[j] += treI
                    k += step
                }
                i += len
            }
            len = len shl 1
        }
    }
}
