package org.openjwc.client.ui.timetable.edit.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.ui.theme.ColorItem
import org.openjwc.client.ui.theme.seedColors

@Preview
@Composable
fun ColorPickerRowPreview() {
    ColorPickerRow(
        selectedIndex = 0,
        onColorSelect = {},
        presetColors = seedColors
    )
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    selectedIndex: Int,
    presetColors: List<Color>,
    onColorSelect: (Int) -> Unit, // Changed to Int (index)
) {
    FlowRow(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        presetColors.forEachIndexed { index, color ->
            ColorItem(
                color = color,
                isSelected = selectedIndex == index,
                onClick = {
                    onColorSelect(index)
                }
            )
        }
    }
}
