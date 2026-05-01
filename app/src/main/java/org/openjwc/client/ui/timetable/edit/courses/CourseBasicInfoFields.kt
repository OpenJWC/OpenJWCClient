package org.openjwc.client.ui.timetable.edit.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R

@Preview(showBackground = true)
@Composable
fun CourseBasicInfoFieldsPreview() {
    CourseBasicInfoFields(
        name = "测试课程",
        onNameChange = {},
        teacher = "教师",
        onTeacherChange = {},
        location = "地点",
        onLocationChange = {}
    )
}

@Composable
fun CourseBasicInfoFields(
    name: String,
    onNameChange: (String) -> Unit,
    teacher: String,
    onTeacherChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.course_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = teacher,
                onValueChange = onTeacherChange,
                label = { Text(stringResource(R.string.teacher)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                label = { Text(stringResource(R.string.location)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}
