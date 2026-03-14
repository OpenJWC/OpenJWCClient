package org.openjwc.client.ui.me.settings.connection

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostScreen(
    initialHost: String,
    initialPort: Int,
    onConfirm: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    var host by remember { mutableStateOf(initialHost) }
    var portString by remember { mutableStateOf(initialPort.toString()) }

    // 校验逻辑
    val isHostValid = host.isNotBlank()
    val portInt = portString.toIntOrNull()
    val isPortValid = portInt != null && portInt in 0..65535
    val canSave = isHostValid && isPortValid

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                        onClick = { if (canSave) onConfirm(host, portInt) },
                        enabled = canSave
                    ) {
                        Text("保存")
                    }
                }
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

            // 端口号输入
            OutlinedTextField(
                value = portString,
                onValueChange = { newValue ->
                    // 只允许输入数字
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

            Text(
                text = "提示：请确保手机可以访问到该内网 IP，或已配置内网穿透。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}