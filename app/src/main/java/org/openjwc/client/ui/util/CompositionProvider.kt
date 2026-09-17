package org.openjwc.client.ui.util

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("CompositionLocal LocalSnackbarController not present")
}

val LocalPagerState = compositionLocalOf<PagerState> { error("No pager state") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("No handle page change") }
val LocalSelectedPage = compositionLocalOf<Int> { error("No selected page") }

/** 由 [androidx.compose.animation.SharedTransitionLayout] 提供，供各页面做 sharedElement 过渡。 */
val LocalSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope?> =
    staticCompositionLocalOf { null }
