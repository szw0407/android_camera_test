package com.example.camera_test.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.example.camera_test.MainActivity
import com.example.camera_test.ui.theme.Camera_testTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect

/**
 * 系统相机管理类
 */
object SystemCameraManager {
    // 用于存储系统相机拍摄的照片
    var systemCameraBitmap: Bitmap? = null

    /**
     * 创建系统相机启动器
     */
    @Composable
    fun rememberSystemCameraLauncher(onImageCaptured: (Bitmap) -> Unit): ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    onImageCaptured(bitmap)
                }
            }
        }
    }

    /**
     * 系统相机效果组件
     */
    @Composable
    fun SystemCameraEffect(
        triggerSystemCamera: Boolean,
        systemCameraIntent: Intent?,
        systemCameraLauncher: ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        
        // 检查是否需要启动系统相机
        LaunchedEffect(triggerSystemCamera) {
            val mainActivity = context as? MainActivity
            if (mainActivity?.triggerSystemCameraLaunch == true && mainActivity.systemCameraLaunchIntent != null) {
                systemCameraLauncher.launch(mainActivity.systemCameraLaunchIntent!!)
                mainActivity.triggerSystemCameraLaunch = false
            }
        }
    }

    /**
     * 启动系统相机
     */
    fun launchSystemCamera(context: Context) {
        // 由于跨Compose作用域的限制，通过MainActivity传递参数
        val mainActivity = context as? MainActivity
        if (mainActivity != null) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            mainActivity.systemCameraLaunchIntent = intent
            mainActivity.triggerSystemCameraLaunch = true
            
            // 重新绘制界面以触发systemCameraLauncher
            mainActivity.setContent {
                Camera_testTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        mainActivity.CameraApp(
                            cameraExecutor = mainActivity.cameraExecutor,
                            initialBitmap = systemCameraBitmap,
                            triggerSystemCamera = true
                        )
                    }
                }
            }
        }
    }
}
