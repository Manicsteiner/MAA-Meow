package com.aliothmoon.maameow.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aliothmoon.maameow.presentation.LocalFloatingWindowContext

enum class TaskPromptButtonLayout {
    HORIZONTAL,
    VERTICAL,
}

@Composable
fun AdaptiveTaskPromptDialog(
    visible: Boolean,
    title: String,
    message: AnnotatedString,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "确认",
    dismissText: String? = "关闭",
    icon: ImageVector? = null,
    confirmColor: Color? = null,
    iconTint: Color? = null,
    buttonLayout: TaskPromptButtonLayout = TaskPromptButtonLayout.HORIZONTAL,
    maxWidth: Dp = 360.dp,
    dismissOnOutsideClick: Boolean = true,
) {
    if (!visible) return

    val resolvedConfirmColor = confirmColor ?: MaterialTheme.colorScheme.primary
    val resolvedIconTint = iconTint ?: resolvedConfirmColor

    if (LocalFloatingWindowContext.current) {
        FloatingTaskPromptDialog(
            title = title,
            message = message,
            onDismissRequest = onDismissRequest,
            onConfirm = onConfirm,
            confirmText = confirmText,
            dismissText = dismissText,
            icon = icon,
            iconTint = resolvedIconTint,
            confirmColor = resolvedConfirmColor,
            buttonLayout = buttonLayout,
            maxWidth = maxWidth,
            dismissOnOutsideClick = dismissOnOutsideClick,
        )
    } else {
        MaterialTaskPromptDialog(
            title = title,
            message = message,
            onDismissRequest = onDismissRequest,
            onConfirm = onConfirm,
            confirmText = confirmText,
            dismissText = dismissText,
            icon = icon,
            iconTint = resolvedIconTint,
            confirmColor = resolvedConfirmColor,
            buttonLayout = buttonLayout,
            maxWidth = maxWidth,
            dismissOnOutsideClick = dismissOnOutsideClick,
        )
    }
}

@Composable
private fun FloatingTaskPromptDialog(
    title: String,
    message: AnnotatedString,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    dismissText: String?,
    icon: ImageVector?,
    iconTint: Color,
    confirmColor: Color,
    buttonLayout: TaskPromptButtonLayout,
    maxWidth: Dp,
    dismissOnOutsideClick: Boolean,
) {
    val overlayInteractionSource = remember { MutableInteractionSource() }
    val cardInteractionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = overlayInteractionSource,
                    onClick = {
                        if (dismissOnOutsideClick) {
                            onDismissRequest()
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)),
                exit = scaleOut(targetScale = 0.85f, animationSpec = tween(150)),
            ) {
                TaskPromptCard(
                    title = title,
                    message = message,
                    onDismissRequest = onDismissRequest,
                    onConfirm = onConfirm,
                    confirmText = confirmText,
                    dismissText = dismissText,
                    icon = icon,
                    iconTint = iconTint,
                    confirmColor = confirmColor,
                    buttonLayout = buttonLayout,
                    maxWidth = maxWidth,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clickable(
                            indication = null,
                            interactionSource = cardInteractionSource,
                            onClick = {},
                        ),
                )
            }
        }
    }
}

@Composable
private fun MaterialTaskPromptDialog(
    title: String,
    message: AnnotatedString,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    dismissText: String?,
    icon: ImageVector?,
    iconTint: Color,
    confirmColor: Color,
    buttonLayout: TaskPromptButtonLayout,
    maxWidth: Dp,
    dismissOnOutsideClick: Boolean,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnOutsideClick,
            dismissOnClickOutside = dismissOnOutsideClick,
            usePlatformDefaultWidth = false,
        ),
    ) {
        TaskPromptCard(
            title = title,
            message = message,
            onDismissRequest = onDismissRequest,
            onConfirm = onConfirm,
            confirmText = confirmText,
            dismissText = dismissText,
            icon = icon,
            iconTint = iconTint,
            confirmColor = confirmColor,
            buttonLayout = buttonLayout,
            maxWidth = maxWidth,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun TaskPromptCard(
    title: String,
    message: AnnotatedString,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    dismissText: String?,
    icon: ImageVector?,
    iconTint: Color,
    confirmColor: Color,
    buttonLayout: TaskPromptButtonLayout,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = maxWidth)
            .wrapContentHeight()
            .shadow(8.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .background(
                            color = iconTint.copy(alpha = 0.1f),
                            shape = CircleShape,
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = iconTint,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
            )

            Spacer(modifier = Modifier.height(20.dp))

            TaskPromptButtons(
                onDismissRequest = onDismissRequest,
                onConfirm = onConfirm,
                confirmText = confirmText,
                dismissText = dismissText,
                confirmColor = confirmColor,
                buttonLayout = buttonLayout,
            )
        }
    }
}

@Composable
private fun TaskPromptButtons(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    dismissText: String?,
    confirmColor: Color,
    buttonLayout: TaskPromptButtonLayout,
) {
    if (buttonLayout == TaskPromptButtonLayout.VERTICAL) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(confirmText)
            }
            dismissText?.let {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(it)
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        dismissText?.let {
            OutlinedButton(
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(it)
            }
        }
        Button(
            onClick = onConfirm,
            modifier = if (dismissText != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = confirmColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(confirmText)
        }
    }
}
