package com.ghostgramlabs.ghostmask.stego

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [CapacityCalculator].
 */
class CapacityCalculatorTest {

    @Test
    fun `100x100 image capacity calculation`() {
        // 100*100 = 10000 pixels, 3 bits each = 30000 bits = 3750 bytes - 4 header = 3746
        val capacity = CapacityCalculator.calculateCapacity(100, 100)
        assertThat(capacity).isEqualTo(3746)
    }

    @Test
    fun `1920x1080 image capacity`() {
        // 1920*1080 = 2,073,600 pixels * 3 bits = 6,220,800 bits = 777,600 bytes - 4 = 777,596
        val capacity = CapacityCalculator.calculateCapacity(1920, 1080)
        assertThat(capacity).isEqualTo(777596)
    }

    @Test
    fun `1x1 image has minimal capacity`() {
        // 1 pixel * 3 bits = 3 bits = 0 bytes (integer division) - 4 = negative, clamped to 0
        val capacity = CapacityCalculator.calculateCapacity(1, 1)
        assertThat(capacity).isEqualTo(0)
    }

    @Test
    fun `zero dimensions return zero`() {
        assertThat(CapacityCalculator.calculateCapacity(0, 100)).isEqualTo(0)
        assertThat(CapacityCalculator.calculateCapacity(100, 0)).isEqualTo(0)
        assertThat(CapacityCalculator.calculateCapacity(0, 0)).isEqualTo(0)
    }

    @Test
    fun `negative dimensions return zero`() {
        assertThat(CapacityCalculator.calculateCapacity(-1, 100)).isEqualTo(0)
    }

    @Test
    fun `canFit returns true for small payload`() {
        assertThat(CapacityCalculator.canFit(100, 100, 100)).isTrue()
    }

    @Test
    fun `canFit returns false for oversized payload`() {
        assertThat(CapacityCalculator.canFit(10, 10, 1000)).isFalse()
    }

    @Test
    fun `canFit returns true at exact capacity`() {
        val capacity = CapacityCalculator.calculateCapacity(100, 100)
        assertThat(CapacityCalculator.canFit(100, 100, capacity)).isTrue()
    }

    @Test
    fun `canFit returns false at one byte over capacity`() {
        val capacity = CapacityCalculator.calculateCapacity(100, 100)
        assertThat(CapacityCalculator.canFit(100, 100, capacity + 1)).isFalse()
    }

    @Test
    fun `capacitySummary shows correct values`() {
        val info = CapacityCalculator.capacitySummary(100, 100, 1000)
        assertThat(info.totalBytes).isEqualTo(3746)
        assertThat(info.usedBytes).isEqualTo(1000)
        assertThat(info.remainingBytes).isEqualTo(2746)
        assertThat(info.fits).isTrue()
    }

    @Test
    fun `capacitySummary shows not fitting`() {
        val info = CapacityCalculator.capacitySummary(10, 10, 1000)
        assertThat(info.fits).isFalse()
        assertThat(info.remainingBytes).isEqualTo(0)
    }
}
