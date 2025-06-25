package com.example.camera_test

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.camera_test.camera.SystemCameraManager
import com.example.camera_test.ui.CameraApp
import com.example.camera_test.ui.theme.Camera_testTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 应用程序主活动
 * 这个类是应用程序的入口点，负责初始化相机执行器和设置主界面
 */
class MainActivity : ComponentActivity() {

    // 相机执行线程
    lateinit var cameraExecutor: ExecutorService
    
    // 用于系统相机的变量 - 使用MutableState以便在值变化时触发重组
    var systemCameraLaunchIntent: Intent? = null
    val triggerSystemCameraLaunch = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化相机执行器
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // 设置界面内容
        setContent {
            Camera_testTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraApp(
                        cameraExecutor = cameraExecutor,
                        initialBitmap = SystemCameraManager.systemCameraBitmap,
                        triggerSystemCamera = triggerSystemCameraLaunch.value
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 确保释放相机执行器资源
        cameraExecutor.shutdown()
    }
    
    // 处理Activity结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
    
    companion object {
        // 用于文件命名的时间戳格式
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        // 用于Document API的请求码
        private const val REQUEST_CODE_CREATE_DOCUMENT = 1001
    }
}
