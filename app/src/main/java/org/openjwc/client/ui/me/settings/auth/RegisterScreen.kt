package org.openjwc.client.ui.me.settings.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.LockReset
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource
import org.openjwc.client.viewmodels.AuthViewModel
import org.openjwc.client.viewmodels.AuthViewModelFactory
import org.openjwc.client.viewmodels.NavEvent

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RegisterScreen(navigator: Navigator) {
    val context = LocalContext.current
    val authDataSource = remember { AuthDataSource(context) }
    val settingsDataSource = remember { SettingsDataSource(context) }
    val authRepository = remember { AuthRepository(authDataSource, settingsDataSource) }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))

    val isRegistering by authViewModel.isRegistering.collectAsState()
    val registerResult by authViewModel.registerResult.collectAsState()

    LaunchedEffect(Unit) {
        for (event in authViewModel.navEvent) {
            when (event) {
                is NavEvent.ToBack -> navigator.pop()
                else -> {}
            }
        }
    }

    val registerError = when (val r = registerResult) {
        is NetworkResult.Failure -> "(${r.code}) ${r.msg}"
        is NetworkResult.Error -> r.msg
        else -> null
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val usernameState = remember { TextFieldState() }
    val emailState = remember { TextFieldState() }
    val passwordState = remember { TextFieldState() }
    val confirmPasswordState = remember { TextFieldState() }

    val usernameError = if (usernameState.text.isBlank()) "Required" else ""
    val emailError = when {
        emailState.text.isBlank() -> "Required"
        !emailState.text.contains("@") -> "Invalid email"
        else -> ""
    }
    val passwordError = when {
        passwordState.text.isBlank() -> "Required"
        passwordState.text.length < 6 -> "At least 6 characters"
        else -> ""
    }
    val confirmPasswordError = when {
        confirmPasswordState.text.isBlank() -> "Required"
        confirmPasswordState.text != passwordState.text -> "Passwords do not match"
        else -> ""
    }

    val canRegister = usernameError.isEmpty() && emailError.isEmpty() &&
            passwordError.isEmpty() && confirmPasswordError.isEmpty()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(stringResource(R.string.create_account)) },
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
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .blurSource()
        ) {
            SegmentedColumn {
                item {
                    SettingsTextFieldWidget(
                        state = usernameState,
                        title = stringResource(R.string.username),
                        error = usernameError
                    )
                }
                item {
                    SettingsTextFieldWidget(
                        state = emailState,
                        title = stringResource(R.string.email),
                        error = emailError
                    )
                }
                item {
                    SettingsTextFieldWidget(
                        state = passwordState,
                        title = stringResource(R.string.password),
                        error = passwordError
                    )
                }
                item {
                    SettingsTextFieldWidget(
                        state = confirmPasswordState,
                        title = stringResource(R.string.confirm_password),
                        error = confirmPasswordError
                    )
                }
            }

            AnimatedVisibility(visible = registerError != null) {
                Text(
                    text = registerError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                )
            }

            Button(
                onClick = {
                    authViewModel.register(
                        usernameState.text.toString().trim(),
                        passwordState.text.toString().trim(),
                        emailState.text.toString().trim()
                    )
                },
                enabled = canRegister && !isRegistering,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                if (isRegistering) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.creating_account))
                } else {
                    Text(stringResource(R.string.create_account))
                }
            }
        }
    }
}
