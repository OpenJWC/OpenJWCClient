package org.openjwc.client.ui.me.settings.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
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
import org.openjwc.client.ui.component.settings.SettingsChooseWidget
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource
import org.openjwc.client.utils.changeAppLanguage
import org.openjwc.client.utils.languages
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.SettingsViewModelFactory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanguageScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val settingsDataSource = remember { SettingsDataSource(context) }
    val authDataSource = remember { AuthDataSource(context) }
    val cachedDataSource = remember { CachedDataSource(context) }
    val settingsRepository = remember { SettingsRepository(settingsDataSource, cachedDataSource, authDataSource, context) }
    val authRepository = remember { AuthRepository(authDataSource, settingsDataSource) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsRepository, authRepository))

    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val currentLanguageCode = settings.languageCode

    val entries = languages.entries.toList()
    val languageNames = entries.map { it.value.translatedName.asString() }
    val currentIndex = entries.indexOfFirst { it.key == currentLanguageCode }.coerceAtLeast(0)

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(stringResource(R.string.language_settings)) },
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .blurSource()
        ) {
            SegmentedColumn {
                item {
                    SettingsChooseWidget(
                        icon = Icons.TwoTone.Language,
                        title = stringResource(R.string.language_settings),
                        items = languageNames,
                        selectedIndex = currentIndex,
                        onSelectedIndexChange = { index ->
                            val code = entries.getOrNull(index)?.key
                            settingsViewModel.updateLanguage(code)
                            changeAppLanguage(code)
                        }
                    )
                }
            }
        }
    }
}
