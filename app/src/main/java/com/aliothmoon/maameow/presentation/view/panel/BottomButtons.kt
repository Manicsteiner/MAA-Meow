package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 底部按钮
 */
@Composable
fun BottomButtons(
    onClose: () -> Unit,
    onStart: () -> Unit,
    isStarting: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.weight(1f).height(36.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            enabled = !isStarting,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Gray,
                disabledContentColor = Color.Gray.copy(alpha = 0.5f)
            )
        ) {
            Text("隐藏")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = onStart,
            modifier = Modifier.weight(1f).height(36.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            enabled = !isStarting,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3),
                disabledContainerColor = Color(0xFF2196F3).copy(alpha = 0.5f)
            )
        ) {
            if (isStarting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("启动中")
                }
            } else {
                Text("启动")
            }
        }
    }
}
