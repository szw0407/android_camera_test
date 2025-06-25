package com.example.camera_test

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import com.example.camera_test.ui.theme.Camera_testTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    lateinit var cameraExecutor: ExecutorService
    
    // 用于系统相机的变量
    var systemCameraLaunchIntent: Intent? = null
    var triggerSystemCameraLaunch = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setContent {
            Camera_testTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraApp(
                        cameraExecutor = cameraExecutor,
                        initialBitmap = systemCameraBitmap,
                        triggerSystemCamera = triggerSystemCameraLaunch
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_CREATE_DOCUMENT = 1001
    }
}

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
    val systemCameraLauncher = rememberSystemCameraLauncher { bitmap ->
        capturedBitmap = bitmap
        shouldShowCamera = false
        
        // 如果是MainActivity，重置触发标志
        val mainActivity = context as? MainActivity
        mainActivity?.triggerSystemCameraLaunch = false
    }
    
    // 检查是否需要启动系统相机
    LaunchedEffect(triggerSystemCamera) {
        val mainActivity = context as? MainActivity
        if (mainActivity?.triggerSystemCameraLaunch == true && mainActivity.systemCameraLaunchIntent != null) {
            systemCameraLauncher.launch(mainActivity.systemCameraLaunchIntent!!)
            mainActivity.triggerSystemCameraLaunch = false
        }
    }
    
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
                    onSystemCameraSelected = { launchSystemCamera(context) }
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

@Composable
fun CameraMethodSelector(
    onCameraXSelected: () -> Unit,
    onSystemCameraSelected: () -> Unit
) {
    val context = LocalContext.current
    
    // 相机权限处理
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onCameraXSelected()
        } else {
            Toast.makeText(context, "需要相机权限才能使用自定义相机", Toast.LENGTH_SHORT).show()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "请选择拍照方式",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "相机图标",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "自定义相机",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "使用CameraX API，提供自定义界面和完全控制",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                onCameraXSelected()
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("使用自定义相机")
                }
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "系统相机图标",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "系统相机",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "使用设备内置的相机应用，界面由系统提供",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = { onSystemCameraSelected() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("使用系统相机")
                }
            }
        }
    }
}

