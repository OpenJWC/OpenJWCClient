package org.openjwc.client.ui.me

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openjwc.client.BuildConfig
import org.openjwc.client.R
import org.openjwc.client.data.models.Event
import org.openjwc.client.data.models.MenuItem
import org.openjwc.client.data.models.SettingSection
import org.openjwc.client.navigation.Screen
import org.openjwc.client.net.models.GitHubRelease
import org.openjwc.client.ui.main.UpdateDialog
import org.openjwc.client.ui.me.settings.MenuSectionCard


@Preview
@Composable
fun TestAboutScreen() {
    AboutScreen(
        onBack = {},
        onToGitHub = {},
        onRoute = {},
        onCheckForUpdate = {},
        updateRelease = null,
        onUpdate = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    updateRelease: GitHubRelease?,
    onBack: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onUpdate: () -> Unit,
    onToGitHub: () -> Unit,
    onRoute: (Screen) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showUpdateDialog by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_icon_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.introduction),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.openjwc_description),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            MenuSectionCard(
                section = SettingSection(
                    title = null, items = listOf(
                        MenuItem.Action(
                            icon = Icons.Default.Sync,
                            label = stringResource(R.string.check_for_updates),
                            onClick = {
                                onCheckForUpdate()
                                showUpdateDialog = true
                            },
                        ),
                        MenuItem.Action(
                            icon = Icons.AutoMirrored.Default.OpenInNew,
                            label = stringResource(R.string.github),
                            subtitle = "https://github.com/OpenJWC",
                            onClick = onToGitHub
                        ),
                    )
                )
            ) {
                when (it) {
                    is Event.Action -> it.onAction()
                    is Event.Route -> onRoute(it.route)
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            MenuSectionCard(
                section = SettingSection(
                    title = null, items = listOf(
                        MenuItem.Route(
                            icon = Icons.Outlined.Policy,
                            title = stringResource(R.string.user_agreement_and_privacy_policy),
                            route = Screen.Policy
                        ),
                        MenuItem.Route(
                            icon = Icons.Outlined.Gavel,
                            title = stringResource(R.string.open_source_licenses),
                            route = Screen.License
                        )
                    )
                )
            ) {
                when (it) {
                    is Event.Action -> it.onAction()
                    is Event.Route -> onRoute(it.route)
                    else -> {}
                }
            }
            if (updateRelease != null && showUpdateDialog) {
                UpdateDialog(
                    gitHubRelease = updateRelease,
                    onDismiss = { showUpdateDialog = false },
                    onUpdate = {
                        showUpdateDialog = false
                        onUpdate()
                    }
                )
            }
        }
    }
}
