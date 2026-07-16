package com.example.hackernews.di

import android.content.Context
import com.example.hackernews.data.classics.ClassicsRepository
import com.example.hackernews.data.classics.ClassicsSelector
import com.example.hackernews.data.config.AssetConfigLoader
import com.example.hackernews.data.local.AppDatabase
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.remote.HnApiService
import com.example.hackernews.data.remote.HnRemoteSource
import com.example.hackernews.data.remote.RssRemoteSource
import com.example.hackernews.data.repository.FeedRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.build(appContext)
    private val configLoader = AssetConfigLoader(appContext)
    private val json = Json { ignoreUnknownKeys = true }
    private val okHttp = OkHttpClient.Builder().build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://hacker-news.firebaseio.com/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    private val hnApi = retrofit.create(HnApiService::class.java)

    val preferencesStore = PreferencesStore(appContext)
    val feedRepository = FeedRepository(
        dao = database.articleDao(),
        prefs = preferencesStore,
        configLoader = configLoader,
        hn = HnRemoteSource(hnApi),
        rss = RssRemoteSource(),
    )
    private val classicsPool = configLoader.loadClassicsPool()
    val classicsRepository = ClassicsRepository(
        selector = ClassicsSelector(classicsPool.items, classicsPool.poolVersion),
        stateStore = preferencesStore,
        bookmarks = feedRepository,
    )
}
