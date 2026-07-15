package com.example.hackernews.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightFormatTest {
    @Test fun emptyAtZero() = assertEquals("▁▁▁▁▁▁▁▁", asciiWeightBar(0f))

    @Test fun halfFilledAtDefaultWeight() = assertEquals("▊▊▊▊▁▁▁▁", asciiWeightBar(1f))

    @Test fun fullAtMaximumWeight() = assertEquals("▊▊▊▊▊▊▊▊", asciiWeightBar(2f))
}
