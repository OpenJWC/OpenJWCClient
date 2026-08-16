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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.flow.collect
import org.openjwc.client.R
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.repository.AuthRepository
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
fun RegisterScreen(navigator: Navigator, authViewModel: AuthViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.create_account)) },
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
        RegisterContent(
            navigator = navigator,
            authViewModel = authViewModel,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@Composable
fun RegisterContent(
    navigator: Navigator,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
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

    val usernameState = remember { TextFieldState() }
    val emailState = remember { TextFieldState() }
    val passwordState = remember { TextFieldState() }
    val confirmPasswordState = remember { TextFieldState() }
    var passwordVisible by remember { mutableStateOf(false) }

    val usernameTrimmed = usernameState.text.toString().trim()
    val emailTrimmed = emailState.text.toString().trim()
    val passwordText = passwordState.text.toString()
    val confirmPasswordText = confirmPasswordState.text.toString()

    val usernameError = if (usernameTrimmed.isEmpty()) stringResource(R.string.required)
    else if (usernameTrimmed.length < 3) stringResource(R.string.username_3_chars_required)
    else ""
    val emailError = when {
        emailTrimmed.isEmpty() -> stringResource(R.string.required)
        !emailTrimmed.contains("@") -> stringResource(R.string.please_type_valid_email)
        else -> ""
    }
    val passwordError = when {
        passwordText.isEmpty() -> stringResource(R.string.required)
        passwordText.length < 6 -> stringResource(R.string.password_requirement)
        else -> ""
    }
    val confirmPasswordError = when {
        confirmPasswordText.isEmpty() -> stringResource(R.string.required)
        confirmPasswordText != passwordText -> stringResource(R.string.password_not_same)
        else -> ""
    }

    val canRegister = usernameError.isEmpty() && emailError.isEmpty() &&
            passwordError.isEmpty() && confirmPasswordError.isEmpty()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SegmentedColumn {
            item {
                SettingsTextFieldWidget(
                    state = usernameState,
                    title = stringResource(R.string.username),
                    error = usernameError,
                    lineLimits = TextFieldLineLimits.SingleLine
                )
            }
            item {
                SettingsTextFieldWidget(
                    state = emailState,
                    title = stringResource(R.string.email),
                    error = emailError,
                    lineLimits = TextFieldLineLimits.SingleLine
                )
            }
            item {
                val pwdTransform = if (passwordVisible) null
                else OutputTransformation { replace(0, length, "•".repeat(length)) }
                SettingsTextFieldWidget(
                    state = passwordState,
                    title = stringResource(R.string.password),
                    error = passwordError,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    outputTransformation = pwdTransform,
                    trailingContent = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.TwoTone.VisibilityOff else Icons.TwoTone.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
            item {
                val confirmTransform = if (passwordVisible) null
                else OutputTransformation { replace(0, length, "•".repeat(length)) }
                SettingsTextFieldWidget(
                    state = confirmPasswordState,
                    title = stringResource(R.string.confirm_password),
                    error = confirmPasswordError,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    outputTransformation = confirmTransform
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
                    usernameTrimmed,
                    passwordText,
                    emailTrimmed
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

