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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
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
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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
    val aospLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                imageBitmap = bitmap
                showSaveButton = true
            }
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
            aospLauncher.launch(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("AOSP原生拍照")
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
            // 预先声明权限请求Launcher
            val writePermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
                if (isGranted) {
                    imageBitmap?.let { saveImageToGallery(context, it) }
                }
            }
            Button(onClick = {
                if (useAospApi) {
                    imageBitmap?.let { saveImageAospApi(context, it) }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        imageBitmap?.let { saveImageToGallery(context, it) }
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            imageBitmap?.let { saveImageToGallery(context, it) }
                        } else {
                            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("保存")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    cameraExecutor: ExecutorService,
    onImageCaptured: (Bitmap) -> Unit
) {
    val photoFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                onImageCaptured(bmp)
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
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val filename = "AOSP_IMG_${System.currentTimeMillis()}.jpg"
    val contentValues = android.content.ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        resolver.openOutputStream(it)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        Toast.makeText(context, "图片已保存到对应目录", Toast.LENGTH_SHORT).show()
    }
} else {
    Toast.makeText(context, "当前安卓版本不支持AOSP原生保存", Toast.LENGTH_SHORT).show()
}
}