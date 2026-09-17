package org.openjwc.client.ui.component.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsTextFieldWidget(
    modifier: Modifier = Modifier,
    state: TextFieldState,
    onClick: (() -> Unit)? = null,
    title: String = "",
    error: String = "",
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    useLabelAsPlaceholder: Boolean = false,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMediumEmphasized.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = MaterialTheme.typography.bodySmallEmphasized.fontFamily,
    ),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    outputTransformation: OutputTransformation? = null,
    cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
    scrollState: ScrollState = rememberScrollState(),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val isImeVisible = WindowInsets.isImeVisible
    val coroutineScope = rememberCoroutineScope()

    val isClickableMode = onClick != null

    val hasFocusReassignBug = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1
    var allowFocus by remember { mutableStateOf(!hasFocusReassignBug) }

    LaunchedEffect(pressed) {
        if (pressed && hasFocusReassignBug && !allowFocus) {
            allowFocus = true
        }
    }

    LaunchedEffect(allowFocus) {
        if (allowFocus && hasFocusReassignBug) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(focused) {
        if (!focused && hasFocusReassignBug) {
            allowFocus = false
        }
    }
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && focused) {
            if (hasFocusReassignBug) {
                allowFocus = false
                delay(100)
                focusManager.clearFocus()
            } else {
                focusManager.clearFocus()
            }
        }
    }

    val currentOnTextLayout by rememberUpdatedState(onTextLayout)

    val showFloatingTitle = useLabelAsPlaceholder && (focused || state.text.isNotEmpty())
    val showPlaceholder = !showFloatingTitle && state.text.isEmpty() &&
        (placeholder != null || useLabelAsPlaceholder)
    val placeholderText = placeholder ?: title

    // 下划线透明度动画：始终占位，避免聚焦 / 失焦导致组件高度跳变
    val underlineAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "TextFieldUnderlineAlpha",
    )

    fun onClickInternal() {
        if (onClick != null) {
            onClick()
            return
        }

        if (!readOnly && enabled) {
            focusRequester.requestFocus()
        }
    }

    SettingsBaseWidget(
        modifier = modifier,
        title = if (useLabelAsPlaceholder) null else title,
        icon = null,
        iconPlaceholder = false,
        leadingContent = leadingContent,
        onClick = if (isClickableMode) {
            { onClickInternal() }
        } else {
            null
        },
        foreContent = {
            if (useLabelAsPlaceholder) {
                AnimatedVisibility(
                    visible = showFloatingTitle,
                    enter = slideInVertically(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        initialOffsetY = { it },
                    ) + fadeIn(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    ),
                    exit = slideOutVertically(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        targetOffsetY = { it },
                    ) + fadeOut(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    ),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
        descriptionColumnContent = {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusProperties {
                        canFocus = allowFocus && !isClickableMode
                    },
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle,
                cursorBrush = if (error.isBlank()) cursorBrush else SolidColor(MaterialTheme.colorScheme.error),
                keyboardOptions = keyboardOptions,
                onKeyboardAction = {
                    onKeyboardAction?.onKeyboardAction(it)
                    if (hasFocusReassignBug) {
                        coroutineScope.launch {
                            allowFocus = false
                            delay(100)
                            focusManager.clearFocus()
                        }
                    } else {
                        focusManager.clearFocus()
                    }
                },
                lineLimits = lineLimits,
                onTextLayout = currentOnTextLayout,
                interactionSource = interactionSource,
                inputTransformation = inputTransformation,
                outputTransformation = outputTransformation,
                scrollState = scrollState,
                decorator = { innerTextField ->
                    Column {
                        val placeholderAnimationScope = this
                        Box(
                            modifier = if (isClickableMode) {
                                Modifier.clickable {
                                    onClickInternal()
                                }
                            } else {
                                Modifier
                            }
                        ) {
                            placeholderAnimationScope.AnimatedVisibility(
                                visible = showPlaceholder,
                                enter = slideInVertically(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                    initialOffsetY = { -it },
                                ) + fadeIn(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                ),
                                exit = slideOutVertically(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                    targetOffsetY = { -it },
                                ) + fadeOut(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                ),
                            ) {
                                Text(
                                    text = placeholderText,
                                    style = textStyle,
                                    color = labelColor.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            innerTextField()
                        }

                        // 下划线始终占位，只动画透明度：避免聚焦时组件高度跳变
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .alpha(underlineAlpha)
                        ) {
                            HorizontalDivider(
                                thickness = 2.dp,
                                color = when {
                                    error.isNotBlank() -> MaterialTheme.colorScheme.error
                                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            )

            AnimatedVisibility(
                visible = error.isNotBlank(),
                enter = expandHorizontally(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    expandFrom = Alignment.Start
                ) + expandVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    expandFrom = Alignment.Top
                ),
                exit = shrinkHorizontally(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    shrinkTowards = Alignment.Start
                ) + shrinkVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    shrinkTowards = Alignment.Top
                )
            ) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailingContent = if (trailingContent != null) {
            {
                trailingContent()
            }
        } else null
    )
}
