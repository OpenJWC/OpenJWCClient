package org.openjwc.client.ui.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.ClipData
import android.content.Intent
import androidx.compose.ui.platform.ClipEntry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.openjwc.client.R
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.navigation.MainTab
import org.openjwc.client.viewmodels.ChatSessionState
import org.openjwc.client.viewmodels.ChatSessionUiModel
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget

@Composable
fun ChatHistoryList(
    sessions: List<ChatSessionUiModel>,
    currentSessionId: Long?,
    onNewChat: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSessionMetadata: (ChatMetadata) -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        val isNewSelected = currentSessionId == null
        Surface(
            onClick = onNewChat,
            shape = RoundedCornerShape(14.dp),
            color = if (isNewSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, Modifier.size(20.dp), tint = if (isNewSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.new_session), style = MaterialTheme.typography.titleSmall, color = if (isNewSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("No chat history", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(items = sessions, key = { _, s -> s.metadata.sessionId }) { _, session ->
                    ChatHistoryCard(
                        session = session,
                        isSelected = session.metadata.sessionId == currentSessionId,
                        onClick = { onSessionClick(session.metadata.sessionId) },
                        onDelete = { onDeleteSession(session.metadata.sessionId) },
                        onRename = { onUpdateSessionMetadata(session.metadata) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatHistoryCard(
    session: ChatSessionUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(CircleShape)) {
                when (session.state) {
                    is ChatSessionState.Loading, is ChatSessionState.Generating, is ChatSessionState.ToolCalling -> {
                        CircularWavyProgressIndicator(
                            Modifier.size(28.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            stroke = Stroke(8f)
                        )
                    }
                    is ChatSessionState.Error -> Icon(Icons.Default.ErrorOutline, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
                    else -> Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(22.dp), tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    session.metadata.title.ifBlank { stringResource(R.string.untitled_session) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null, Modifier.size(18.dp), tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit_session_name)) }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_session), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showDeleteConfirm = true }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_session)) },
            text = { Text("Delete \"${session.metadata.title.ifBlank { "Untitled" }}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** The old ChatMainContent is below — keep it unchanged **/

@Composable
fun ChatMainContent(
    chatViewModel: ChatViewModel,
    mainViewModel: MainViewModel,
    newsViewModel: NewsViewModel,
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

    // Bottom sheet for news attachment
    var showNewsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showNewsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewsSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            NewsAttachmentSheet(
                newsViewModel = newsViewModel,
                onSelect = { notice ->
                    chatViewModel.addAttachment(notice)
                    showNewsSheet = false
                }
            )
        }
    }

    val horizontalPadding = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 12.dp
        WindowWidthSizeClass.Medium -> 32.dp
        WindowWidthSizeClass.Expanded -> 64.dp
        else -> 16.dp
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding).padding(horizontal = horizontalPadding)
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
            onAddAttachment = { showNewsSheet = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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

@Preview
@Composable
fun TestChatHistoryList() {
    ChatHistoryList(
        sessions = listOf(ChatSessionUiModel(metadata = ChatMetadata(sessionId = 1, title = "Session 1"), state = ChatSessionState.Idle)),
        currentSessionId = 1,
        onNewChat = {}, onSessionClick = {}, onDeleteSession = {}, onUpdateSessionMetadata = {}, modifier = Modifier
    )
}

@Composable
private fun NewsAttachmentSheet(
    newsViewModel: NewsViewModel,
    onSelect: (FetchedNotice) -> Unit
) {
    LaunchedEffect(Unit) { newsViewModel.loadLabels() }
    val labels = newsViewModel.labels.collectAsStateWithLifecycle().value
    var selectedLabel by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Attach News", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))

        if (labels.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Loading...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            if (selectedLabel.isEmpty()) selectedLabel = labels.first()

            // Label tabs
            LazyColumn(Modifier.fillMaxWidth().height(48.dp)) {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(labels) { label ->
                            val isSel = label == selectedLabel
                            Surface(
                                onClick = { selectedLabel = label },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // News list - scrollable
            var loadTrigger by remember { mutableIntStateOf(0) }
            LaunchedEffect(selectedLabel) { newsViewModel.loadCategory(selectedLabel); loadTrigger++ }
            val notices = newsViewModel.getNewsState(selectedLabel)

            if (notices.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Loading...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    item {
                        SegmentedColumn {
                            notices.forEach { notice ->
                                item {
                                    SettingsBaseWidget(
                                        icon = Icons.Default.Newspaper,
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        title = notice.title,
                                        description = notice.date,
                                        onClick = { onSelect(notice) }
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}
