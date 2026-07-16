package com.example.hackernews.di

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContainerTest {
    @Test fun loadsBundledConfiguration() = runBlocking {
        val container = AppContainer(ApplicationProvider.getApplicationContext())

        assertEquals(6, container.feedRepository.topicsStream().first().size)
        assertTrue(container.classicsRepository.poolSize > 0)
    }
}
