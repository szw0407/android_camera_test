package com.example.camera_test.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

/**
 * 保存方法选择按钮组件
 */
@Composable
fun SaveMethodButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 2.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 保存方法按钮预览 - 未选中状态
 */
@Preview(showBackground = true)
@Composable
fun SaveMethodButtonPreviewUnselected() {
    com.example.camera_test.ui.theme.Camera_testTheme {
        Surface {
            SaveMethodButton(
                text = "示例按钮",
                selected = false,
                onClick = {}
            )
        }
    }
}

/**
 * 保存方法按钮预览 - 已选中状态
 */
@Preview(showBackground = true)
@Composable
fun SaveMethodButtonPreviewSelected() {
    com.example.camera_test.ui.theme.Camera_testTheme {
        Surface {
            SaveMethodButton(
                text = "示例按钮",
                selected = true,
                onClick = {}
            )
        }
    }
}
