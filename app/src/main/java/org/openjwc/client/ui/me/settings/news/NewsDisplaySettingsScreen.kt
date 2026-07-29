package org.openjwc.client.ui.me.settings.news

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.R
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.CachedDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.SettingsViewModelFactory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewsDisplaySettingsScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val settingsDataSource = remember { SettingsDataSource(context) }
    val authDataSource = remember { AuthDataSource(context) }
    val cachedDataSource = remember { CachedDataSource(context) }
    val settingsRepository = remember { SettingsRepository(settingsDataSource, cachedDataSource, authDataSource, context) }
    val authRepository = remember { AuthRepository(authDataSource, settingsDataSource) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsRepository, authRepository))

    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val savedFreshDays = settings.freshDays
    val freshDaysState = remember { TextFieldState(savedFreshDays.toString()) }
    val scrollState = rememberScrollState()

    val freshDaysError = run {
        val d = freshDaysState.text.toString().toIntOrNull()
        when {
            freshDaysState.text.isBlank() -> "Required"
            d == null || d <= 0 -> "Must be a positive integer"
            else -> ""
        }
    }
    val isValid = freshDaysError.isEmpty()

    fun save() {
        val days = freshDaysState.text.toString().toIntOrNull() ?: savedFreshDays
        settingsViewModel.updateFreshDays(days)
        navigator.pop()
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.news_display_settings)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(innerPadding)
        ) {
            SegmentedColumn(title = stringResource(R.string.highlight_fresh_news)) {
                item {
                    SettingsTextFieldWidget(
                        state = freshDaysState,
                        title = stringResource(R.string.fresh_threshold_days),
                        error = freshDaysError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
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
