package org.openjwc.client.ui.me.settings.auth

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.data.datastore.AuthSession
import org.openjwc.client.net.models.DeviceQuery
import org.openjwc.client.net.models.DevicesQueryResponseData
import org.openjwc.client.net.models.DevicesUnbindSuccessResponse
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse


@Preview
@Composable
fun TestAccountScreen() {
    AccountScreen(
        onBack = {},
        onRefreshDevices = {},
        onUnbindDevice = {},
        thisDeviceId = "1",
        isLoadingDeviceIds = false,
        devicesResult = NetworkResult.Success(
            response = SuccessResponse(
                message = "success",
                data = DevicesQueryResponseData(
//                    limitedDeviceCount = 3,
                    deviceQueries = listOf(
                        DeviceQuery(
                            deviceUUID = "1",
                            deviceName = "测试设备",
                            lastLogin = "2023-01-01",
                        )
                    )
                )
            )
        ),
        unbindResult = NetworkResult.Success(DevicesUnbindSuccessResponse("")),
        authSession = AuthSession(
            username = "test",
            email = "georgeclinton@my-own-personal-domain.com",
            uuid = "test_uuid",
            deviceName = "test_device",
            isLoggedIn = true
        ),
        onLogin = {},
        onRegister = {},
        onLogout = {}
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    authSession: AuthSession,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onLogout: () -> Unit,
    onRefreshDevices: () -> Unit,
    isLoadingDeviceIds: Boolean,
    onUnbindDevice: (String) -> Unit,
    thisDeviceId: String,
    devicesResult: NetworkResult<SuccessResponse<DevicesQueryResponseData>>,
    unbindResult: NetworkResult<DevicesUnbindSuccessResponse>,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var deviceToUnbind by remember { mutableStateOf<DeviceQuery?>(null) }

    LaunchedEffect(authSession.isLoggedIn) {
        if (authSession.isLoggedIn) {
            onRefreshDevices()
        }
    }

    if (deviceToUnbind != null) {
        AlertDialog(
            onDismissRequest = { deviceToUnbind = null },
            title = { Text("确认解绑") },
            text = { Text("确定要解绑设备 [${deviceToUnbind?.deviceName ?: "Unknown Device"}] 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    deviceToUnbind?.let { onUnbindDevice(it.deviceUUID) }
                    deviceToUnbind = null
                }) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deviceToUnbind = null }) { Text("取消") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(if (authSession.isLoggedIn) "账号管理" else "账号") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            AccountBox(
                authSession = authSession,
                onLogin = onLogin,
                onLogout = onLogout,
                onRegister = onRegister
            )

            if (authSession.isLoggedIn) {
                DeviceList(
                    thisDeviceId = thisDeviceId,
                    isLoadingDeviceIds = isLoadingDeviceIds,
                    devicesResult = devicesResult,
                    unbindResult = unbindResult,
                    onRefreshDevices = onRefreshDevices,
                    onUnbind = { deviceToUnbind = it }
                )
            } else {
                Text(
                    text = "登录后即可查看和管理已授权的设备列表。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


@Preview
@Composable
fun TestAccountBox() {
    AccountBox(
        authSession = AuthSession(),
        onLogin = {},
        onLogout = {},
        onRegister = {}
    )
}

@Composable
fun AccountBox(
    authSession: AuthSession,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 用户头像/占位符
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = if (authSession.isLoggedIn) Icons.Default.Person else Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f).animateContentSize()) {
                    if (authSession.isLoggedIn) {
                        // 已登录状态
                        Text(
                            text = authSession.username ?: "未命名用户",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = authSession.email ?: "未绑定邮箱",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "设备: ${authSession.deviceName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text(
                            text = "欢迎使用",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (authSession.isLoggedIn) {
                    TextButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("退出登录")
                    }
                } else {
                    TextButton(onClick = onRegister) {
                        Text("立即注册")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onLogin,
                    ) {
                        Text("登录")
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceList(
    thisDeviceId: String,
    isLoadingDeviceIds: Boolean,
    devicesResult: NetworkResult<SuccessResponse<DevicesQueryResponseData>>,
    unbindResult: NetworkResult<DevicesUnbindSuccessResponse>,
    onRefreshDevices: () -> Unit,
    onUnbind: (DeviceQuery) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Text("设备管理", style = MaterialTheme.typography.titleLarge)
            /*if (devicesResult is NetworkResult.Success) {
                val data = devicesResult.response.data
                Text(
                    "${data.deviceQueries.size} / ${data.limitedDeviceCount} 个配额已使用",
                    style = MaterialTheme.typography.bodySmall
                )
            }*/
        }
        FilledTonalButton(onClick = onRefreshDevices) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("刷新")
        }
    }
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .animateContentSize()
        ) {
            if (isLoadingDeviceIds) {
                CircularWavyProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(24.dp)
                )
            } else {
                when (devicesResult) {
                    is NetworkResult.Success -> {
                        val deviceQueries = devicesResult.response.data.deviceQueries
                        when (unbindResult) {
                            is NetworkResult.Success -> {}
                            is NetworkResult.Failure -> {
                                Text(
                                    text = "解绑失败(${unbindResult.code}): ${unbindResult.msg}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            is NetworkResult.Error -> {
                                Text(
                                    text = "解绑失败: ${unbindResult.msg}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (devicesResult.response.data.deviceQueries.isEmpty()) {
                            Text(
                                "暂无绑定设备",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        } else {
                            deviceQueries.forEach { query ->
                                DeviceItem(
                                    name = query.deviceName,
                                    id = query.deviceUUID,
                                    isCurrentDevice = query.deviceUUID == thisDeviceId,
                                    onUnbindClick = {onUnbind(query)}
                                )
                            }
                        }
                    }

                    is NetworkResult.Error -> {
                        ErrorMessageView(
                            message = "查询设备失败: ${devicesResult.msg}",
                            onRetry = onRefreshDevices
                        )
                    }

                    is NetworkResult.Failure -> {
                        ErrorMessageView(
                            message = "查询设备失败(${devicesResult.code}): ${devicesResult.msg}",
                            onRetry = onRefreshDevices
                        )
                    }
                }
            }
        }
    }
    Text(
        text = "本机 ID: $thisDeviceId",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp)
    )
}


@Composable
fun DeviceItem(
    name: String,
    id: String,
    isCurrentDevice: Boolean,
    onUnbindClick: () -> Unit
) {
    Surface(
        color = if (isCurrentDevice) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        ListItem(
            headlineContent = {
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = id,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            supportingContent = {
                if (isCurrentDevice) {
                    Text(
                        text = "当前登录的设备",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            leadingContent = {
                Icon(
                    imageVector = if (isCurrentDevice) Icons.Default.PhonelinkSetup else Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = if (isCurrentDevice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                IconButton(onClick = onUnbindClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "解绑",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

@Composable
fun ErrorMessageView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = "若改动 API Key，请先保存设置再点击重试",
            style = MaterialTheme.typography.bodySmall,
        )
        FilledTonalButton(onClick = onRetry) {
            Text("重试")
        }
    }
}
