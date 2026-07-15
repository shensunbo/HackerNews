package com.example.hackernews.ui.util

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.example.hackernews.domain.model.ReadingMode

fun shouldUseCustomTabs(mode: ReadingMode, providerAvailable: Boolean): Boolean =
    mode == ReadingMode.CUSTOM_TABS && providerAvailable

object LinkOpener {
    fun open(context: Context, url: String, mode: ReadingMode) {
        val uri = url.toUri()
        val provider = CustomTabsClient.getPackageName(context, emptyList())
        if (shouldUseCustomTabs(mode, providerAvailable = provider != null)) {
            val opened = runCatching {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .apply { intent.setPackage(provider) }
                    .launchUrl(context, uri)
            }.isSuccess
            if (opened) return
        }

        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
