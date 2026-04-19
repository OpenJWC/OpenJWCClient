package org.openjwc.client.ui.me.settings.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

fun String.filterAscii(): String = this.filter { it.code in 0..127 }

@Preview
@Composable
fun TestRegisterScreen() {
    RegisterScreen(
        registerError = null,
        onRegister = { _, _, _ -> },
        onBack = {},
        isRegistering = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    isRegistering: Boolean,
    registerError: String?,
    onRegister: (String, String, String) -> Unit,
    onBack: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // 校验逻辑
    val usernameError by remember { derivedStateOf { username.isNotEmpty() && username.length < 3 } }
    val emailError by remember {
        derivedStateOf {
            email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
    }
    val passwordError by remember {
        derivedStateOf {
            val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$".toRegex()
            password.isNotEmpty() && !password.matches(passwordPattern)
        }
    }
    val confirmPasswordError by remember { derivedStateOf { confirmPassword.isNotEmpty() && confirmPassword != password } }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("创建账号") },
                navigationIcon = {
                    // 注册时禁用返回
                    IconButton(onClick = onBack, enabled = !isRegistering) {
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 用户名
            OutlinedTextField(
                value = username,
                onValueChange = { input ->
                    val filtered = input.filterAscii()
                    if (filtered.length <= 20) username = filtered
                },
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isRegistering, // 禁用
                isError = usernameError,
                supportingText = { if (usernameError) Text("用户名至少需要3个字符") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            // 邮箱
            OutlinedTextField(
                value = email,
                onValueChange = { input ->
                    val filtered = input.filterAscii()
                    if (filtered.length <= 50) email = filtered
                },
                label = { Text("电子邮箱") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRegistering, // 禁用
                singleLine = true,
                isError = emailError,
                supportingText = { if (emailError) Text("请输入有效的邮箱地址") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
            )

            // 密码
            OutlinedTextField(
                value = password,
                onValueChange = { input ->
                    val filtered = input.filterAscii()
                    if (filtered.length <= 100) password = filtered
                },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRegistering,
                isError = passwordError,
                singleLine = true,
                supportingText = { if (passwordError) Text("密码需包含大小写字母、数字及特殊字符，且至少 8 位") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isRegistering) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // 确认密码
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { input ->
                    val filtered = input.filterAscii()
                    if (filtered.length <= 100) confirmPassword = filtered
                },
                label = { Text("确认密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isRegistering, // 禁用
                isError = confirmPasswordError,
                supportingText = { if (confirmPasswordError) Text("两次输入的密码不一致") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (registerError != null) {
                Text(
                    text = registerError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onRegister(username.trim(), password.trim(), email.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRegistering && !usernameError && !emailError &&
                        !passwordError && !confirmPasswordError &&
                        username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
            ) {
                if (isRegistering) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在创建账号...")
                } else {
                    Text("立即注册")
                }
            }
        }
    }
}