package com.example.hackernews.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.hackernews.HackerNewsApp
import com.example.hackernews.di.AppContainer

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val application = LocalContext.current.applicationContext as HackerNewsApp
    return viewModel(
        factory = viewModelFactory {
            initializer { create(application.container) }
        },
    )
}
