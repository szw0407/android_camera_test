package com.example.camera_test.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.widget.Toast
import com.example.camera_test.camera.CameraXManager
import com.example.camera_test.ui.theme.Camera_testTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * 相机控制按钮
 */
@Composable
fun CameraControlButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 相机内容组件，包括预览和拍照功能
 */
@Composable
fun CameraContent(
    cameraExecutor: java.util.concurrent.ExecutorService,
    onImageCaptured: (android.graphics.Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    
    // 状态监控
    val lensFacingState = remember { CameraXManager.getLensFacing() }
    val torchState = remember { CameraXManager.getTorchState() }
    
    // 检查设备是否有前置摄像头
    val hasFrontCamera = remember { mutableStateOf(false) }
    
    // 检查前置摄像头是否可用
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            hasFrontCamera.value = CameraXManager.hasFrontCamera(context)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f)) {
            // 相机预览
            CameraXManager.CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onImageCaptureCreated = { imageCapture = it },
                lifecycleOwner = lifecycleOwner
            )
            
            // 顶部控制栏
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 闪光灯控制
                if (lensFacingState.value == CameraSelector.LENS_FACING_BACK) {
                    CameraControlButton(
                        onClick = { CameraXManager.toggleTorch() },
                        icon = if (torchState.value) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (torchState.value) "关闭闪光灯" else "打开闪光灯"
                    )
                } else {
                    // 前置相机通常没有闪光灯，但为了保持UI一致，放一个占位符
                    Box(modifier = Modifier.size(48.dp))
                }
                
                // 关闭按钮
                CameraControlButton(
                    onClick = onClose,
                    icon = Icons.Default.Clear,
                    contentDescription = "关闭相机"
                )
            }
            
            // 右侧控制栏（前后摄像头切换）
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            ) {
                // 仅当设备有前置摄像头时才显示切换按钮
                if (hasFrontCamera.value) {
                    CameraControlButton(
                        onClick = { CameraXManager.switchCamera() },
                        icon = Icons.Default.Cameraswitch,
                        contentDescription = "切换相机"
                    )
                }
            }
        }
        
        // 底部控制栏包含拍摄按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    if (!isCapturing && imageCapture != null) {
                        isCapturing = true
                        CameraXManager.captureImage(
                            context = context,
                            imageCapture = imageCapture!!,
                            executor = cameraExecutor,
                            onImageCaptured = { bitmap ->
                                onImageCaptured(bitmap)
                                isCapturing = false
                            },
                            onError = {
                                isCapturing = false
                                Toast.makeText(context, "照片拍摄失败", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .border(4.dp, Color.White, CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) Color.LightGray.copy(alpha = 0.5f)
                    else Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isCapturing) Color.DarkGray else Color.LightGray,
                            shape = CircleShape
                        )
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCapturing) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

/**
 * 相机内容组件预览
 */
@Preview(showBackground = true)
@Composable
fun CameraContentPreview() {
    Camera_testTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // 使用临时的Executor实例，仅用于预览
            val previewExecutor = remember { Executors.newSingleThreadExecutor() }
            CameraContent(
                cameraExecutor = previewExecutor,
                onImageCaptured = {},
                onClose = {}
            )
        }
    }
}
