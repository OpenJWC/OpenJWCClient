package org.openjwc.client.ui.me.settings.log

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.log.Logger
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val logs = Logger.logHistory

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(stringResource(R.string.log)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .blurSource()
        ) {
            items(items = logs, key = { it.id }) { log ->
                LogItemWidget(log)
            }
        }
    }
}

@Composable
private fun LogItemWidget(log: Logger.LogEntry) {
    val levelColor = when (log.level) {
        Logger.Level.ERROR -> MaterialTheme.colorScheme.error
        Logger.Level.WARNING -> MaterialTheme.colorScheme.tertiary
        Logger.Level.INFO -> MaterialTheme.colorScheme.primary
        Logger.Level.DEBUG -> MaterialTheme.colorScheme.outline
        Logger.Level.VERBOSE -> MaterialTheme.colorScheme.primaryFixedDim
        else -> MaterialTheme.colorScheme.primaryFixedDim
    }

    val timeString = remember(log.timestamp) {
        java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(log.timestamp)
    }

    SettingsBaseWidget(
        icon = Icons.TwoTone.BugReport,
        iconColor = levelColor,
        title = "${log.tag} | ${log.level.name}",
        description = log.message ?: "",
        modifier = Modifier.fillMaxWidth(),
        foreContent = {
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace
            )
        }
    ) {}
}
