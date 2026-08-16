package org.openjwc.client.ui.timetable.edit.tables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.openjwc.client.R
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget

@Composable
fun ConfigSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSwitchWidget(
        icon = Icons.Default.CalendarViewWeek,
        title = label,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Preview
@Composable
fun ConfigSwitchRowPreview() {
    ConfigSwitchRow(label = stringResource(R.string.show_weekends), checked = true, onCheckedChange = {})
}
