package com.aliothmoon.maameow.presentation.components

import android.text.InputType
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import com.aliothmoon.maameow.presentation.LocalFloatingWindowContext

/**
 * 数字输入框（失焦时验证）
 *
 * 根据运行环境自动选择合适的实现：
 * - 悬浮窗环境：使用 FloatWindowEditText（原生 EditText 包装）
 * - 普通环境：使用 BasicTextField + OutlinedTextField 装饰
 *
 * @param value 当前值
 * @param onValueChange 值变化回调（仅在失焦且验证通过后调用）
 * @param modifier 修饰符
 * @param hint 提示文本
 * @param minimum 最小值
 * @param maximum 最大值
 * @param increment 步进值
 * @param valueFormat 格式化字符串
 * @param enabled 是否启用
 */
@Composable
fun INumericField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    minimum: Int = 0,
    maximum: Int = Int.MAX_VALUE,
    increment: Int = 1,
    valueFormat: String = "%d",
    enabled: Boolean = true
) {
    val isInFloatingWindow = LocalFloatingWindowContext.current
    var inputText by remember(value) { mutableStateOf(valueFormat.format(value)) }

    // 失焦时的验证逻辑（两个环境共用）
    val validateOnFocusLost = {
        val text = inputText
        if (text.isEmpty() || text == "-") {
            // 空值或仅负号时使用最小值
            val newValue = minimum
            inputText = valueFormat.format(newValue)
            if (newValue != value) {
                onValueChange(newValue)
            }
        } else {
            val intValue = text.toIntOrNull()
            if (intValue != null) {
                // 范围限制
                val clampedValue = intValue.coerceIn(minimum, maximum)
                // increment 对齐
                val alignedValue = if (increment > 1) {
                    (clampedValue / increment) * increment
                } else {
                    clampedValue
                }
                // 更新显示文本和值
                inputText = valueFormat.format(alignedValue)
                if (alignedValue != value) {
                    onValueChange(alignedValue)
                }
            } else {
                // 无效输入，恢复为当前值
                inputText = valueFormat.format(value)
            }
        }
    }

    if (isInFloatingWindow) {
        // 悬浮窗环境：使用 FloatWindowEditText
        FloatWindowEditText(
            value = inputText,
            onValueChange = { text ->
                // 只允许数字和负号
                if (text.isEmpty() || text == "-" || text.toIntOrNull() != null) {
                    inputText = text
                }
            },
            modifier = modifier,
            hint = hint,
            singleLine = true,
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
            enabled = enabled,
            onFocusChange = { hasFocus ->
                if (!hasFocus) {
                    validateOnFocusLost()
                }
            }
        )
    } else {
        // 普通环境：使用 OutlinedTextField
        OutlinedTextField(
            value = inputText,
            onValueChange = { text ->
                // 只允许数字和负号
                if (text.isEmpty() || text == "-" || text.toIntOrNull() != null) {
                    inputText = text
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        validateOnFocusLost()
                    }
                },
            placeholder = { Text(hint) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}
