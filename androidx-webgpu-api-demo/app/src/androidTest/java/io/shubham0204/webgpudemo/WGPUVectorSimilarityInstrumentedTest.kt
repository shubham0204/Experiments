package io.shubham0204.webgpudemo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class WGPUVectorSimilarityInstrumentedTest {

    private val lock = CountDownLatch(1)
    private var results: Array<FloatArray> = emptyArray()

    /**
     * Testing async/callback functions in JUnit
     * https://stackoverflow.com/a/1829949/13546426
     */
    @Test
    fun testWGPUVectorSimilarity() {
        val numVectors = 2
        val wgpuComputeShader = WGPUComputeShader()
        val vectors = Array(numVectors) { FloatArray(16) { Random.nextFloat() } }
        wgpuComputeShader.execute(vectors) {
            results = it
            lock.countDown()
        }
        lock.await(5000, TimeUnit.MILLISECONDS)
        for (i in 0..<numVectors) {
            assertEquals(1.0f, results[i][i], 0.01f)
        }
    }
}
