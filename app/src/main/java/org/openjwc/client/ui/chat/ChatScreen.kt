package org.openjwc.client.ui.chat

import android.content.ClipData
import android.content.Intent
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.openjwc.client.R
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata

import org.openjwc.client.navigation.MainTab
import org.openjwc.client.ui.theme.CardConfig
import org.openjwc.client.viewmodels.ChatSessionState
import org.openjwc.client.viewmodels.ChatSessionUiModel
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel

@Composable
fun ChatMainContent(
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
    val scope = rememberCoroutineScope()
    val addAttachmentHint = stringResource(R.string.add_attachment_hint)

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
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (messages.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.start_new_chat), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                ChatList(
                    listState = listState,
                    chatMessages = messages,
                    sessionState = sessionState,
                    onCopy = { msg ->
                        chatViewModel.copyMessage(msg)
                        scope.launch { clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText(msg.text, msg.text))) }
                    },
                    onShare = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, it.text); type = "text/plain" }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    },
                    onDelete = { chatViewModel.deleteMessage(it.messageId) }
                )
            }
        }

        ChatInputBar(
            textValue = chatViewModel.inputText.collectAsState().value,
            onSendMessage = { chatViewModel.sendMessage() },
            onTextChange = { chatViewModel.updateInputText(it) },
            onAddAttachment = {
                mainViewModel.updateTab(MainTab.News)
                Toast.makeText(context, addAttachmentHint, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).imePadding(),
            isSending = sessionState !is ChatSessionState.Idle && sessionState !is ChatSessionState.Error,
            attachments = chatViewModel.attachments.collectAsStateWithLifecycle().value,
            onDeleteAttachment = { chatViewModel.deleteAttachment(it) },
        )
    }
}

@Composable
fun ChatList(
    listState: androidx.compose.foundation.lazy.LazyListState,
    chatMessages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    sessionState: ChatSessionState,
    onDelete: (ChatMessage) -> Unit = {},
    onCopy: (ChatMessage) -> Unit = {},
    onShare: (ChatMessage) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val showBackToBottom by remember { derivedStateOf { val li = listState.layoutInfo; val l = li.visibleItemsInfo.lastOrNull(); li.totalItemsCount > 0 && (l?.index ?: 0) < li.totalItemsCount - 1 } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val bubbleMaxFraction = 0.85f

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(chatMessages) { index, message ->
                val isLast = index == chatMessages.lastIndex
                val isLoading = isLast && (sessionState is ChatSessionState.Loading || sessionState is ChatSessionState.ToolCalling || sessionState is ChatSessionState.Generating)
                MessageBubble(message = message, isLoading = isLoading, onCopy = { onCopy(message) }, onShare = { onShare(message) }, onDelete = { onDelete(message) }, maxWidthFraction = bubbleMaxFraction)
            }
        }

        AnimatedVisibility(visible = showBackToBottom, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(), modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
            FloatingActionButton(onClick = { scope.launch { if (chatMessages.isNotEmpty()) listState.animateScrollToItem(chatMessages.size - 1) } }, containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Icon(Icons.Default.ArrowDownward, stringResource(R.string.bottom))
            }
        }
    }
}

@Composable
fun ChatHistoryList(sessions: List<ChatSessionUiModel>, currentSessionId: Long?, onNewChat: () -> Unit, onSessionClick: (Long) -> Unit, onDeleteSession: (Long) -> Unit, onUpdateSessionMetadata: (ChatMetadata) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = CardConfig.cardAlpha), modifier = Modifier.fillMaxWidth()) {
                NavigationDrawerItem(label = { Text(stringResource(R.string.new_session)) }, selected = currentSessionId == null, onClick = onNewChat, icon = { Icon(Icons.Default.Add, null) }, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
        itemsIndexed(items = sessions, key = { _, s -> s.metadata.sessionId }) { _, session ->
            ChatHistoryItem(session = session, isSelected = session.metadata.sessionId == currentSessionId, onSessionClick = onSessionClick, onDeleteSession = onDeleteSession, onUpdateSessionMetadata = onUpdateSessionMetadata)
        }
    }
}

@Composable
fun ChatHistoryItem(session: ChatSessionUiModel, isSelected: Boolean, onSessionClick: (Long) -> Unit, onDeleteSession: (Long) -> Unit, onUpdateSessionMetadata: (ChatMetadata) -> Unit) {
    var showMenu by remember(isSelected) { mutableStateOf(false) }
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = CardConfig.cardAlpha) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = CardConfig.cardAlpha), modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            NavigationDrawerItem(
                label = { Text(session.metadata.title.ifBlank { stringResource(R.string.untitled_session) }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                selected = isSelected,
                onClick = { onSessionClick(session.metadata.sessionId) },
                icon = {
                    when (session.state) {
                        is ChatSessionState.Loading, is ChatSessionState.Generating, is ChatSessionState.ToolCalling -> Box(contentAlignment = Alignment.Center) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp), color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary, stroke = Stroke(8f))
                        }
                        is ChatSessionState.Error -> Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        else -> Icon(Icons.Default.ChatBubbleOutline, null)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more)) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.edit_session_name)) }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { onUpdateSessionMetadata(session.metadata); showMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete_session), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { onDeleteSession(session.metadata.sessionId); showMenu = false })
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TestChatHistoryList() {
    ChatHistoryList(sessions = listOf(ChatSessionUiModel(metadata = ChatMetadata(sessionId = 1, title = "Session 1"), state = ChatSessionState.Idle)), currentSessionId = 1, onNewChat = {}, onSessionClick = {}, onDeleteSession = {}, onUpdateSessionMetadata = {}, modifier = Modifier)
}
