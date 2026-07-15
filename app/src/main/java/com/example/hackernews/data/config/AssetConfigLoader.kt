package com.example.hackernews.data.config

import android.content.Context
import com.example.hackernews.domain.model.Topic

fun interface TopicConfigSource {
    fun loadTopics(): List<Topic>
}

class AssetConfigLoader(private val context: Context) : TopicConfigSource {
    override fun loadTopics(): List<Topic> = parseTopics(readAsset("topics.json"))
    fun loadClassics(): List<ClassicItem> = parseClassics(readAsset("classics.json"))
    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }
}
