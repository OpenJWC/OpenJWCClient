package org.openjwc.client.ui.me.settings.timetable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.theme.courseBackgroundColors
import org.openjwc.client.viewmodels.TimetableDisplayPrefs
import java.time.DayOfWeek

val mockCourses = listOf(
    Course(
        id = 0L,
        tableId = 0L,
        name = "测试课程",
        teacher = "老师",
        location = "地点",
        dayOfWeek = DayOfWeek.MONDAY,
        startPeriod = 1,
        duration = 2,
        color = courseBackgroundColors[0],
        weekRule = (1..16).toSet(),
        note = ""
    )
)

val mockTableMetadata = TableMetadata(
    id = 0,
    tableName = "测试课表",
    semesterConfig = SemesterConfig.default(),
    isCurrent = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetablePrefsScreen(
    prefs: TimetableDisplayPrefs,
    onBack: () -> Unit,
    onToggleTimeline: (Boolean) -> Unit,
    onToggleDate: (Boolean) -> Unit,
    onTogglePeriodTime: (Boolean) -> Unit,
    onToggleNonCurrentWeek: (Boolean) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.timetable_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.show_timeline)) },
                        supportingContent = { Text(stringResource(R.string.show_timeline_desc)) },
                        trailingContent = {
                            Switch(checked = prefs.showTimeline, onCheckedChange = onToggleTimeline)
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.show_date_header)) },
                        supportingContent = { Text(stringResource(R.string.show_date_header_desc)) },
                        trailingContent = {
                            Switch(checked = prefs.showDate, onCheckedChange = onToggleDate)
                        }
                    )
                    // 显示课节具体时间
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.show_period_time)) },
                        supportingContent = { Text(stringResource(R.string.show_period_time_desc)) },
                        trailingContent = {
                            Switch(checked = prefs.showPeriodTime, onCheckedChange = onTogglePeriodTime)
                        }
                    )
                    // 显示非本周课程
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.show_non_current_week)) },
                        supportingContent = { Text(stringResource(R.string.show_non_current_week_desc)) },
                        trailingContent = {
                            Switch(checked = prefs.showNonCurrentWeek, onCheckedChange = onToggleNonCurrentWeek)
                        }
                    )
                }
            }
        }
        /*Text(
            text = stringResource(R.string.preview_effect),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )
        TimetableGrid(
            tableMetadata = mockTableMetadata,
            courses = mockCourses,
            currentWeek = 2,
            showNonCurrentWeek = prefs.showNonCurrentWeek,
            showTimeLine = prefs.showTimeline,
            showDate = prefs.showDate,
            showPeriodTime = prefs.showPeriodTime,
            onCourseClick = {},
            onEmptySlotClick = { _, _ -> }
        )*/
    }
}
