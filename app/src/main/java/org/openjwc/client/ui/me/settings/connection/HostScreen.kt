package org.openjwc.client.ui.me.settings.connection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.net.models.Proxy

@Preview
@Composable
fun TestHostScreen() {
    val proxy by remember { mutableStateOf(Proxy.NoProxy()) }
    HostScreen(
        initialHost = "127.0.0.1",
        initialPort = 8,
        initialProxy = proxy,
        onConfirm = { _, _, _, _ -> },
        onBack = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostScreen(
    initialHost: String,
    initialPort: Int,
    initialUseHttp: Boolean = false,
    initialProxy: Proxy,
    onConfirm: (String, Int, Boolean, Proxy) -> Unit,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var host by remember { mutableStateOf(initialHost) }
    var portString by remember { mutableStateOf(initialPort.toString()) }
    var useHttp by remember { mutableStateOf(initialUseHttp) }

    var proxyType by remember {
        mutableIntStateOf(
            when (initialProxy) {
                is Proxy.NoProxy -> 0
                is Proxy.HttpProxy -> 1
                is Proxy.SocksProxy -> 2
            }
        )
    }
    var proxyHost by remember {
        mutableStateOf(
            when (initialProxy) {
                is Proxy.HttpProxy -> initialProxy.host
                is Proxy.SocksProxy -> initialProxy.host
                else -> "127.0.0.1"
            }
        )
    }
    var proxyPortString by remember {
        mutableStateOf(
            when (initialProxy) {
                is Proxy.HttpProxy -> initialProxy.port.toString()
                is Proxy.SocksProxy -> initialProxy.port.toString()
                else -> "8888"
            }
        )
    }

    val isHostValid = host.isNotBlank()
    val portInt = portString.toIntOrNull()
    val isPortValid = portInt != null && portInt in 0..65535

    val proxyPortInt = proxyPortString.toIntOrNull()
    val isProxyValid =
        proxyType == 0 || (proxyHost.isNotBlank() && proxyPortInt != null && proxyPortInt in 0..65535)

    val currentProxy = when (proxyType) {
        1 -> Proxy.HttpProxy(proxyHost, proxyPortInt ?: 8888)
        2 -> Proxy.SocksProxy(proxyHost, proxyPortInt ?: 1080)
        else -> Proxy.NoProxy()
    }

    val isChanged =
        initialHost != host || initialPort != portInt || initialUseHttp != useHttp || initialProxy != currentProxy
    val canSave = isHostValid && isPortValid && isProxyValid && isChanged

    LaunchedEffect(initialHost, initialPort, initialUseHttp, initialProxy) {
        host = initialHost
        portString = initialPort.toString()
        useHttp = initialUseHttp
        proxyType = when (initialProxy) {
            is Proxy.NoProxy -> 0
            is Proxy.HttpProxy -> 1
            is Proxy.SocksProxy -> 2
        }
        proxyHost = when (initialProxy) {
            is Proxy.HttpProxy -> initialProxy.host
            is Proxy.SocksProxy -> initialProxy.host
            else -> "127.0.0.1"
        }
        proxyPortString = when (initialProxy) {
            is Proxy.HttpProxy -> initialProxy.port.toString()
            is Proxy.SocksProxy -> initialProxy.port.toString()
            else -> "8888"
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("网络配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (canSave) onConfirm(host, portInt, useHttp, currentProxy) },
                        enabled = canSave
                    ) { Text("保存") }
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
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "服务器配置",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it.trim() },
                        label = { Text("主机地址") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isHostValid,
                        supportingText = {
                            if (!isHostValid) {
                                Text("服务器地址不能为空")
                            }
                        },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = portString,
                        onValueChange = { if (it.all { c -> c.isDigit() }) portString = it },
                        label = { Text("端口号") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isPortValid,
                        supportingText = {
                            if (!isPortValid) {
                                Text("端口应在 0-65535 之间")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        "代理设置",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))


                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf(
                            Triple(0, "无", Icons.Default.LinkOff),
                            Triple(1, "HTTP", Icons.Default.Http),
                            Triple(2, "SOCKS", Icons.Default.SettingsInputComponent)
                        )

                        options.forEachIndexed { index, (type, label, icon) ->
                            val isSelected = proxyType == type
                            SegmentedButton(
                                selected = isSelected,
                                onClick = { proxyType = type },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                icon = {
                                    Crossfade(
                                        targetState = isSelected,
                                        label = "ProxyIconSwitch"
                                    ) { target ->
                                        Icon(
                                            imageVector = if (target) Icons.Default.Check else icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = proxyType != 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            val ipv4Pattern = remember {
                                Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$")
                            }
                            val isProxyHostError =
                                proxyHost.isBlank() || !ipv4Pattern.matches(proxyHost)
                            val isProxyPortError = proxyPortInt == null || proxyPortInt !in 0..65535

                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = proxyHost,
                                onValueChange = { proxyHost = it.trim() },
                                label = { Text("代理主机") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = isProxyHostError,
                                supportingText = { if (isProxyHostError) Text("代理地址目前只支持 IPv4 格式") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = proxyPortString,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) proxyPortString = it
                                },
                                label = { Text("代理端口") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = isProxyPortError,
                                supportingText = { if (isProxyPortError) Text("端口需在 0-65535 之间") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("使用不安全的 HTTP 连接", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "关闭后将强制使用 HTTPS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = useHttp, onCheckedChange = { useHttp = it })
                }
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