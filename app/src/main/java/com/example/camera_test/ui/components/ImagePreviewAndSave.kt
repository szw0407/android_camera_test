package com.example.camera_test.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.camera_test.data.SaveMethod
import com.example.camera_test.utils.ImageSaver

/**
 * 图片预览和保存组件
 */
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
            val saved = ImageSaver.saveImageUsingLegacyMethod(context, bitmap)
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
//            horizontalArrangement = Arrangement.SpaceEvenly
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
                text = "SAF (Document API)",
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
                                    val saved = ImageSaver.saveImageUsingLegacyMethod(context, bitmap)
                                    if (saved) {
                                        Toast.makeText(context, "图片已使用传统方式保存", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "使用传统方式保存失败", Toast.LENGTH_SHORT).show()
                                    }
                                    saveInProgress = false
                                }
                            }
                            SaveMethod.MEDIA_STORE -> {
                                val saved = ImageSaver.saveImageUsingMediaStore(context, bitmap)
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

/**
 * ImagePreviewAndSave预览
 */
@Preview(showBackground = true)
@Composable
fun ImagePreviewAndSavePreview() {
    val previewBitmap = remember {
        // 创建一个简单的预览用位图
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
    
    com.example.camera_test.ui.theme.Camera_testTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ImagePreviewAndSave(
                bitmap = previewBitmap,
                onNewPhotoRequested = {},
                selectedSaveMethod = SaveMethod.MEDIA_STORE,
                onSaveMethodSelected = {}
            )
        }
    }
}
