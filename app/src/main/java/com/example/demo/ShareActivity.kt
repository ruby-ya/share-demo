package com.example.demo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.example.demo.data.HistoryItem
import com.example.demo.ui.screens.ShareImageScreen
import com.example.demo.viewmodel.HistoryViewModel
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import kotlin.math.max

class ShareActivity : ComponentActivity() {
    private var currentImageUri: Uri? = null
    private val historyViewModel: HistoryViewModel by viewModels()

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        const val SHARE_REQUEST_CODE = 99
        private const val PERMISSION_REQUEST_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intent = intent
        if (Intent.ACTION_SEND == intent.action && intent.type?.startsWith("image/") == true) {
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (imageUri != null) {
                setContent {
                    ShareImageScreen(
                        imageUri = imageUri,
                        onPickImage = ::openImagePicker,
                        onShare = { quality, maxSize, compressionLevel ->
                            handleShare(imageUri, quality, maxSize, compressionLevel)
                        },
                        onNavigateUp = { finish() }
                    )
                }
            }
        } else {
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/gif"))
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun handleShare(imageUri: Uri, quality: Int, maxSize: Int, compressionLevel: Int) {
        val context = this
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 获取原始图片信息
                val originalSize = context.contentResolver.openInputStream(imageUri)?.use { 
                    it.available().toLong() 
                } ?: 0L

                // 检查是否是 GIF
                val isGif = contentResolver.getType(imageUri)?.equals("image/gif") == true
                        || imageUri.path?.endsWith(".gif", ignoreCase = true) == true

                if (isGif) {
                    // GIF 格式处理
                    val gifFile = File(
                        context.cacheDir,
                        "compressed_${System.currentTimeMillis()}.gif"
                    )

                    // 直接传递 Uri，不再需要 GifDrawable
                    compressGif(
                        context = context,
                        outputFile = gifFile,
                        width = maxSize,
                        height = maxSize,
                        quality = quality,
                        originalUri = imageUri
                    )

                    val compressedUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        gifFile
                    )

                    // 保存历史记录
                    saveToHistory(
                        originalUri = imageUri,
                        compressedUri = compressedUri,
                        originalSize = originalSize,
                        compressedSize = gifFile.length(),
                        isGif = true
                    )

                    withContext(Dispatchers.Main) {
                        shareImage(compressedUri, "image/gif")
                    }
                } else {
                    // 非 GIF 格式压缩为 JPG
                    val compressedBitmap = generatePreview(
                        context = context,
                        imageUri = imageUri,
                        quality = quality,
                        maxSize = maxSize
                    ) ?: return@launch

                    val compressedFile = File(
                        context.cacheDir,
                        "compressed_${System.currentTimeMillis()}.jpg"
                    ).apply {
                        outputStream().use { out ->
                            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                        }
                    }

                    val compressedUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        compressedFile
                    )

                    saveToHistory(
                        originalUri = imageUri,
                        compressedUri = compressedUri,
                        originalSize = originalSize,
                        compressedSize = compressedFile.length(),
                        isGif = false
                    )

                    withContext(Dispatchers.Main) {
                        shareImage(compressedUri, "image/jpeg")
                    }

                    compressedBitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "处理图片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareImage(uri: Uri, mimeType: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "分享图片"))
    }

    private fun saveToHistory(
        originalUri: Uri,
        compressedUri: Uri,
        originalSize: Long,
        compressedSize: Long,
        isGif: Boolean
    ) {
        lifecycleScope.launch {
            try {
                contentResolver.openInputStream(originalUri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)

                    val fileName = contentResolver.query(originalUri, null, null, null, null)?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                it.getString(nameIndex)
                            } else {
                                "image_${System.currentTimeMillis()}.${if (isGif) "gif" else "jpg"}"
                            }
                        } else {
                            "image_${System.currentTimeMillis()}.${if (isGif) "gif" else "jpg"}"
                        }
                    } ?: "image_${System.currentTimeMillis()}.${if (isGif) "gif" else "jpg"}"

                    val historyItem = HistoryItem(
                        imageUri = compressedUri.toString(),
                        fileName = fileName,
                        fileSize = originalSize,
                        width = options.outWidth,
                        height = options.outHeight,
                        timestamp = Date(),
                        compressedSize = compressedSize
                    )

