package com.example.demo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.demo.ui.screens.ShareImageScreen
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import androidx.activity.viewModels
import com.example.demo.data.HistoryItem
import com.example.demo.viewmodel.HistoryViewModel
import java.util.Date
import android.content.pm.PackageManager
import android.os.Build

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
        if (imageUri.path?.endsWith(".gif") == true) {
            // GIF 直接分享，记录原始信息
            saveToHistory(
                originalUri = imageUri,
                processedUri = imageUri,  // GIF 不处理，原始和处理后的 URI 相同
                processedSize = null      // 不压缩，所以没有处理后的大小
            )
            shareGifDirectly(imageUri)
        } else {
            val context = this
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = context.contentResolver.openInputStream(imageUri)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: return@launch

                    val gifFile = convertBitmapToGif(
                        context = context,
                        bitmap = bitmap,
                        fileName = "emoticon_${System.currentTimeMillis()}.gif",
                        maxSize = maxSize,
                        quality = quality,
                        compressionLevel = compressionLevel
                    )
                    
                    // 获取处理后的 URI
                    val processedUri = FileProvider.getUriForFile(
                        context,
                        "${packageName}.fileprovider",
                        gifFile
                    )

                    // 保存历史记录，包含处理后的信息
                    saveToHistory(
                        originalUri = imageUri,
                        processedUri = processedUri,
                        processedSize = gifFile.length()
                    )

                    withContext(Dispatchers.Main) {
                        shareGifConverted(gifFile)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "处理图片失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveToHistory(
        originalUri: Uri,
        processedUri: Uri,
        processedSize: Long?
    ) {
        lifecycleScope.launch {
            try {
                // 获取原始图片信息
                contentResolver.openInputStream(originalUri)?.use { inputStream ->
                    // 获取图片信息
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)

                    // 获取文件信息
                    val cursor = contentResolver.query(originalUri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                            val sizeIndex = it.getColumnIndex(MediaStore.Images.Media.SIZE)
                            
                            val fileName = if (nameIndex != -1) {
                                it.getString(nameIndex)
                            } else {
                                originalUri.lastPathSegment?.substringAfterLast('/') ?: "unknown.gif"
                            }
                            
                            val originalSize = if (sizeIndex != -1) {
                                it.getLong(sizeIndex)
                            } else {
                                inputStream.available().toLong()
                            }

                            // 创建历史记录
                            val historyItem = HistoryItem(
                                imageUri = processedUri.toString(),  // 保存处理后的 URI
                                fileName = fileName,
                                fileSize = originalSize,             // 原始文件大小
                                width = options.outWidth,
                                height = options.outHeight,
                                timestamp = Date(),
                                compressedSize = processedSize       // 处理后的文件大小
                            )

                            // 保存到数据库
                            historyViewModel.insertHistory(historyItem)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun shareGifConverted(gifFile: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", gifFile)
        shareGifDirectly(uri)
    }

    private fun shareGifDirectly(gifUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            action = Intent.ACTION_SEND
            type = "image/gif"
            putExtra(Intent.EXTRA_STREAM, gifUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(
            Intent.createChooser(shareIntent, "分享表情包"),
            SHARE_REQUEST_CODE
        )
    }

    private fun convertBitmapToGif(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        maxSize: Int,
        quality: Int,
        compressionLevel: Int
    ): File {
        val file = File(context.cacheDir, fileName)

        // 计算新的尺寸，保持宽高比
        val useScale = max(bitmap.width, bitmap.height) > maxSize
        val scaleFactor = if (bitmap.width > bitmap.height) {
            maxSize.toFloat() / bitmap.width
        } else {
            maxSize.toFloat() / bitmap.height
        }
        val newWidth = if (useScale) (bitmap.width * scaleFactor).toInt() else bitmap.width
        val newHeight = if (useScale) (bitmap.height * scaleFactor).toInt() else bitmap.height

        // 调整Bitmap尺寸
        val resizedBitmap = if (useScale) {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else bitmap

        ByteArrayOutputStream().use { byteArrayOutputStream ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            FileOutputStream(file).use { fileOutputStream ->
                fileOutputStream.write(byteArrayOutputStream.toByteArray())
            }
        }

        // 如果不是原始bitmap，释放资源
        if (resizedBitmap !== bitmap) {
            resizedBitmap.recycle()
        }

        return file
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
} 