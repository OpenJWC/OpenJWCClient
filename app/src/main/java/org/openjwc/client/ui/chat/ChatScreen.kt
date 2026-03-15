package org.openjwc.client.ui.chat

import android.content.ClipData
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatSession
import org.openjwc.client.viewmodels.ChatEvent
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.SendMessageState
import androidx.compose.runtime.collectAsState


@Composable
fun ChatList(
    listState: LazyListState,
    chatMessages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    sendMessageState: SendMessageState,
    onDelete: (ChatMessage) -> Unit = {},
    onCopy: (ChatMessage) -> Unit = {},
    onShare: (ChatMessage) -> Unit = {}
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
                isLoading = isSending,
                onCopy = onCopy,
                onShare = onShare,
                onDelete = onDelete
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
    drawerState: DrawerState,
    viewModel: ChatViewModel
) {
    val historySessions by viewModel.allSessions.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val showEditMetadataDialog = remember { MutableStateFlow(false) }
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
//    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val drawerContent = @Composable {
        ModalDrawerSheet {
            ChatHistoryList(
                sessions = historySessions,
                currentSessionId = sessionId,
                onSessionClick = { id ->
                    viewModel.loadSession(id)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.toNewChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { id -> viewModel.deleteSession(id) },
                onUpdateSessionMetadata = {
                    showEditMetadataDialog.value = true
                }
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        modifier = Modifier
            .padding(contentPadding)
            .consumeWindowInsets(paddingValues = contentPadding)
    ) {
        Box(Modifier.fillMaxSize()) {
            ChatMainContent(
                viewModel = viewModel,
                windowSizeClass = windowSizeClass,
                contentPadding = PaddingValues()
            )
        }
    }

    if (showEditMetadataDialog.collectAsState().value) {
        EditMetadataDialog(
            onDismiss = { showEditMetadataDialog.value = false },
            onConfirm = { newTitle ->
                showEditMetadataDialog.value = false
                viewModel.currentSessionMetadata.value?.let {
                    viewModel.updateMetadata(it.copy(title = newTitle))
                }
            },
            initialTitle = viewModel.currentSessionMetadata.value?.title ?: ""
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
    viewModel: ChatViewModel,
    windowSizeClass: WindowSizeClass,
    contentPadding: PaddingValues
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sendMessageState by viewModel.sendMessageState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current

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
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            chatMessages = messages,
            sendMessageState = sendMessageState,
            onCopy = {

                viewModel.copyMessage(it, clipboardManager, context) },
            onShare = { /*TODO: viewModel.shareMessage(it)*/ },
            onDelete = { viewModel.deleteMessage(it.messageId) }
        )

        ChatInputBar(
            onSendMessage = { viewModel.sendMessage(it) },
            onAttachment = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .imePadding(),
            sendButtonEnabled = sendMessageState is SendMessageState.Idle
        )
    }
}