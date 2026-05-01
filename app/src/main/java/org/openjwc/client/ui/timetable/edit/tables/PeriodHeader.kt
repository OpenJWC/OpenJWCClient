package org.openjwc.client.ui.timetable.edit.tables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.openjwc.client.R

@Composable
fun PeriodHeader(onAdd: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(R.string.schedule), style = MaterialTheme.typography.titleSmall)
        Row {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, stringResource(R.string.add), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Preview
@Composable
fun PeriodHeaderPreview() {
    PeriodHeader(onAdd = {}, onDelete = {})
}