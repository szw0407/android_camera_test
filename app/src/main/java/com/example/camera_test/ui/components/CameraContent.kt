package com.example.camera_test.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.camera.core.ImageCapture
import android.content.Context
import android.widget.Toast
import com.example.camera_test.camera.CameraXManager

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
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f)) {
            CameraXManager.CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onImageCaptureCreated = { imageCapture = it },
                lifecycleOwner = lifecycleOwner
            )
            
            // 关闭按钮
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "关闭相机",
                    tint = Color.White
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
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
