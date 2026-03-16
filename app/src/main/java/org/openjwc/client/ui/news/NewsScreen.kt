package org.openjwc.client.ui.news

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.notification.RequestNotificationPermissionButton
import org.openjwc.client.viewmodels.NewsViewModel

@Composable
fun NewsScreen(
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
    viewModel: NewsViewModel = viewModel()
) {

}