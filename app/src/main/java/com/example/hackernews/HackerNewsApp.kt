package com.example.hackernews

import android.app.Application
import com.example.hackernews.di.AppContainer

class HackerNewsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
