package com.nxoim.sample.ui.theme

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
internal actual fun SampleTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme())
        DarkColorScheme
    else
        LightColorScheme

    MaterialExpressiveTheme(
        colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalOverscrollFactory provides rememberNotCupertinoOverscrollFactory()
            ) {
                Surface(content = content)
            }
        }
    )
}