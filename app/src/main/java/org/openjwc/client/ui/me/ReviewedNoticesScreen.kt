package org.openjwc.client.ui.me

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Article
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.HourglassEmpty
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.appPreferences
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReviewedNoticesScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val reviewCount = context.appPreferences.getInt("review_count", 0)
    val reviews = remember {
        (0 until reviewCount).map { i ->
            val id = context.appPreferences.getString("review_id_$i", "$i") ?: "$i"
            val title = context.appPreferences.getString("review_title_$i", "Notice $i") ?: "Notice $i"
            val status = context.appPreferences.getString("review_status_$i", "pending") ?: "pending"
            Triple(id, title, status)
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(stringResource(R.string.upload_results)) },
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
        if (reviews.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                    .blurSource()
                    .padding(32.dp),
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Article,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_upload_records),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                    .blurSource()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(reviews) { (id, title, status) ->
                    val (icon, color) = when (status) {
                        "approved" -> Icons.TwoTone.CheckCircle to MaterialTheme.colorScheme.primary
                        "rejected" -> Icons.TwoTone.Error to MaterialTheme.colorScheme.error
                        else -> Icons.TwoTone.HourglassEmpty to MaterialTheme.colorScheme.secondary
                    }
                    SettingsBaseWidget(
                        icon = icon,
                        iconColor = color,
                        title = title,
                        modifier = Modifier.fillMaxWidth(),
                        foreContent = {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                    ) {}
                }
            }
        }
    }
}
