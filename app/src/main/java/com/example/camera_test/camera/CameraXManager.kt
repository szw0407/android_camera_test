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
import androidx.compose.runtime.LaunchedEffect
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
    
    // 相机初始化标志
    private var shouldReinitCamera = mutableStateOf(false)
    
    /**
     * 获取相机重新初始化标志
     */
    fun getShouldReinitCamera(): MutableState<Boolean> = shouldReinitCamera
    
    /**
     * 切换前后摄像头
     */
    fun switchCamera() {
        // 切换镜头方向状态
        lensFacing.value = if (lensFacing.value == CameraSelector.LENS_FACING_BACK) 
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        
        // 当切换到前置摄像头时，确保关闭闪光灯（因为大多数前置相机没有闪光灯）
        if (lensFacing.value == CameraSelector.LENS_FACING_FRONT) {
            torchState.value = false
            camera?.cameraControl?.enableTorch(false)
        }
        
        // 触发相机重新初始化
        shouldReinitCamera.value = !shouldReinitCamera.value
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
     * 直接控制闪光灯状态
     */
    fun enableTorch(enable: Boolean) {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                torchState.value = enable
                it.cameraControl.enableTorch(enable)
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
            // 这里不需要unbindAll，因为我们只是检查相机可用性，没有绑定任何用例
            hasCamera
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 创建相机预览组件
     * 每次lensFacing变化时，都会重新绑定相机，实现真正的前后摄像头切换
     */
    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        onImageCaptureCreated: (ImageCapture) -> Unit,
        lifecycleOwner: LifecycleOwner
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val currentLensFacing = getLensFacing()
        val currentTorchState = getTorchState()
        // 用于保存ImageCapture实例
        val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }
        // 用于保存PreviewView实例
        val previewViewState = remember { mutableStateOf<PreviewView?>(null) }

        // 绑定相机的函数
        fun bindCamera() {
            val previewView = previewViewState.value ?: return
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCaptureState.value = imageCapture
                onImageCaptureCreated(imageCapture)
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(currentLensFacing.value)
                    .build()
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    camera?.let {
                        if (it.cameraInfo.hasFlashUnit()) {
                            it.cameraControl.enableTorch(currentTorchState.value)
                        }
                    }
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // 只要镜头方向或闪光灯状态变化就重新绑定相机
        LaunchedEffect(currentLensFacing.value) {
            // 只有在PreviewView已创建时才绑定
            if (previewViewState.value != null) {
                bindCamera()
            }
        }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewViewState.value = previewView
                // 首次创建时立即绑定相机
                bindCamera()
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
