package com.example.camera_test.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.example.camera_test.MainActivity
import com.example.camera_test.ui.CameraApp
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
        
        // 添加更多参数到LaunchedEffect的key，使其更可靠地响应变化
        LaunchedEffect(triggerSystemCamera, systemCameraIntent) {
            try {
                val mainActivity = context as? MainActivity
                if (mainActivity?.triggerSystemCameraLaunch == true && mainActivity.systemCameraLaunchIntent != null) {
                    // 确保相机意图存在且有效
                    if (mainActivity.systemCameraLaunchIntent!!.resolveActivity(context.packageManager) != null) {
                        systemCameraLauncher.launch(mainActivity.systemCameraLaunchIntent!!)
                    } else {
                        Toast.makeText(context, "无法启动相机应用", Toast.LENGTH_SHORT).show()
                    }
                    // 无论成功与否，重置触发标志
                    mainActivity.triggerSystemCameraLaunch = false
                }
            } catch (e: Exception) {
                // 捕获可能的异常
                Toast.makeText(context, "启动相机时出错: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
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
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                
                // 检查设备是否支持相机
                if (intent.resolveActivity(context.packageManager) != null) {
                    mainActivity.systemCameraLaunchIntent = intent
                    mainActivity.triggerSystemCameraLaunch = true
                    
                    // 不再重新设置整个内容，只设置触发标志，让LaunchedEffect监听变化
                    // 这会更安全，不会干扰应用的状态管理
                } else {
                    // 设备不支持相机
                    Toast.makeText(context, "设备不支持相机功能", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 捕获任何可能的异常
                Toast.makeText(context, "启动相机时发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}
