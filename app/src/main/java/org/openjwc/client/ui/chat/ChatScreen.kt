package org.openjwc.client.ui.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatSession
import org.openjwc.client.ui.main.MainTab
import org.openjwc.client.viewmodels.ChatSessionState
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel


@Composable
fun ChatList(
    listState: LazyListState,
    chatMessages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    sessionState: ChatSessionState,
    onDelete: (ChatMessage) -> Unit = {},
    onCopy: (ChatMessage) -> Unit = {},
    onShare: (ChatMessage) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val showBackToBottom by remember {
        derivedStateOf {

            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            if (totalItems == 0) {
                false
            } else {
                (lastVisibleItem?.index ?: 0) < totalItems - 1
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val bubbleMaxWidth = maxWidth * 0.85f

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(chatMessages) { index, message ->
                val isLastMessage = index == chatMessages.lastIndex
                val isVisualLoading = isLastMessage &&
                        (sessionState is ChatSessionState.Loading ||
                                sessionState is ChatSessionState.ToolCalling ||
                                sessionState is ChatSessionState.Generating)
                val isSending = isVisualLoading

                MessageBubble(
                    message = message,
                    isLoading = isSending,
                    onCopy = onCopy,
                    onShare = onShare,
                    onDelete = onDelete,
                    maxWidth = bubbleMaxWidth
                )
            }
        }
        BackToBottomButton(
            visible = showBackToBottom,
            onClick = {
                scope.launch {
                    if (chatMessages.isNotEmpty()) {
                        listState.animateScrollToItem(chatMessages.size - 1)
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}


@Composable
fun BackToBottomButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier.padding(12.dp)
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Bottom")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    sessionId: Long? = null,
    windowSizeClass: WindowSizeClass,
    drawerState: DrawerState,
    chatViewModel: ChatViewModel,
    mainViewModel: MainViewModel,
    contentPadding: PaddingValues,
) {
    val historySessions by chatViewModel.allSessions.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    LocalContext.current
    val showEditMetadataDialog = remember { MutableStateFlow(false) }

//    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val drawerContent = @Composable {
        ModalDrawerSheet(windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top)) {
            ChatHistoryList(
                sessions = historySessions,
                currentSessionId = sessionId,
                onSessionClick = { id ->
                    chatViewModel.loadSession(id)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    chatViewModel.toNewChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { id -> chatViewModel.deleteSession(id) },
                onUpdateSessionMetadata = {
                    showEditMetadataDialog.value = true
                },
                // 确保它占据可用空间
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = contentPadding.calculateTopPadding())
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            ChatMainContent(
                chatViewModel = chatViewModel,
                windowSizeClass = windowSizeClass,
                contentPadding = contentPadding,
                mainViewModel = mainViewModel
            )
        }
    }

    if (showEditMetadataDialog.collectAsState().value) {
        EditMetadataDialog(
            onDismiss = { showEditMetadataDialog.value = false },
            onConfirm = { newTitle ->
                showEditMetadataDialog.value = false
                chatViewModel.currentSessionMetadata.value?.let {
                    chatViewModel.updateMetadata(it.copy(title = newTitle))
                }
            },
            initialTitle = chatViewModel.currentSessionMetadata.value?.title ?: ""
        )
    }
}

@Composable
fun ChatHistoryList(
    sessions: List<ChatSession>,
    currentSessionId: Long?,
    onNewChat: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSessionMetadata: (ChatMetadata) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            NavigationDrawerItem(
                label = { Text("新建会话") },
                selected = currentSessionId == null,
                onClick = onNewChat,
                icon = { Icon(Icons.Default.Add, null) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

        }
        items(
            items = sessions,
            key = { it.metadata.sessionId }
        ) { session ->
            ChatHistoryItem(
                session = session,
                isSelected = session.metadata.sessionId == currentSessionId,
                onSessionClick = onSessionClick,
                onDeleteSession = onDeleteSession,
                onUpdateSessionMetadata = onUpdateSessionMetadata
            )
        }
    }
}

@Composable
fun ChatHistoryItem(
    session: ChatSession,
    isSelected: Boolean,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSessionMetadata: (ChatMetadata) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

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
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Box {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "编辑会话名称",
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onUpdateSessionMetadata(session.metadata)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "删除会话",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onDeleteSession(session.metadata.sessionId)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMainContent(
    chatViewModel: ChatViewModel,
    mainViewModel: MainViewModel,
    windowSizeClass: WindowSizeClass,
    contentPadding: PaddingValues
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val currentMetadata by chatViewModel.currentSessionMetadata.collectAsStateWithLifecycle()
    val sessionState by chatViewModel.getSessionState(currentMetadata?.sessionId).collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current

    val horizontalPadding = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 12.dp
        WindowWidthSizeClass.Medium -> 32.dp
        WindowWidthSizeClass.Expanded -> 64.dp
        else -> 16.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding)
            .padding(horizontal = horizontalPadding)
    ) {
        ChatList(
            listState = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            chatMessages = messages,
            sessionState = sessionState,
            onCopy = {
                chatViewModel.copyMessage(it, clipboardManager)
            },
            onShare = { /*TODO: viewModel.shareMessage(it)*/ },
            onDelete = { chatViewModel.deleteMessage(it.messageId) }
        )

        ChatInputBar(
            textValue = chatViewModel.inputText.collectAsState().value,
            onSendMessage = { chatViewModel.sendMessage() },
            onTextChange = { chatViewModel.updateInputText(it) },
            onAddAttachment = {
                mainViewModel.updateTab(MainTab.News)
                Toast.makeText(context, "请长按资讯卡片添加附件", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .imePadding(),
            isSending = sessionState !is ChatSessionState.Idle && sessionState !is ChatSessionState.Error,
            attachments = chatViewModel.attachments.collectAsStateWithLifecycle().value,
            onDeleteAttachment = {chatViewModel.deleteAttachment(it)},
        )
    }
}