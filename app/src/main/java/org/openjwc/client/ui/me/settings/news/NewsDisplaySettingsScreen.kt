package org.openjwc.client.ui.me.settings.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.ui.news.InfoCard
import java.time.LocalDate

@Preview
@Composable
fun TestNewsDisplaySettingsScreen() {
    NewsDisplaySettingsScreen(
        initialFreshDays = 3,
        onSave = {},
        onBack = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDisplaySettingsScreen(
    initialFreshDays: Int,
    onSave: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var freshDaysString by remember { mutableStateOf(initialFreshDays.toString()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val freshDaysInt = freshDaysString.toIntOrNull()
    val isValid = freshDaysInt != null && freshDaysInt >= 0
    val isChanged = freshDaysInt != initialFreshDays
    val canSave = isValid && isChanged

    LaunchedEffect(initialFreshDays) {
        freshDaysString = initialFreshDays.toString()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.news_display_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (canSave) onSave(freshDaysInt) },
                        enabled = canSave
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.highlight_fresh_news),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = stringResource(R.string.highlight_fresh_news_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = freshDaysString,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                freshDaysString = it
                            }
                        },
                        label = { Text(stringResource(R.string.fresh_threshold_days)) },
                        placeholder = { Text(stringResource(R.string.fresh_threshold_example)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isValid && freshDaysString.isNotEmpty(),
                        supportingText = {
                            if (!isValid && freshDaysString.isNotEmpty()) {
                                Text(stringResource(R.string.invalid_positive_integer))
                            } else {
                                Text(stringResource(R.string.disable_highlight_hint))
                            }
                        },
                        trailingIcon = {
                            if (freshDaysString.isNotEmpty()) {
                                IconButton(onClick = { freshDaysString = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        }
                    )
                }
            }

            // 预览效果区
            Text(
                text = stringResource(R.string.preview_effect),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            InfoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                fetchedNotice = FetchedNotice(
                    id = "preview",
                    title = stringResource(R.string.preview_fresh_title),
                    date = LocalDate.now().toString(),
                    label = stringResource(R.string.notice),
                    detailUrl = "",
                    isPage = true,
                    contentText = "",
                    attachmentUrls = emptyList()
                ),
                onClick = {},
                isFavorited = false,
                isFresh = isValid && freshDaysInt != 0
            )
            InfoCard(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                fetchedNotice = FetchedNotice(
                    id = "preview",
                    title = stringResource(R.string.preview_old_title),
                    date = LocalDate.now().toString(),
                    label = stringResource(R.string.notice),
                    detailUrl = "",
                    isPage = true,
                    contentText = "",
                    attachmentUrls = emptyList()
                ),
                onClick = {},
                isFavorited = false,
                isFresh = false
            )
        }
    }
}