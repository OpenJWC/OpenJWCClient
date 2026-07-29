package org.openjwc.client.ui.me

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Newspaper
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Upload
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.openjwc.client.R
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeScreenContent(navigator: Navigator) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SegmentedColumn {
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Settings,
                    title = stringResource(R.string.settings),
                    onClick = { navigator.push(Screen.Settings) }
                )
            }
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Newspaper,
                    title = stringResource(R.string.favorite_news),
                    onClick = { navigator.push(Screen.Favorite) }
                )
            }
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.History,
                    title = stringResource(R.string.reviewed_notices),
                    onClick = { navigator.push(Screen.Review) }
                )
            }
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Upload,
                    title = stringResource(R.string.upload_news),
                    onClick = { navigator.push(Screen.UploadNews) }
                )
            }
        }

        SegmentedColumn {
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Info,
                    title = stringResource(R.string.about),
                    onClick = { navigator.push(Screen.About) }
                )
            }
        }
    }
}
