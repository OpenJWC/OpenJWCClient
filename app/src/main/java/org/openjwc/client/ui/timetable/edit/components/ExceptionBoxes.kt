package org.openjwc.client.ui.timetable.edit.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.openjwc.client.ui.component.WarningCard


@Composable
fun WarningBox(
    text: String,
    modifier: Modifier = Modifier
) {
    WarningCard(
        message = text,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier
    )
}

@Composable
fun ErrorBox(
    text: String,
    modifier: Modifier = Modifier
) {
    WarningCard(
        message = text,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier
    )
}

@Preview
@Composable
fun WarningBoxPreview() {
    WarningBox(text = "这是一个警告框")
}

@Preview
@Composable
fun ErrorBoxPreview() {
    ErrorBox(text = "这是一个错误框")
}