                    historyViewModel.insertHistory(historyItem)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generatePreview(
        context: Context,
        imageUri: Uri,
        quality: Int,
        maxSize: Int
    ): Bitmap? {
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it)
        }
        if (bitmap == null) return null

        val useScale = max(bitmap.width, bitmap.height) > maxSize
        val scaleFactor = if (bitmap.width > bitmap.height) {
            maxSize.toFloat() / bitmap.width
        } else {
            maxSize.toFloat() / bitmap.height
        }
        val newWidth = if (useScale) (bitmap.width * scaleFactor).toInt() else bitmap.width
        val newHeight = if (useScale) (bitmap.height * scaleFactor).toInt() else bitmap.height

        val resizedBitmap = if (useScale) {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else bitmap

        return resizedBitmap
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val selectedImageUri = data.data
                    if (selectedImageUri != null) {
                        currentImageUri = selectedImageUri
                        startCrop(selectedImageUri)
                    }
                }
            }
            UCrop.REQUEST_CROP -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val resultUri = UCrop.getOutput(data)
                    if (resultUri != null) {
                        currentImageUri = resultUri
                        setContent {
                            ShareImageScreen(
                                imageUri = resultUri,
                                onPickImage = ::openImagePicker,
                                onShare = { quality, maxSize, compressionLevel ->
                                    handleShare(resultUri, quality, maxSize, compressionLevel)
                                },
                                onNavigateUp = { finish() }
                            )
                        }
                    }
                }
            }
            SHARE_REQUEST_CODE -> {
                finish()
            }
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.gif"))
        
        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1024, 1024)
            .withOptions(UCrop.Options().apply {
                setCompressionQuality(80)
                setHideBottomControls(false)
                setFreeStyleCropEnabled(true)
            })
            .start(this)
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 及以上
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
            }
        } else {
            // Android 9-12
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
        }
    }

    // 修改 compressGif 函数
    private suspend fun compressGif(
        context: Context,
        outputFile: File,
        width: Int,
        height: Int,
        quality: Int,
        originalUri: Uri
    ) = withContext(Dispatchers.IO) {
        try {
            // 创建临时输入文件
            val tempInputFile = File(context.cacheDir, "temp_input_${System.currentTimeMillis()}.gif")
            
            // 复制原始文件到临时文件
            context.contentResolver.openInputStream(originalUri)?.use { input ->
                tempInputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 根据质量参数计算具体的压缩参数
            val fps = when {
                quality < 30 -> 10  // 低质量：降低帧率
                quality < 60 -> 15
                else -> 20         // 保持较高帧率
            }

            val colors = when {
                quality < 30 -> 64    // 低质量：减少颜色数
                quality < 60 -> 128
                quality < 90 -> 192
                else -> 256          // 保持全部颜色
            }

            // 构建 FFmpeg 命令
            val command = arrayOf(
                "-y",  // 覆盖输出文件
                "-i", tempInputFile.absolutePath,
                "-vf", buildString {
                    // 调整分辨率，保持宽高比
                    append("scale=$width:$height:force_original_aspect_ratio=decrease,")
                    // 设置帧率
                    append("fps=$fps,")
                    // 调色板生成
                    append("split[s0][s1];")
                    append("[s0]palettegen=max_colors=$colors:stats_mode=single[p];")
                    // 使用调色板和抖动
                    append("[s1][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle")
                },
                // 设置压缩级别
                "-compression_level", "${(100 - quality) / 10}",
                outputFile.absolutePath
            )

            // 执行 FFmpeg 命令
            val session = FFmpegKit.execute(command.joinToString(" "))
            
            if (ReturnCode.isSuccess(session.returnCode)) {
                // 验证输出文件
                if (!outputFile.exists() || outputFile.length() == 0L) {
                    throw Exception("Output file is empty or does not exist")
                }
            } else {
                // 如果主要命令失败，尝试使用备用命令（更简单的压缩）
                val fallbackCommand = arrayOf(
                    "-y",
                    "-i", tempInputFile.absolutePath,
                    "-vf", "scale=$width:$height:force_original_aspect_ratio=decrease,fps=$fps",
                    outputFile.absolutePath
                )
                
                val fallbackSession = FFmpegKit.execute(fallbackCommand.joinToString(" "))
                if (!ReturnCode.isSuccess(fallbackSession.returnCode)) {
                    throw Exception("FFmpeg execution failed: ${session.failStackTrace}")
                }
            }

            // 清理临时文件
            tempInputFile.delete()

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "GIF 处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            // 如果处理失败，复制原始文件
            context.contentResolver.openInputStream(originalUri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            // 清理所有临时文件
            try {
                context.cacheDir.listFiles { file ->
                    file.name.startsWith("temp_")
                }?.forEach { it.delete() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1

        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2

            while ((halfHeight / inSampleSize) >= reqHeight
                && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    // 修改预估大小计算，使其更准确
    private fun calculateEstimatedSize(
        originalSize: Long,
        quality: Int,
        maxSize: Int,
        width: Int,
        height: Int,
        isGif: Boolean
    ): Long {
        if (!isGif) {
            // JPEG 的预估保持不变
            val sizeReduction = (100 - quality) / 100f
            val scaleReduction = if (maxOf(width, height) > maxSize) {
                val scale = maxSize.toFloat() / maxOf(width, height)
                1 - (scale * scale)
            } else 0f
            return (originalSize * (1 - sizeReduction) * (1 - scaleReduction)).toLong()
        } else {
            // GIF 的预估大小计算
            val scaleRatio = if (maxOf(width, height) > maxSize) {
                maxSize.toFloat() / maxOf(width, height)
            } else 1f
            
            // 考虑帧率和颜色数量的影响
            val qualityFactor = (quality / 100f).let { q ->
                // 非线性映射，低质量时压缩更aggressive
                0.3f + (0.7f * q * q)
            }
            
            val sizeFactor = scaleRatio * scaleRatio
            val frameRateFactor = 0.8f  // 考虑帧率限制的影响
            
            return (originalSize * sizeFactor * qualityFactor * frameRateFactor).toLong()
        }
    }
} 