package org.openjwc.client.ui.me.settings.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect
import org.openjwc.client.R
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.viewmodels.AuthViewModel
import org.openjwc.client.viewmodels.AuthViewModelFactory
import org.openjwc.client.viewmodels.NavEvent

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(navigator: Navigator, authViewModel: AuthViewModel) {

    val isLoggingIn by authViewModel.isLoggingIn.collectAsState()
    val loginResult by authViewModel.loginResult.collectAsState()

    LaunchedEffect(Unit) {
        for (event in authViewModel.navEvent) {
            when (event) {
                is NavEvent.ToBack -> navigator.pop()
                else -> {}
            }
        }
    }

    val loginError = when (val r = loginResult) {
        is NetworkResult.Failure -> "(${r.code}) ${r.msg}"
        is NetworkResult.Error -> r.msg
        else -> null
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val usernameState = remember { TextFieldState() }
    val passwordState = remember { TextFieldState() }
    var passwordVisible by remember { mutableStateOf(false) }

    val canLogin = usernameState.text.isNotBlank() && passwordState.text.isNotBlank()
    val usernameError = if (usernameState.text.isBlank()) "Required" else ""
    val passwordError = if (passwordState.text.isBlank()) "Required" else ""
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.login)) },
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
            SegmentedColumn {
                item {
                    SettingsTextFieldWidget(
                        state = usernameState,
                        title = stringResource(R.string.account),
                        error = usernameError
                    )
                }
                item {
                    val pwdTransform = if (passwordVisible) null
                    else OutputTransformation { replace(0, length, "•".repeat(length)) }
                    SettingsTextFieldWidget(
                        state = passwordState,
                        title = stringResource(R.string.password),
                        error = passwordError,
                        outputTransformation = pwdTransform,
                        trailingContent = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                enabled = !isLoggingIn
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.TwoTone.VisibilityOff else Icons.TwoTone.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }

            if (loginError != null) {
                Text(
                    text = loginError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 32.dp, top = 4.dp)
                )
            }

            Button(
                onClick = {
                    authViewModel.login(
                        usernameState.text.toString().trim(),
                        passwordState.text.toString().trim()
                    )
                },
                enabled = canLogin && !isLoggingIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            ) {
                if (isLoggingIn) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.logging_in))
                } else {
                    Text(stringResource(R.string.login))
                }
            }

            TextButton(
                onClick = { navigator.push(Screen.Register) },
                enabled = !isLoggingIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(stringResource(R.string.no_account_register_now))
            }
        }
    }
}
