package com.aliothmoon.maameow.presentation.view.panel.fight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 关卡选择按钮组
 * 使用 FlowRow 自动换行平铺显示关卡选项
 */
@Composable
fun StageButtonGroup(
    modifier: Modifier = Modifier,
    label: String,
    selectedValue: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    displayMapper: (String) -> String = { it },
    isOpenCheck: (String) -> Boolean = { true },
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                val isSelected = item == selectedValue
                val isOpen = isOpenCheck(item)
                val displayText = displayMapper(item)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onItemSelected(item) },
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        !isOpen -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            !isOpen -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
