package org.openjwc.client.navigation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import org.openjwc.client.navigation3.NavContainer

@Composable
fun NavGraph() {
    val focusManager = LocalFocusManager.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        NavContainer()
    }
}
