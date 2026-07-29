package org.openjwc.client.ui.me.settings.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Http
import androidx.compose.material.icons.twotone.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.net.models.Proxy
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsChooseWidget
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HostScreen(navigator: Navigator, settingsViewModel: SettingsViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val hostState = remember { TextFieldState(settings.host) }
    val portState = remember { TextFieldState(settings.port.toString()) }
    var useHttp by remember { mutableStateOf(settings.useHttp) }

    val currentProxy = settings.proxy
    var proxyType by remember { mutableIntStateOf(
        when (currentProxy) {
            is Proxy.HttpProxy -> 1
            is Proxy.SocksProxy -> 2
            else -> 0
        }
    ) }
    val proxyHost = when (currentProxy) {
        is Proxy.HttpProxy -> currentProxy.host
        is Proxy.SocksProxy -> currentProxy.host
        else -> ""
    }
    val proxyPort = when (currentProxy) {
        is Proxy.HttpProxy -> currentProxy.port.toString()
        is Proxy.SocksProxy -> currentProxy.port.toString()
        else -> "8080"
    }
    val proxyHostState = remember { TextFieldState(proxyHost) }
    val proxyPortState = remember { TextFieldState(proxyPort) }

    val showProxyFields = proxyType != 0
    val scrollState = rememberScrollState()

    val hostError = if (hostState.text.isBlank()) "Required" else ""
    val portError = run {
        val p = portState.text.toString().toIntOrNull()
        when {
            portState.text.isBlank() -> "Required"
            p == null || p !in 0..65535 -> "0–65535"
            else -> ""
        }
    }
    val proxyPortError = if (showProxyFields) {
        val p = proxyPortState.text.toString().toIntOrNull()
        when {
            proxyPortState.text.isBlank() -> "Required"
            p == null || p !in 0..65535 -> "0–65535"
            else -> ""
        }
    } else ""

    val isValid = hostError.isEmpty() && portError.isEmpty() && proxyPortError.isEmpty()

    fun save() {
        settingsViewModel.updateHost(hostState.text.toString())
        settingsViewModel.updatePort(portState.text.toString().toIntOrNull() ?: settings.port)
        settingsViewModel.updateUseHttp(useHttp)
        val proxy = when (proxyType) {
            1 -> Proxy.HttpProxy(proxyHostState.text.toString(), proxyPortState.text.toString().toIntOrNull() ?: 8080)
            2 -> Proxy.SocksProxy(proxyHostState.text.toString(), proxyPortState.text.toString().toIntOrNull() ?: 8080)
            else -> Proxy.NoProxy()
        }
        settingsViewModel.updateProxy(proxy)
        navigator.pop()
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.network_config)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(innerPadding)
        ) {
            SegmentedColumn(title = stringResource(R.string.server_config)) {
                item {
                    SettingsTextFieldWidget(
                        state = hostState,
                        title = "Host URL",
                        error = hostError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }
                item {
                    SettingsTextFieldWidget(
                        state = portState,
                        title = "Port",
                        error = portError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    SettingsChooseWidget(
                        icon = Icons.TwoTone.Http,
                        title = "Protocol",
                        items = listOf("HTTP", "HTTPS"),
                        selectedIndex = if (useHttp) 0 else 1,
                        onSelectedIndexChange = { useHttp = it == 0 }
                    )
                }
            }

            SegmentedColumn(title = "Proxy") {
                item {
                    SettingsChooseWidget(
                        icon = Icons.TwoTone.LinkOff,
                        title = "Proxy Type",
                        items = listOf("None", "HTTP Proxy", "SOCKS Proxy"),
                        selectedIndex = proxyType,
                        onSelectedIndexChange = { proxyType = it }
                    )
                }
                if (showProxyFields) {
                    item {
                        SettingsTextFieldWidget(
                            state = proxyHostState,
                            title = "Proxy Host",
                            error = if (proxyHostState.text.isBlank()) "Required" else "",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                    }
                    item {
                        SettingsTextFieldWidget(
                            state = proxyPortState,
                            title = "Proxy Port",
                            error = proxyPortError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            Button(
                onClick = { save() },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
