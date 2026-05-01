package org.openjwc.client.ui.timetable.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R

@Composable
fun EmptyGuidePlaceholder(onImport: () -> Unit, onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_timetables_yet),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onImport) { Text(stringResource(R.string.import_timetable)) }
        TextButton(onClick = onCreate) { Text(stringResource(R.string.create_a_blank_timetable)) }
    }
}
@Preview
@Composable
fun EmptyGuidePlaceholderPreview() {
    EmptyGuidePlaceholder(onImport = {}, onCreate = {})
}