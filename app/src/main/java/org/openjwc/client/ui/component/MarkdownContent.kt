package org.openjwc.client.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownImage
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.compose.LocalReferenceLinkHandler
import com.mikepenz.markdown.model.rememberMarkdownState
import com.mikepenz.markdown.utils.resolveImageLink

/** 宽屏下的正文最大行宽；窄屏不受影响。 */
private val MAX_CONTENT_WIDTH = 720.dp


/**
 * 统一 Markdown 渲染入口（mikepenz multiplatform-markdown-renderer，Material 3 主题）。
 * - 带代码高亮与文本选择，链接由库内 Compose LinkAnnotation 处理，可直接点击；
 * - 图片由库自带的 Coil3 image transformer 加载（coil-network-okhttp 自动注册网络 fetcher），块级图片居中；
 * - 段落留白收紧，长通知（每行一个 `<p>`）读起来更紧凑；
 * - 宽屏上限行宽并居中，避免一行拉得太长。
 * retainState = true：内容更新时保留旧内容，避免解析期间闪成加载态。
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    /** 点击正文图片时回调「图片地址 + 图片在 window 中的位置」；为 null 时图片不可点。 */
    onImageClick: ((String, Rect) -> Unit)? = null,
) {
    val normalized = remember(markdown) { MarkdownLinkNormalizer.normalize(markdown) }
    val markdownState = rememberMarkdownState(normalized, retainState = true)
    // 外层必须填满宽度，否则 Box 只有内容宽，居中不会生效
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        SelectionContainer {
            Markdown(
                markdownState = markdownState,
                modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH),
                // 默认标题偏大，这里收敛到更贴近正文层级的 M3 样式
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.headlineSmall,
                    h2 = MaterialTheme.typography.titleLarge,
                    h3 = MaterialTheme.typography.titleMedium,
                    h4 = MaterialTheme.typography.titleSmall,
                    h5 = MaterialTheme.typography.titleSmall,
                    h6 = MaterialTheme.typography.titleSmall,
                    paragraph = MaterialTheme.typography.bodyLarge,
                ),
                components = markdownComponents(
                    codeBlock = highlightedCodeBlock,
                    codeFence = highlightedCodeFence,
                    // 默认段落留白偏大，长通知读起来很散
                    paragraph = { model ->
                        MarkdownParagraph(
                            content = model.content,
                            node = model.node,
                            modifier = Modifier.padding(vertical = 2.dp),
                            style = model.typography.paragraph,
                        )
                    },
                    // 块级图片居中（仍用库自带的渲染 + Coil3 transformer，只套一层居中容器）
                    image = { model ->
                        // 库对每个图片节点复用同一个组件 lambda，必须用 key() 隔离组合位置，
                        // 否则所有图片会共用同一份 remember 状态（导致点一张、其它张一起动）
                        key(model.node) {
                        // 必须用库自己的解析：model.content 是整篇 markdown（不是单张图），
                        // 只有结合 model.node 才能定位到这一张的地址。
                        // 之前用正则从 content 里取，导致所有图片都解析成第一张的 URL，
                        // 共享元素 key 全部相同 → 点一张、其它张一起动。
                        // 注意：ReferenceLinkHandler 只在 Markdown(...) 内部提供，必须在这里读
                        val refHandler = LocalReferenceLinkHandler.current
                        val imageUrl = remember(model.node, model.content, refHandler) {
                            runCatching { model.node.resolveImageLink(model.content, refHandler) }
                                .getOrNull()
                                ?.takeIf { it.isNotBlank() }
                        }
                        var imageBounds by remember { mutableStateOf(Rect.Zero) }
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { imageBounds = it.boundsInWindow() }
                                    .then(
                                        if (onImageClick != null && imageUrl != null) {
                                            Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) { onImageClick(imageUrl, imageBounds) }
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                MarkdownImage(content = model.content, node = model.node)
                            }
                        }
                        }
                    },
                ),
                imageTransformer = Coil3ImageTransformerImpl,
            )
        }
    }
}
