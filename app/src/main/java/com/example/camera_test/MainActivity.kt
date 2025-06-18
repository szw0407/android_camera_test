package com.example.camera_test

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.camera_test.ui.theme.Camera_testTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            Camera_testTheme {
                CameraTestScreen(cameraExecutor)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CameraTestScreen(cameraExecutor: ExecutorService) {
    val context = LocalContext.current as Activity
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var useAospApi by remember { mutableStateOf(false) }
    var showSaveButton by remember { mutableStateOf(false) }
    var showCameraX by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
        if (isGranted) {
            showCameraX = true
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            useAospApi = false
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                showCameraX = true
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("授权相机权限并拍照（自定义快门）")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            useAospApi = true
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val bitmap = result.data?.extras?.get("data") as? Bitmap
                    if (bitmap != null) {
                        imageBitmap = bitmap
                        showSaveButton = true
                    }
                }
            }
            launcher.launch(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("AOSP API无权限拍照")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (showCameraX) {
            CameraXPreview(
                onImageCaptured = { bmp ->
                    imageBitmap = bmp
                    showSaveButton = true
                    showCameraX = false
                },
                cameraExecutor = cameraExecutor,
                setImageCapture = { imageCapture = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                imageCapture?.let { capture ->
                    takePhoto(context, capture, cameraExecutor) { bmp ->
                        imageBitmap = bmp
                        showSaveButton = true
                        showCameraX = false
                    }
                }
            }) {
                Text("快门")
            }
        }
        imageBitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(240.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (showSaveButton) {
            Button(onClick = {
                if (useAospApi) {
                    // 直接保存（AOSP API，无需权限）
                    imageBitmap?.let { saveImageAospApi(context, it) }
                } else {
                    // 传统保存策略：新系统请求MediaStore权限，老系统请求WRITE_EXTERNAL_STORAGE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        imageBitmap?.let { saveImageToGallery(context, it) }
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            imageBitmap?.let { saveImageToGallery(context, it) }
                        } else {
                            val launcher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
                                if (isGranted) {
                                    imageBitmap?.let { saveImageToGallery(context, it) }
                                }
                            }
                            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("保存")
            }
        }
    }
}

@Composable
fun CameraXPreview(
    onImageCaptured: (Bitmap) -> Unit,
    cameraExecutor: ExecutorService,
    setImageCapture: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageCapture = ImageCapture.Builder().build()
            setImageCapture(imageCapture)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(ctx))
        previewView
    }, modifier = Modifier.size(320.dp))
}

fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    cameraExecutor: ExecutorService,
    onImageCaptured: (Bitmap) -> Unit
) {
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.cacheDir,
        "temp_${System.currentTimeMillis()}.jpg"
    ).build()
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                onImageCaptured(bmp)
                imageProxy.close()
            }
            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

fun saveImageToGallery(context: Context, bitmap: Bitmap) {
    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/CameraTest")
        }
        val imageUri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let { context.contentResolver.openOutputStream(it) }
    } else {
        val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM).toString() + "/CameraTest"
        val file = File(imagesDir)
        if (!file.exists()) file.mkdirs()
        FileOutputStream(File(file, filename))
    }
    fos?.use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
}

fun saveImageAospApi(context: Context, bitmap: Bitmap) {
    // 直接用AOSP接口保存图片（如直接写入cache或app私有目录，无需权限）
    val filename = "AOSP_IMG_${System.currentTimeMillis()}.jpg"
    val file = File(context.cacheDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    Toast.makeText(context, "AOSP方式已保存到app缓存目录", Toast.LENGTH_SHORT).show()
}