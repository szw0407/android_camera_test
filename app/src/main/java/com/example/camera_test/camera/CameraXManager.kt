package com.example.camera_test.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService

/**
 * CameraX 相关功能的管理类
 */
object CameraXManager {
    // 相机方向状态：默认为后置相机
    private val lensFacing = mutableStateOf(CameraSelector.LENS_FACING_BACK)
    
    // 闪光灯状态
    private val torchState = mutableStateOf(false)
    
    // 当前相机实例
    private var camera: Camera? = null
    
    /**
     * 获取当前使用的相机镜头方向
     */
    fun getLensFacing(): MutableState<Int> = lensFacing
    
    /**
     * 获取当前闪光灯状态
     */
    fun getTorchState(): MutableState<Boolean> = torchState
    
    /**
     * 切换前后摄像头
     */
    fun switchCamera() {
        lensFacing.value = if (lensFacing.value == CameraSelector.LENS_FACING_BACK) 
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    }
    
    /**
     * 切换闪光灯状态
     */
    fun toggleTorch() {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                torchState.value = !torchState.value
                it.cameraControl.enableTorch(torchState.value)
            }
        }
    }
    
    /**
     * 检查设备是否有前置摄像头
     */
    fun hasFrontCamera(context: Context): Boolean {
        return try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val hasCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            cameraProvider.unbindAll() // 释放相机提供程序
            hasCamera
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 创建相机预览组件
     */
    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        onImageCaptureCreated: (ImageCapture) -> Unit,
        lifecycleOwner: LifecycleOwner
    ) {
        val currentLensFacing = remember { lensFacing }
        val currentTorchState = remember { torchState }
        
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    
                    onImageCaptureCreated(imageCapture)
                    
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(currentLensFacing.value)
                        .build()
                    
                    try {
                        cameraProvider.unbindAll()
                        // 绑定相机实例并保存引用，以便控制闪光灯等功能
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        
                        // 设置初始闪光灯状态
                        camera?.let {
                            if (it.cameraInfo.hasFlashUnit()) {
                                it.cameraControl.enableTorch(currentTorchState.value)
                            }
                        }
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = modifier
        )
    }

    /**
     * 拍摄照片
     */
    fun captureImage(
        context: Context,
        imageCapture: ImageCapture,
        executor: ExecutorService,
        onImageCaptured: (Bitmap) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val photoFile = File(
            context.cacheDir,
            "temp_${System.currentTimeMillis()}.jpg"
        )
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        ContextCompat.getMainExecutor(context).execute {
                            onImageCaptured(bitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exception)
                    }
                }
            }
        )
    }
}
