package org.openjwc.client.ui.chat

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.openjwc.client.net.chat.ChatMessage
import org.openjwc.client.net.chat.ChatSession
import org.openjwc.client.viewmodels.ChatEvent
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.SendMessageState


@Composable
fun ChatList(
    listState: LazyListState,
    chatMessages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    sendMessageState: SendMessageState
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(chatMessages) { index, message ->
            val isLastMessage = index == chatMessages.lastIndex
            val isSending = sendMessageState is SendMessageState.Sending && isLastMessage

            MessageBubble(
                message = message,
                isLoading = isSending
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: Long? = null,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
    viewModel: ChatViewModel
) {
    // 观察所有历史会话
    val historySessions by viewModel.allSessions.collectAsStateWithLifecycle(emptyList())
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val events by viewModel.events.collectAsStateWithLifecycle(null)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is ChatEvent.ShowSnackBar -> {
                    // TODO: 显示 SnackBar
                }
            }
        }
    }
    // 决定布局模式
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    // 定义侧边栏内容
    val drawerContent = @Composable {
        ModalDrawerSheet {
            ChatHistoryList(
                sessions = historySessions,
                currentSessionId = sessionId,
                onSessionClick = { id ->
                    viewModel.loadSession(id)
                    scope.launch { drawerState.close() }
                }
            )
        }
    }

    if (isExpanded) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(paddingValues = contentPadding)
        ) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(Modifier.width(300.dp)) {
                        ChatHistoryList(
                            sessions = historySessions,
                            currentSessionId = sessionId,
                            onSessionClick = { viewModel.loadSession(it) }
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                ChatMainContent(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    contentPadding = PaddingValues()
                )
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = drawerContent,
            modifier = Modifier.padding(contentPadding).consumeWindowInsets(paddingValues = contentPadding)
        ) {
            Box(Modifier.fillMaxSize()) {
                ChatMainContent(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    contentPadding = PaddingValues()
                )

                // TODO: 在外层加上菜单按钮
            }
        }
    }
}
@Composable
fun ChatHistoryList(
    sessions: List<ChatSession>,
    currentSessionId: Long?,
    onSessionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "历史会话",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(sessions) { session ->
            val isSelected = session.metadata.sessionId == currentSessionId
            NavigationDrawerItem(
                label = {
                    Text(
                        text = session.metadata.title.ifBlank { "无标题会话" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = isSelected,
                onClick = { onSessionClick(session.metadata.sessionId) },
                icon = { Icon(Icons.Default.ChatBubbleOutline, null) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun ChatMainContent(
    viewModel: ChatViewModel,
    windowSizeClass: WindowSizeClass,
    contentPadding: PaddingValues
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sendMessageState by viewModel.sendMessageState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val horizontalPadding = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 12.dp
        WindowWidthSizeClass.Medium -> 32.dp
        WindowWidthSizeClass.Expanded -> 64.dp // 既然左侧有列表了，右侧边距可以适当缩小
        else -> 16.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = horizontalPadding)
    ) {
        ChatList(
            listState = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            chatMessages = messages,
            sendMessageState = sendMessageState,
        )

        ChatInputBar(
            onSendMessage = { viewModel.sendMessage(it) },
            onAttachment = {},
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).imePadding(),
            sendButtonEnabled = sendMessageState is SendMessageState.Idle
        )
    }
}