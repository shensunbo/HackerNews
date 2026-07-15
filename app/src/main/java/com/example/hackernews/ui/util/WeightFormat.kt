package com.example.hackernews.ui.util

import kotlin.math.roundToInt

fun asciiWeightBar(value: Float, segments: Int = 8): String {
    val filled = ((value.coerceIn(0f, 2f) / 2f) * segments).roundToInt()
    return "▊".repeat(filled) + "▁".repeat(segments - filled)
}
