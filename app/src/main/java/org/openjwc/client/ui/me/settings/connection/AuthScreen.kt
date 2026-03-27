package org.openjwc.client.ui.me.settings.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.net.models.DevicesQueryResponseData
import org.openjwc.client.net.models.DevicesUnbindSuccessResponse
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse


@Preview
@Composable
fun TestAuthScreen() {
    AuthScreen(
        initialAuthKey = "",
        onConfirm = {},
        onBack = {},
        onRefreshDevices = {},
        onUnbindDevice = {},
        thisDeviceId = "1",
        isLoadingDeviceIds = false,
        devicesResult = NetworkResult.Success(
            response = SuccessResponse(
                message = "success",
                data = DevicesQueryResponseData(
                    limitedDeviceCount = 3,
                    deviceIDs = emptyList()
                )
            )
        ),
        unbindResult = NetworkResult.Success(DevicesUnbindSuccessResponse(""))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    initialAuthKey: String,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
    onRefreshDevices: () -> Unit,
    isLoadingDeviceIds: Boolean,
    onUnbindDevice: (String) -> Unit,
    thisDeviceId: String,
    devicesResult: NetworkResult<SuccessResponse<DevicesQueryResponseData>>,
    unbindResult: NetworkResult<DevicesUnbindSuccessResponse>,
) {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var authKey by remember { mutableStateOf(initialAuthKey) }
    var deviceToUnbind by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialAuthKey) {
        authKey = initialAuthKey
        onRefreshDevices()
    }

    val isAuthKeyValid = authKey.isNotBlank()
    val isChanged = initialAuthKey != authKey
    val canSave = isAuthKeyValid/* && isChanged*/

    if (deviceToUnbind != null) {
        AlertDialog(
            onDismissRequest = { deviceToUnbind = null },
            title = { Text("确认解绑") },
            text = { Text("确定要解绑设备 [${deviceToUnbind}] 吗？解绑后该设备将无法访问服务。") },
            confirmButton = {
                TextButton(onClick = {
                    deviceToUnbind?.let { id ->
                        onUnbindDevice(id)
                    }
                    deviceToUnbind = null
                }) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToUnbind = null }) {
                    Text("取消")
                }
            }
        )
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("鉴权设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (canSave) onConfirm(authKey) },
                        enabled = canSave
                    ) {
                        Text("保存")
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = authKey,
                onValueChange = { input ->
                    val filteredInput = input.filter { it.code <= 127 }
                    authKey = filteredInput.trim()
                },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isAuthKeyValid,
                singleLine = true,
                supportingText = {
                    if (!isAuthKeyValid) Text("API Key不能为空")
                }
            )

            Text(
                text = "请联系管理员获取 API Key。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (isLoadingDeviceIds) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                // --- 设备列表部分 ---
                when (devicesResult) {
                    is NetworkResult.Success -> {
                        val deviceIds = devicesResult.response.data.deviceIDs
                        val limitedCount = devicesResult.response.data.limitedDeviceCount
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "已绑定设备 (${deviceIds.size}/$limitedCount)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { onRefreshDevices() },
                                enabled = true
                            ) {
                                Text("刷新")
                            }
                        }
                        Text(
                            text = "请先点击保存再刷新设备列表。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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

                        if (devicesResult.response.data.deviceIDs.isEmpty()) {
                            Text("暂无绑定设备", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            deviceIds.forEach { id ->
                                DeviceItem(
                                    id = id,
                                    isCurrentDevice = id == thisDeviceId,
                                    onUnbindClick = { deviceToUnbind = id }
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
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
                Text(
                    text = "本机 ID: $thisDeviceId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

@Composable
fun DeviceItem(
    id: String,
    isCurrentDevice: Boolean,
    onUnbindClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = id,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            if (isCurrentDevice) {
                Text(
                    text = "当前设备",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        IconButton(onClick = onUnbindClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "解绑",
                tint = MaterialTheme.colorScheme.error
            )
        }
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
        TextButton(onClick = onRetry) {
            Text("重试")
        }
    }
}
