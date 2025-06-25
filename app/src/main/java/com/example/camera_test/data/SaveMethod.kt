package com.example.camera_test.data

/**
 * 图像保存方法的枚举类
 */
enum class SaveMethod {
    LEGACY_STORAGE, // 传统的文件IO存储
    MEDIA_STORE,    // MediaStore API存储
    DOCUMENT_API    // 用户选择保存路径
}
