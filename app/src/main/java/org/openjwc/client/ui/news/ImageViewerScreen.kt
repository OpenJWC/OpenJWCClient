package org.openjwc.client.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size as CoilSize
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.viewmodels.NewsViewModel

/**
 * 图片查看器（独立导航路由）：全屏展示原图，点击任意处关闭。
 */
@Composable
fun ImageViewerScreen(navigator: Navigator, newsViewModel: NewsViewModel) {
    val url = newsViewModel.viewerImageUrl.collectAsStateWithLifecycle().value
    if (url == null) {
        LaunchedEffect(Unit) { navigator.pop() }
        return
    }

    val context = LocalContext.current
    // 查看器按原图解码（正文里的缩略显示仍按屏幕尺寸，省内存）
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(CoilSize.ORIGINAL)
            .precision(Precision.INEXACT)
            .build()
    }

    // entry 销毁时清空「正在查看的图片」
    DisposableEffect(Unit) {
        onDispose { newsViewModel.clearViewerImage() }
    }

    fun dismiss() {
        navigator.pop()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { dismiss() },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(24.dp),
        )
    }
}