@Composable
fun CameraContent(
    cameraExecutor: ExecutorService,
    onImageCaptured: (Bitmap) -> Unit,
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
            CameraPreview(
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
                        captureImage(
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

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current
    
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
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier
    )
}

private fun captureImage(
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

enum class SaveMethod {
    LEGACY_STORAGE, // 传统的文件IO存储
    MEDIA_STORE,    // MediaStore API存储
    DOCUMENT_API    // 用户选择保存路径
}

@Composable
fun ImagePreviewAndSave(
    bitmap: Bitmap,
    onNewPhotoRequested: () -> Unit,
    selectedSaveMethod: SaveMethod?,
    onSaveMethodSelected: (SaveMethod) -> Unit
) {
    val context = LocalContext.current
    var saveInProgress by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity
    
    // 活动结果处理器 - 用于Document API
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        Toast.makeText(context, "图片已保存到所选位置", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "保存失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        saveInProgress = false
    }
    
    // 存储权限请求处理器 - 用于传统存储
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val saved = saveImageUsingLegacyMethod(context, bitmap)
            if (saved) {
                Toast.makeText(context, "图片已使用传统方式保存", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "使用传统方式保存失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
        }
        saveInProgress = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图片预览部分
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "已拍摄的照片",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        // 保存方法选择
        Text(
            text = "选择保存方式",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SaveMethodButton(
                text = "传统文件IO",
                selected = selectedSaveMethod == SaveMethod.LEGACY_STORAGE,
                onClick = { onSaveMethodSelected(SaveMethod.LEGACY_STORAGE) }
            )
            
            SaveMethodButton(
                text = "MediaStore API",
                selected = selectedSaveMethod == SaveMethod.MEDIA_STORE,
                onClick = { onSaveMethodSelected(SaveMethod.MEDIA_STORE) }
            )
            
            SaveMethodButton(
                text = "用户选择保存位置",
                selected = selectedSaveMethod == SaveMethod.DOCUMENT_API,
                onClick = { onSaveMethodSelected(SaveMethod.DOCUMENT_API) }
            )
        }
        
        // 操作按钮区
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onNewPhotoRequested,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("重新拍摄")
            }
            
            Button(
                onClick = {
                    if (selectedSaveMethod != null && !saveInProgress) {
                        saveInProgress = true
                        when (selectedSaveMethod) {
                            SaveMethod.LEGACY_STORAGE -> {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    val saved = saveImageUsingLegacyMethod(context, bitmap)
                                    if (saved) {
                                        Toast.makeText(context, "图片已使用传统方式保存", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "使用传统方式保存失败", Toast.LENGTH_SHORT).show()
                                    }
                                    saveInProgress = false
                                }
                            }
                            SaveMethod.MEDIA_STORE -> {
                                val saved = saveImageUsingMediaStore(context, bitmap)
                                if (saved) {
                                    Toast.makeText(context, "图片已使用MediaStore保存", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "使用MediaStore保存失败", Toast.LENGTH_SHORT).show()
                                }
                                saveInProgress = false
                            }
                            SaveMethod.DOCUMENT_API -> {
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_TITLE, "Photo_${System.currentTimeMillis()}.jpg")
                                }
                                try {
                                    createDocumentLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "启动文件选择器失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    saveInProgress = false
                                }
                            }
                        }
                    } else if (selectedSaveMethod == null) {
                        Toast.makeText(context, "请先选择保存方式", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                enabled = selectedSaveMethod != null && !saveInProgress
            ) {
                if (saveInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "保存",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("保存图片")
                }
            }
        }
        
        // 保存方法说明
        AnimatedVisibility(
            visible = selectedSaveMethod != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedSaveMethod?.let { method ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = when (method) {
                                SaveMethod.LEGACY_STORAGE -> "传统文件IO方式"
                                SaveMethod.MEDIA_STORE -> "MediaStore API方式"
                                SaveMethod.DOCUMENT_API -> "用户选择保存位置"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = when (method) {
                                SaveMethod.LEGACY_STORAGE -> 
                                    "适用于Android 10以下的系统，通过直接文件IO操作将图片保存到设备存储。此方法在较新的Android系统上需要特殊权限。"
                                SaveMethod.MEDIA_STORE -> 
                                    "适用于所有Android版本，通过系统的媒体库API保存图片，无需请求存储权限，符合新版Android的隐私要求。"
                                SaveMethod.DOCUMENT_API -> 
                                    "让用户自行选择保存位置，提供最大灵活性，可以保存到第三方存储服务，如云存储。"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaveMethodButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 2.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// 用于存储系统相机拍摄的照片
private var systemCameraBitmap: Bitmap? = null

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

// 启动系统相机
private fun launchSystemCamera(context: Context) {
    // 这个函数现在只是一个传递，真正的实现在CameraApp中
    // 由于跨Compose作用域的限制，我们需要让调用方完成剩余工作
    
    // 系统相机仍然使用旧的方式启动，但会通过全局变量传递bitmap
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
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraApp(
                        cameraExecutor = mainActivity.cameraExecutor,
                        initialBitmap = systemCameraBitmap,
                        triggerSystemCamera = true
                    )
                }
            }
        }
    }
}

// 使用传统文件IO方法保存图片
private fun saveImageUsingLegacyMethod(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(imagesDir, "CameraApp")
        
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        val file = File(appDir, fileName)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        
        // 确保图片出现在图库中
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(file)
        mediaScanIntent.data = contentUri
        context.sendBroadcast(mediaScanIntent)
        
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// 使用MediaStore API保存图片
private fun saveImageUsingMediaStore(context: Context, bitmap: Bitmap): Boolean {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "IMG_$timeStamp.jpg"
    
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraApp")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    
    return try {
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            
            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
