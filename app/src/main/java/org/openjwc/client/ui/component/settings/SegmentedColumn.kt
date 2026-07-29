@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.openjwc.client.ui.component.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

private const val PADDING_HORIZONTAL = 16
private const val PADDING_VERTICAL = 8

private const val bouncyStiffness = 800f
private const val bouncyDamping = 0.5f

@DslMarker
annotation class SegmentedColumnDsl

@Immutable
data class SegmentedItemData(
    val key: Any?,
    val visible: Boolean,
    val customTopPadding: Dp? = null,
    val forceFlatTop: Boolean = false,
    val forceFlatBottom: Boolean = false,
    val content: @Composable () -> Unit
)

@SegmentedColumnDsl
class SegmentedColumnScope {
    val items = mutableListOf<SegmentedItemData>()

    private var isInsideExpandableBody: Boolean = false
    private var parentVisibilityMask: Boolean = true

    fun item(
        key: Any? = null,
        visible: Boolean = true,
        topPadding: Dp? = null,
        forceFlatTop: Boolean = false,
        forceFlatBottom: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val resolvedForceFlatTop = forceFlatTop || isInsideExpandableBody
        val resolvedVisible = visible && parentVisibilityMask

        items.add(
            SegmentedItemData(
                key = key ?: items.size,
                visible = resolvedVisible,
                customTopPadding = topPadding,
                forceFlatTop = resolvedForceFlatTop,
                forceFlatBottom = forceFlatBottom,
                content = content
            )
        )
    }

    fun expandableItem(
        animatedVisibility: Boolean = true,
        expanded: Boolean,
        topPadding: Dp? = null,
        topContent: @Composable () -> Unit,
        bottomContent: (SegmentedColumnScope.() -> Unit),
    ) {
        val previousInsideBody = isInsideExpandableBody
        val previousVisibilityMask = parentVisibilityMask

        item(
            visible = animatedVisibility,
            topPadding = topPadding,
            forceFlatBottom = expanded,
            content = topContent
        )

        isInsideExpandableBody = true
        parentVisibilityMask = previousVisibilityMask && animatedVisibility && expanded

        bottomContent()

        isInsideExpandableBody = previousInsideBody
        parentVisibilityMask = previousVisibilityMask
    }
}

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    contentPadding: PaddingValues = PaddingValues(
        horizontal = PADDING_HORIZONTAL.dp,
        vertical = PADDING_VERTICAL.dp
    ),
    content: SegmentedColumnScope.() -> Unit
) {
    val scope = SegmentedColumnScope().apply(content)
    val allItems = scope.items

    if (allItems.isEmpty()) return

    Column(modifier = modifier.padding(contentPadding)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    start = PADDING_HORIZONTAL.dp,
                    top = PADDING_VERTICAL.dp,
                    bottom = 8.dp
                )
            )
        }

        val firstVisibleIndex = allItems.indexOfFirst { it.visible }
        val lastVisibleIndex = allItems.indexOfLast { it.visible }
        val focusManager = LocalFocusManager.current

        val dpSpring = spring<Dp>(dampingRatio = bouncyDamping, stiffness = bouncyStiffness)

        allItems.forEachIndexed { index, itemData ->
            key(itemData.key ?: index) {
                val isFirst = index == firstVisibleIndex || (index == 0 && !itemData.visible)
                val isLast = index == lastVisibleIndex || (index == allItems.lastIndex && !itemData.visible)

                val baseTopRadius = if (isFirst) 16.dp else 5.dp
                val baseBottomRadius = if (isLast) 16.dp else 5.dp

                val targetTopRadius = if (itemData.forceFlatTop) 0.dp else baseTopRadius
                val targetBottomRadius = if (itemData.forceFlatBottom) 0.dp else baseBottomRadius

                val isDynamicDpSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

                val currentTopRadius = if (isDynamicDpSupported) {
                    animateDpAsState(targetTopRadius, dpSpring, label = "TopRadius").value
                } else targetTopRadius

                val currentBottomRadius = if (isDynamicDpSupported) {
                    animateDpAsState(targetBottomRadius, dpSpring, label = "BottomRadius").value
                } else targetBottomRadius

                val shape = RoundedCornerShape(
                    topStart = max(0.dp, currentTopRadius),
                    topEnd = max(0.dp, currentTopRadius),
                    bottomStart = max(0.dp, currentBottomRadius),
                    bottomEnd = max(0.dp, currentBottomRadius)
                )

                val targetTopPadding = itemData.customTopPadding
                    ?: (if (isFirst) 0.dp else ListItemDefaults.SegmentedGap)
                val currentTopPadding = if (isDynamicDpSupported) {
                    animateDpAsState(targetTopPadding, dpSpring, label = "TopPadding").value
                } else targetTopPadding

                var hasFocus by remember { mutableStateOf(false) }

                LaunchedEffect(itemData.visible) {
                    if (!itemData.visible && hasFocus) {
                        focusManager.clearFocus()
                    }
                }

                AnimatedVisibility(
                    visible = itemData.visible,
                    enter = fadeIn(spring(dampingRatio = bouncyDamping, stiffness = bouncyStiffness)) + expandVertically(spring(dampingRatio = bouncyDamping, stiffness = bouncyStiffness)),
                    exit = fadeOut(spring(dampingRatio = bouncyDamping, stiffness = bouncyStiffness)) + shrinkVertically(spring(dampingRatio = bouncyDamping, stiffness = bouncyStiffness))
                ) {
                    Box(
                        modifier = Modifier
                            .onFocusChanged { hasFocus = it.hasFocus }
                            .semantics { if (!itemData.visible) hideFromAccessibility() }
                    ) {
                        CompositionLocalProvider(LocalSegmentedItemShape provides shape) {
                            Column(modifier = Modifier.padding(top = currentTopPadding)) {
                                itemData.content()
                            }
                        }
                    }
                }
            }
        }
    }
}
