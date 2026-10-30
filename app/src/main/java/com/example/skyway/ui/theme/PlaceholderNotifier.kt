package com.example.skyway.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun interface PlaceholderNotifier { fun notifyPlaceholder() }
val LocalPlaceholderNotifier = staticCompositionLocalOf<PlaceholderNotifier> {
    error("PlaceholderNotifier must be provided at the application root")
}
fun CoroutineScope.showNotImplemented(host: SnackbarHostState, message: String) {
    launch {
        host.currentSnackbarData?.dismiss()
        host.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Short)
    }
}
val MaterialTheme.snackbar: PlaceholderNotifier
    @Composable @ReadOnlyComposable get() = LocalPlaceholderNotifier.current
