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
        
        // 直接使用传入的triggerSystemCamera参数，而不是从MainActivity再次获取
        LaunchedEffect(triggerSystemCamera, systemCameraIntent) {
            android.util.Log.d("SystemCameraEffect", "LaunchedEffect触发: triggerSystemCamera=$triggerSystemCamera")
            
            try {
                val mainActivity = context as? MainActivity
                // 使用传入的triggerSystemCamera参数，保持逻辑一致性
                if (triggerSystemCamera && mainActivity?.systemCameraLaunchIntent != null) {
                    android.util.Log.d("SystemCameraEffect", "条件满足，准备启动相机")
                    
                    // 确保相机意图存在且有效
                    if (mainActivity.systemCameraLaunchIntent!!.resolveActivity(context.packageManager) != null) {
                        android.util.Log.d("SystemCameraEffect", "启动系统相机...")
                        Toast.makeText(context, "正在打开相机...", Toast.LENGTH_SHORT).show()
                        systemCameraLauncher.launch(mainActivity.systemCameraLaunchIntent!!)
                    } else {
                        Toast.makeText(context, "无法启动相机应用", Toast.LENGTH_SHORT).show()
                        android.util.Log.e("SystemCameraEffect", "无法启动相机应用")
                    }
                    
                    // 无论成功与否，重置触发标志
                    mainActivity.triggerSystemCameraLaunch.value = false
                    android.util.Log.d("SystemCameraEffect", "触发标志已重置为false")
                }
            } catch (e: Exception) {
                // 捕获可能的异常
                Toast.makeText(context, "启动相机时出错: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("SystemCameraEffect", "异常: ${e.message}", e)
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
                    // 使用MutableState的value设置，确保触发重组
                    mainActivity.triggerSystemCameraLaunch.value = true
                    
                    // 打印日志以便调试
                    android.util.Log.d("SystemCameraManager", "已设置触发标志为true，准备启动系统相机")
                    Toast.makeText(context, "正在启动系统相机...", Toast.LENGTH_SHORT).show()
                } else {
                    // 设备不支持相机
                    Toast.makeText(context, "设备不支持相机功能", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("SystemCameraManager", "设备不支持相机功能")
                }
            } catch (e: Exception) {
                // 捕获任何可能的异常
                Toast.makeText(context, "启动相机时发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}
