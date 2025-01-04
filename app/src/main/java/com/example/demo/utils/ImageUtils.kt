package com.example.demo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

suspend fun generatePreview(
    context: Context,
    imageUri: Uri,
    quality: Int,
    maxSize: Int
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            // 计算新的尺寸
            val useScale = max(originalBitmap.width, originalBitmap.height) > maxSize
            val scaleFactor = if (originalBitmap.width > originalBitmap.height) {
                maxSize.toFloat() / originalBitmap.width
            } else {
                maxSize.toFloat() / originalBitmap.height
            }
            
            val newWidth = if (useScale) (originalBitmap.width * scaleFactor).toInt() else originalBitmap.width
            val newHeight = if (useScale) (originalBitmap.height * scaleFactor).toInt() else originalBitmap.height

            // 调整尺寸
            val resizedBitmap = if (useScale) {
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else originalBitmap

            // 压缩质量
            val outputBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
            ByteArrayOutputStream().use { stream ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                val byteArray = stream.toByteArray()
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }.also {
                // 清理不再需要的位图
                if (resizedBitmap !== originalBitmap) {
                    resizedBitmap.recycle()
                }
                originalBitmap.recycle()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
} 