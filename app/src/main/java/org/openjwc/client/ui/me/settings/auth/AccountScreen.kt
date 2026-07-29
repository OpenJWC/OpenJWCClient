package org.openjwc.client.ui.me.settings.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AccountCircle
import androidx.compose.material.icons.twotone.Logout
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.PhonelinkSetup
import androidx.compose.material.icons.twotone.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.R
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.CachedDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.net.models.DeviceQuery
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource
import org.openjwc.client.viewmodels.AuthViewModel
import org.openjwc.client.viewmodels.AuthViewModelFactory
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.SettingsViewModelFactory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountScreen(navigator: Navigator) {
    val context = LocalContext.current
    val authDataSource = remember { AuthDataSource(context) }
    val settingsDataSource = remember { SettingsDataSource(context) }
    val authRepository = remember { AuthRepository(authDataSource, settingsDataSource) }
    val settingsRepository = remember { SettingsRepository(settingsDataSource, CachedDataSource(context), authDataSource, context) }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsRepository, authRepository))

    val authSession by authViewModel.authSession.collectAsStateWithLifecycle()
    val deviceResult by settingsViewModel.deviceResult.collectAsStateWithLifecycle()
    val isLoadingDevices by settingsViewModel.isLoadingDeviceResult.collectAsStateWithLifecycle()

    var showDevices by remember { mutableStateOf(false) }
    var deviceToUnbind by remember { mutableStateOf<DeviceQuery?>(null) }

    LaunchedEffect(showDevices) {
        if (showDevices && authSession.isLoggedIn) {
            settingsViewModel.devicesQuery()
        }
    }

    if (deviceToUnbind != null) {
        AlertDialog(
            onDismissRequest = { deviceToUnbind = null },
            title = { Text(stringResource(R.string.confirm_unbind)) },
            text = { Text(stringResource(R.string.unbind_device_confirm_msg, deviceToUnbind?.deviceName ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    deviceToUnbind?.let { settingsViewModel.unbindAndRefresh(it.deviceUUID) }
                    deviceToUnbind = null
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToUnbind = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(stringResource(R.string.account_management)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .blurSource()
                .verticalScroll(rememberScrollState())
        ) {
            SegmentedColumn(title = stringResource(R.string.account)) {
                if (authSession.isLoggedIn && !authSession.username.isNullOrBlank()) {
                    item {
                        SettingsBaseWidget(
                            icon = Icons.TwoTone.AccountCircle,
                            title = authSession.username ?: "",
                            description = authSession.email ?: ""
                        ) {}
                    }
                    item {
                        SettingsBaseWidget(
                            icon = Icons.TwoTone.PhonelinkSetup,
                            title = stringResource(R.string.device_management),
                            onClick = { showDevices = !showDevices }
                        )
                    }
                    item {
                        SettingsBaseWidget(
                            icon = Icons.TwoTone.Logout,
                            title = stringResource(R.string.logout),
                            onClick = { authViewModel.logout() }
                        )
                    }
                } else {
                    item {
                        SettingsBaseWidget(
                            icon = Icons.TwoTone.Person,
                            title = stringResource(R.string.not_logged_in),
                            iconPlaceholder = false
                        ) {}
                    }
                }
            }

            AnimatedVisibility(visible = showDevices && authSession.isLoggedIn) {
                Column {
                    if (isLoadingDevices) {
                        SegmentedColumn(title = stringResource(R.string.device_management)) {
                            item {
                                SettingsBaseWidget(title = stringResource(R.string.loading)) {}
                            }
                        }
                    } else when (val result = deviceResult) {
                        is NetworkResult.Success -> {
                            val devices = result.response.data.deviceQueries
                            SegmentedColumn(title = stringResource(R.string.device_management)) {
                                if (devices.isEmpty()) {
                                    item {
                                        SettingsBaseWidget(title = stringResource(R.string.no_bound_devices)) {}
                                    }
                                } else {
                                    devices.forEach { device ->
                                        val isCurrent = device.deviceUUID == authSession.uuid
                                        item {
                                            SettingsBaseWidget(
                                                icon = if (isCurrent) Icons.TwoTone.PhonelinkSetup else Icons.TwoTone.Smartphone,
                                                title = device.deviceName,
                                                description = if (isCurrent) stringResource(R.string.current_device) else null,
                                                onClick = if (!isCurrent) { { deviceToUnbind = device } } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is NetworkResult.Failure -> {
                            SegmentedColumn(title = stringResource(R.string.device_management)) {
                                item {
                                    SettingsBaseWidget(
                                        title = stringResource(R.string.query_devices_failed_with_code, result.code, result.msg)
                                    ) {}
                                }
                            }
                        }

                        is NetworkResult.Error -> {
                            SegmentedColumn(title = stringResource(R.string.device_management)) {
                                item {
                                    SettingsBaseWidget(
                                        title = stringResource(R.string.query_devices_failed, result.msg)
                                    ) {}
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (!authSession.isLoggedIn) {
                Button(
                    onClick = { navigator.push(Screen.Login) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.login))
                }
                Button(
                    onClick = { navigator.push(Screen.Register) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(stringResource(R.string.create_account))
                }
            }

            if (showDevices && authSession.isLoggedIn) {
                Text(
                    text = stringResource(R.string.local_device_id, authSession.uuid),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
