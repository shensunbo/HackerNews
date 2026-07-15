package com.example.hackernews.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class HnItem(
    val id: Long = 0,
    val title: String? = null,
    val url: String? = null,
    val score: Int? = null,
    val time: Long? = null,      // 秒
    val type: String? = null,
)
