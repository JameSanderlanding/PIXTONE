package com.vova.glyph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vova.glyph.ui.GlyphRoot
import com.vova.glyph.ui.theme.GlyphTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            GlyphTheme {
                GlyphAppRoot()
            }
        }
    }

    @Composable
    private fun GlyphAppRoot() {
        val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(application))
        GlyphRoot(vm)
    }
}
