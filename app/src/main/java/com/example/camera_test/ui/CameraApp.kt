package com.example.camera_test.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camera_test.MainActivity
import com.example.camera_test.camera.SystemCameraManager
import com.example.camera_test.data.SaveMethod
import com.example.camera_test.ui.components.CameraContent
import com.example.camera_test.ui.components.CameraMethodSelector
import com.example.camera_test.ui.components.ImagePreviewAndSave
import java.util.concurrent.ExecutorService
import com.example.camera_test.ui.theme.Camera_testTheme
import androidx.compose.material3.Surface
import java.util.concurrent.Executors

/**
 * 相机应用的主组合函数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraApp(
    cameraExecutor: ExecutorService,
    initialBitmap: Bitmap? = null,
    triggerSystemCamera: Boolean = false
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(initialBitmap) }
    var shouldShowCamera by remember { mutableStateOf(false) }
    var selectedImageSaveMethod by remember { mutableStateOf<SaveMethod?>(null) }
    
    // 系统相机拍照启动器
    val systemCameraLauncher = SystemCameraManager.rememberSystemCameraLauncher { bitmap ->
        capturedBitmap = bitmap
        shouldShowCamera = false
        
        // 如果是MainActivity，重置触发标志
        val mainActivity = context as? MainActivity
        mainActivity?.triggerSystemCameraLaunch?.value = false
    }
    
    // 系统相机效果
    SystemCameraManager.SystemCameraEffect(
        triggerSystemCamera = triggerSystemCamera,
        systemCameraIntent = (context as? MainActivity)?.systemCameraLaunchIntent,
        systemCameraLauncher = systemCameraLauncher
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "相机应用", 
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (capturedBitmap == null && !shouldShowCamera) {
                CameraMethodSelector(
                    onCameraXSelected = { shouldShowCamera = true },
                    onSystemCameraSelected = { SystemCameraManager.launchSystemCamera(context) }
                )
            } else if (shouldShowCamera) {
                CameraContent(
                    cameraExecutor = cameraExecutor,
                    onImageCaptured = { bitmap ->
                        capturedBitmap = bitmap
                        shouldShowCamera = false
                    },
                    onClose = { shouldShowCamera = false }
                )
            } else {
                ImagePreviewAndSave(
                    bitmap = capturedBitmap!!,
                    onNewPhotoRequested = {
                        capturedBitmap = null
                        shouldShowCamera = false
                    },
                    selectedSaveMethod = selectedImageSaveMethod,
                    onSaveMethodSelected = { method ->
                        selectedImageSaveMethod = method
                    }
                )
            }
        }
    }
}

/**
 * 相机应用预览 - 选择相机方式界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 420, heightDp = 880)
@Composable
fun CameraAppPreview_Selector() {
    // 创建一个模拟的ExecutorService用于预览
    val previewExecutor = remember { Executors.newSingleThreadExecutor() }
    
    Camera_testTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CameraApp(
                cameraExecutor = previewExecutor,
                initialBitmap = null,
                triggerSystemCamera = false
            )
        }
    }
}

/**
 * 相机应用预览 - 图片预览和保存界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 420, heightDp = 880)
@Composable
fun CameraAppPreview_ImagePreview() {
    // 创建一个模拟的ExecutorService用于预览
    val previewExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // 创建一个简单的预览用位图
    val previewBitmap = remember {
        val width = 400
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.LTGRAY)
        
        // 绘制一些内容，以便让预览看起来更像实际的照片
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLUE
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, height / 2f, 50f, paint)
        
        bitmap
    }
    
    Camera_testTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CameraApp(
                cameraExecutor = previewExecutor,
                initialBitmap = previewBitmap,
                triggerSystemCamera = false
            )
        }
    }
}
