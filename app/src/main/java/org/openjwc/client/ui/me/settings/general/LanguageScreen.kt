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
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsChooseWidget
import org.openjwc.client.utils.changeAppLanguage
import org.openjwc.client.utils.languages
import org.openjwc.client.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanguageScreen(navigator: Navigator, settingsViewModel: SettingsViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.language_settings)) },
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
        LanguageContent(
            navigator = navigator,
            settingsViewModel = settingsViewModel,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@Composable
fun LanguageContent(
    navigator: Navigator,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val currentLanguageCode = settings.languageCode

    val entries = languages.entries.toList()
    val languageNames = entries.map { it.value.translatedName.asString() }
    val currentIndex = entries.indexOfFirst { it.key == currentLanguageCode }.coerceAtLeast(0)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
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

