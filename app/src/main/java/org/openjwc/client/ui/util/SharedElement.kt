package org.openjwc.client.ui.util

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.openjwc.client.ui.theme.ThemeConfig

/**
 * 官方共享元素（两态内容不同，bounds 形变 + 内容交叉淡入）。
 *
 * - [SharedTransitionScope] 由 `SharedTransitionLayout` 提供（见 `NavContainer`）；
 * - [LocalNavAnimatedContentScope] 由 navigation3 的 `NavDisplay` 为每个 entry 提供；
 * - 两侧内容不同（卡片 vs 详情页）时用 `sharedBounds`：位置/尺寸插值 + 内容交叉淡入淡出。
 *
 * 未处于导航容器内（例如预览）时退化为普通 Modifier。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsWithNav(
    key: Any,
    /**
     * 形变时内容如何适配动画尺寸：
     * - [SharedTransitionScope.ResizeMode.RemeasureToBounds]：按动画尺寸重新测量（文字重排，适合卡片）；
     * - [SharedTransitionScope.ResizeMode.ScaleToBounds]：整体缩放（不重测，适合含滚动/顶栏的整页，避免退出时跳动）。
     */
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
): Modifier {
    // 关闭动画时不做共享元素形变（RemeasureToBounds 每帧重测量，最吃配置）
    if (!ThemeConfig.animationsEnabled) return this
    // 预测返回动画选了 None：卡片不再跟随返回手势做形变
    if (ThemeConfig.predictiveBackAnimation == "None") return this
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(sharedScope) {
        this@sharedBoundsWithNav.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
            enter = fadeIn(),
            exit = fadeOut(),
            resizeMode = resizeMode,
        )
    }
}
