package com.example.hackernews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.theme.HackerNewsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HackerNewsTheme { Placeholder() } }
    }
}

@Composable
private fun Placeholder() {
    Scaffold(Modifier.fillMaxSize()) { p ->
        Text(
            "> hackernews ready █",
            Modifier.padding(p).padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
