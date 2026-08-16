package org.openjwc.client.ui.timetable.edit.courses

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.res.stringResource
import org.openjwc.client.R
import org.openjwc.client.ui.component.settings.SegmentedColumnScope
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget

/**
 * 课程基础信息（名称/教师/地点）。
 * 每个字段独立占一个 SegmentedColumn item，形成连贯分组。
 */
fun SegmentedColumnScope.CourseBasicInfoFields(
    nameState: TextFieldState,
    teacherState: TextFieldState,
    locationState: TextFieldState
) {
    item {
        SettingsTextFieldWidget(
            state = nameState,
            title = stringResource(R.string.course_name)
        )
    }
    item {
        SettingsTextFieldWidget(
            state = teacherState,
            title = stringResource(R.string.teacher)
        )
    }
    item {
        SettingsTextFieldWidget(
            state = locationState,
            title = stringResource(R.string.location)
        )
    }
}
