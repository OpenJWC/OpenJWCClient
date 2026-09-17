package org.openjwc.client.ui.news

import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Attachment
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first

import androidx.compose.runtime.snapshotFlow

import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openjwc.client.R
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.ui.util.sharedBoundsWithNav
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.ui.component.MarkdownContent

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun NewsDetailScreen(
    navigator: Navigator,
    newsViewModel: NewsViewModel,
    /** true=作为共享元素目标（从资讯卡片打开）；false=普通页面（从聊天工具卡片打开，走全局预测返回）。 */
    sharedElement: Boolean = true,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val notice by newsViewModel.currentNewsToDisplay.collectAsState()
    val contentScrollState = rememberScrollState()

    // 详情页 entry 在打开查看器时会被 dispose：离开前保存滚动位置
    DisposableEffect(notice?.id) {
        val id = notice?.id
        onDispose { if (id != null) newsViewModel.saveDetailScroll(id, contentScrollState.value) }
    }

    // 正文异步解析 + 图片加载，布局高度是逐步长起来的：
    // 等可滚动范围够大再恢复，避免被夹到 0（表现为「返回后回到开头」）
    LaunchedEffect(notice?.id) {
        val id = notice?.id ?: return@LaunchedEffect
        val saved = newsViewModel.detailScrollOffset(id)
        if (saved > 0) {
            runCatching { snapshotFlow { contentScrollState.maxValue }.first { it >= saved } }
            contentScrollState.scrollTo(saved)
        }
    }
    fun openUrl(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
    }

    // 打开动画（卡片→详情）播放期间不渲染 Markdown：正文布局很贵，动画期间渲染会掉帧，
    // 也会让正文里的图片参与转场。动画播完后正文再整块出现。
    // 状态放在 ViewModel：从图片查看器返回时详情页 entry 会重建，不能重放这段空白。
    val contentReady = newsViewModel.detailContentReady
    LaunchedEffect(notice?.id) {
        notice?.let { newsViewModel.prepareDetailContent(it.id) }
    }

    // 调试：详情页进入/退出状态（PreEnter/Visible/PostExit），用于对齐转场时间线
    val navAnimScope = androidx.navigation3.ui.LocalNavAnimatedContentScope.current
    val navFrom = navAnimScope.transition.currentState
    val navTo = navAnimScope.transition.targetState
    LaunchedEffect(navFrom, navTo) {
        org.openjwc.client.log.Logger.d("NavDetail", "transition $navFrom -> $navTo")
    }
    // 官方共享元素：与卡片同 key，卡片↔详情自动插值位置/尺寸（返回手势同样跟随）。
    // 聊天工具卡片入口（sharedElement=false）走全局预测返回，不做整页形变。
    val sharedNotice = notice
    val pageSharedModifier = if (sharedElement && sharedNotice != null) {
        Modifier.sharedBoundsWithNav("news-card-${sharedNotice.id}")
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
    Box(modifier = Modifier.fillMaxSize().then(pageSharedModifier)) {
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = notice?.title ?: stringResource(R.string.news_not_found),
                        maxLines = 1,
                        // 标题过长时滚动（跑马灯）而不是省略号
                        modifier = Modifier.basicMarquee(),
                    )
                },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (notice == null) {
            Column(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection).padding(innerPadding).padding(32.dp)) {
                Icon(Icons.TwoTone.Description, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.news_empty_egg), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            val n = notice!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(innerPadding)
                    .verticalScroll(contentScrollState)
                    .padding(16.dp),
            ) {
                // 整块内容限宽并居中：宽屏下日期、正文、附件对齐在同一条中轴线上
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = NEWS_DETAIL_MAX_WIDTH)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (n.date.isNotBlank()) Text(n.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    val content = n.contentText
                    if (content.isNullOrBlank()) {
                        Text(stringResource(R.string.no_detail_view_original), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    } else if (contentReady) {
                        // 动画期间不渲染 Markdown（重内容会掉帧），就绪后再整块出现
                        MarkdownContent(
                            content,
                            onImageClick = { imageUrl, _ ->
                                newsViewModel.openImageViewer(imageUrl)
                                navigator.push(Screen.ImageViewer)
                            },
                        )
                    }

                    val urls = n.attachmentUrls.orEmpty().filter { it.isNotBlank() }
                    if (urls.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.attachment_list_count, urls.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        urls.forEach { url ->
                            SettingsBaseWidget(icon = Icons.TwoTone.Attachment, title = url.substringAfterLast("/").ifBlank { url }, modifier = Modifier.fillMaxWidth(), onClick = { openUrl(url) }) {}
                        }
                    }
                    if (n.detailUrl.isNotBlank()) {
                        SettingsJumpPageWidget(icon = Icons.TwoTone.OpenInNew, title = stringResource(R.string.view_in_browser), onClick = { openUrl(n.detailUrl) })
                    }
                }
            }
        }
    }
    }
    }
}

/** 宽屏下资讯详情的最大内容宽度。 */
private val NEWS_DETAIL_MAX_WIDTH = 720.dp
