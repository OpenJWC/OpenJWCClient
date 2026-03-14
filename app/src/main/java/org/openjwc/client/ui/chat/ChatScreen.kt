package org.openjwc.client.ui.chat

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.net.chat.ChatMessage
import org.openjwc.client.viewmodels.ChatEvent
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.SendMessageState

@Composable
fun ChatScreen(
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
    // TODO: windowSizeClass 还没用到，即将在支持多会话之后用来区分手机和平板左侧的列表形式
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val horizontalPadding = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 12.dp  // 手机：窄边距
        WindowWidthSizeClass.Medium -> 32.dp   // 折叠屏/小平板：中等边距
        WindowWidthSizeClass.Expanded -> 100.dp // 大平板：宽边距，让内容居中
        else -> 16.dp
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sendMessageState by viewModel.sendMessageState.collectAsStateWithLifecycle()
    // 如果发送了信息，则自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
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
            horizontalPadding = 0.dp
        )

        ChatInputBar(
            onSendMessage = { message -> viewModel.sendMessage(message) },
            onAttachment = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .imePadding(),
            sendButtonEnabled = sendMessageState is SendMessageState.Idle
        )
    }
}

@Composable
fun ChatList(
    listState: LazyListState,
    chatMessages: List<ChatMessage>,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .padding(horizontal = horizontalPadding),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chatMessages) { message ->
            MessageBubble(message)
        }
    }
}


