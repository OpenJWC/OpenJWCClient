package org.openjwc.client.ui.me.settings.connection

// ... 保持原有 import 不变
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostScreen(
    initialHost: String,
    initialPort: Int,
    initialUseHttp: Boolean = false,
    onConfirm: (String, Int, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var host by remember { mutableStateOf(initialHost) }
    var portString by remember { mutableStateOf(initialPort.toString()) }
    var useHttp by remember { mutableStateOf(initialUseHttp) }

    LaunchedEffect(initialHost, initialPort, initialUseHttp) {
        host = initialHost
        portString = initialPort.toString()
        useHttp = initialUseHttp
    }

    val isHostValid = host.isNotBlank()
    val portInt = portString.toIntOrNull()
    val isPortValid = portInt != null && portInt in 0..65535
    val isChanged = initialHost != host || initialPort != portInt || initialUseHttp != useHttp
    val canSave = isHostValid && isPortValid && isChanged

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("服务器配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (canSave) onConfirm(
                                host,
                                portInt,
                                useHttp
                            )
                        },
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
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text("主机地址") },
                placeholder = { Text("例如: 10.0.0.1 或 seu.edu.cn") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isHostValid,
                singleLine = true,
                supportingText = {
                    if (!isHostValid) Text("地址不能为空")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = portString,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        portString = newValue
                    }
                },
                label = { Text("端口号") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isPortValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    if (!isPortValid) Text("请输入有效的端口 (0-65535)")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "使用不安全的 HTTP 连接",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "关闭后将强制使用 HTTPS (推荐)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useHttp,
                    onCheckedChange = { useHttp = it }
                )
            }

            Text(
                text = "提示：请确保设备能够访问到该地址，且防火墙正确配置。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
