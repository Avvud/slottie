package com.example.samlottie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.samlottie.rlottie.RlottieView
import com.example.samlottie.ui.theme.SamlottieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SamlottieTheme {
                LottieScreen()
            }
        }
    }
}

@Composable
fun LottieScreen() {
    val configuration = LocalConfiguration.current
    val minDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val stickerSize = (minDp * 0.6f).dp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                RlottieView(context).apply {
                    setAnimationAsset("loading.json")
                    setRenderScale(0.3f)
                }
            },
            modifier = Modifier.size(stickerSize)
        )
    }
}
