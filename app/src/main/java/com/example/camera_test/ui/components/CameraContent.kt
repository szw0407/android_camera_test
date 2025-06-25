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
import androidx.compose.ui.res.painterResource
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
 * 相机控制按钮 - 更现代化的设计
 */
@Composable
fun CameraControlButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0x88000000).copy(alpha = if (isSelected) 0.8f else 0.5f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) Color.Yellow else tint,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * 闪光灯控制按钮 - 支持三种状态（开、关、自动）
 */
@Composable
fun FlashControlButton(
    flashMode: Int,
    onFlashModeChanged: (Int) -> Unit
) {
    val (icon, description) = when (flashMode) {
        0 -> Pair(Icons.Default.FlashOff, "打开闪光灯")
        1 -> Pair(Icons.Default.FlashOn, "关闭闪光灯")
        else -> Pair(Icons.Default.FlashAuto, "自动闪光灯")
    }
    
    CameraControlButton(
        onClick = { 
            // 循环三种状态：关闭 -> 开启 -> 自动 -> 关闭
            onFlashModeChanged((flashMode + 1) % 3) 
        },
        icon = icon,
        contentDescription = description,
        isSelected = flashMode != 0
    )
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
    
    // 闪光灯模式：0=关闭，1=开启，2=自动
    var flashMode by remember { mutableStateOf(0) }
    
    // 检查设备是否有前置摄像头
    val hasFrontCamera = remember { mutableStateOf(false) }
    
    // 检查前置摄像头是否可用
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            hasFrontCamera.value = CameraXManager.hasFrontCamera(context)
        }
    }

    // 监听闪光灯模式的变化并应用
    LaunchedEffect(flashMode) {
        if (flashMode == 1) {
            CameraXManager.enableTorch(true)
        } else {
            CameraXManager.enableTorch(false)
        }
        // 注意：自动模式需要在拍照时特殊处理
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览
        CameraXManager.CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onImageCaptureCreated = { imageCapture = it },
            lifecycleOwner = lifecycleOwner
        )
        
        // 顶部控制栏 - 半透明背景
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0x44000000))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 闪光灯控制 - 仅在后置相机时显示
                if (lensFacingState.value == CameraSelector.LENS_FACING_BACK) {
                    FlashControlButton(
                        flashMode = flashMode,
                        onFlashModeChanged = { flashMode = it }
                    )
                } else {
                    // 占位符
                    Box(modifier = Modifier.size(56.dp))
                }
                
                // 关闭按钮
                CameraControlButton(
                    onClick = onClose,
                    icon = Icons.Default.Clear,
                    contentDescription = "关闭相机"
                )
            }
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
                    onClick = { 
                        CameraXManager.switchCamera() 
                        // 切换到前置相机时，强制关闭闪光灯
                        if (lensFacingState.value == CameraSelector.LENS_FACING_FRONT) {
                            flashMode = 0
                        }
                    },
                    icon = Icons.Default.Cameraswitch,
                    contentDescription = "切换相机"
                )
            }
        }
        
        // 底部控制栏包含拍摄按钮 - 半透明渐变背景
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0x99000000))
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
                    .size(80.dp)
                    .border(4.dp, Color.White, CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) Color.LightGray.copy(alpha = 0.5f)
                    else Color.White.copy(alpha = 0.2f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else Color.White)
                        .border(
                            width = 2.dp,
                            color = if (isCapturing) Color.DarkGray else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/**
 * 相机内容组件预览 - 使用模拟数据
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
            
            // 模拟相机预览的UI
            Box(modifier = Modifier.fillMaxSize()) {
                // 模拟相机预览背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray)
                )
                
                // 顶部控制栏 - 半透明背景
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color(0x44000000))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 闪光灯控制
                        CameraControlButton(
                            onClick = { },
                            icon = Icons.Default.FlashOff,
                            contentDescription = "闪光灯"
                        )
                        
                        // 关闭按钮
                        CameraControlButton(
                            onClick = { },
                            icon = Icons.Default.Clear,
                            contentDescription = "关闭相机"
                        )
                    }
                }
                
                // 右侧控制栏（前后摄像头切换）
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp)
                ) {
                    CameraControlButton(
                        onClick = { },
                        icon = Icons.Default.Cameraswitch,
                        contentDescription = "切换相机"
                    )
                }
                
                // 底部控制栏包含拍摄按钮
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0x99000000))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, Color.White, CircleShape),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}
