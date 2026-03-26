package com.ghostgramlabs.ghostmask.stego

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CapacityCalculatorTest {

    @Test
    fun `100x100 image capacity calculation in stealth mode`() {
        assertThat(CapacityCalculator.calculateCapacity(100, 100, 1)).isEqualTo(3746)
    }

    @Test
    fun `1920x1080 image capacity`() {
        assertThat(CapacityCalculator.calculateCapacity(1920, 1080, 1)).isEqualTo(777596)
    }

    @Test
    fun `1x1 image has minimal capacity`() {
        assertThat(CapacityCalculator.calculateCapacity(1, 1, 1)).isEqualTo(0)
    }

    @Test
    fun `zero dimensions return zero`() {
        assertThat(CapacityCalculator.calculateCapacity(0, 100, 1)).isEqualTo(0)
        assertThat(CapacityCalculator.calculateCapacity(100, 0, 1)).isEqualTo(0)
        assertThat(CapacityCalculator.calculateCapacity(0, 0, 1)).isEqualTo(0)
    }

    @Test
    fun `negative dimensions return zero`() {
        assertThat(CapacityCalculator.calculateCapacity(-1, 100, 1)).isEqualTo(0)
    }

    @Test
    fun `canFit returns true for small payload`() {
        assertThat(CapacityCalculator.canFit(100, 100, 100, 1)).isTrue()
    }

    @Test
    fun `canFit returns false for oversized payload`() {
        assertThat(CapacityCalculator.canFit(10, 10, 1000, 1)).isFalse()
    }

    @Test
    fun `canFit returns true at exact capacity`() {
        val capacity = CapacityCalculator.calculateCapacity(100, 100, 1)
        assertThat(CapacityCalculator.canFit(100, 100, capacity, 1)).isTrue()
    }

    @Test
    fun `capacitySummary shows correct values`() {
        val info = CapacityCalculator.capacitySummary(100, 100, 1000, 1)
        assertThat(info.totalBytes).isEqualTo(3746)
        assertThat(info.usedBytes).isEqualTo(1000)
        assertThat(info.remainingBytes).isEqualTo(2746)
        assertThat(info.fits).isTrue()
        assertThat(info.recommendedLsbBits).isEqualTo(1)
    }

    @Test
    fun `higher bit modes increase capacity`() {
        val oneBit = CapacityCalculator.calculateCapacity(100, 100, 1)
        val twoBit = CapacityCalculator.calculateCapacity(100, 100, 2)
        val threeBit = CapacityCalculator.calculateCapacity(100, 100, 3)

        assertThat(twoBit).isGreaterThan(oneBit)
        assertThat(threeBit).isGreaterThan(twoBit)
    }
}
