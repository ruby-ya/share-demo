package com.example.demo.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.demo.ui.icons.CustomIcons
import com.example.demo.utils.generatePreview
import com.example.demo.utils.formatFileSize

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class,
    ExperimentalGlideComposeApi::class
)
@Composable
fun ShareImageScreen(
    imageUri: Uri,
    onPickImage: () -> Unit,
    onShare: (Int, Int, Int) -> Unit,
    onNavigateUp: () -> Unit = {}
) {
    var quality by remember { mutableStateOf(80) }
    var maxSize by remember { mutableStateOf(1024) }
    var compressionLevel by remember { mutableStateOf(7) }
    
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf(0L) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }

    val context = LocalContext.current

    var showImageDetail by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 添加 coroutineScope
    val coroutineScope = rememberCoroutineScope()

    var estimatedSize by remember { mutableStateOf(0f) }

    // 加载图片信息
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                // 先获取图片尺寸
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true  // 只解码图片尺寸，不加载图片
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                imageWidth = options.outWidth
                imageHeight = options.outHeight
            }

            // 再获取其他文件信息
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val cursor = context.contentResolver.query(imageUri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(MediaStore.Images.Media.SIZE)
                        val mimeTypeIndex = it.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex) ?: ""
                        }
                        if (sizeIndex != -1) {
                            fileSize = it.getLong(sizeIndex)
                        }
                    }
                }

                // 如果从 MediaStore 获取不到文件名，尝试从 Uri 路径获取
                if (fileName.isEmpty()) {
                    fileName = imageUri.lastPathSegment?.substringAfterLast('/') ?: "unknown.gif"
                }

                // 如果从 MediaStore 获取不到文件大小，尝试从输入流获取
                if (fileSize == 0L) {
                    fileSize = inputStream.available().toLong()
                }
            }

            // 计算初始预估大小
            val originalSize = fileSize / 1024f // KB
            val sizeReduction = (100 - quality) / 100f
            val scaleReduction = if (max(imageWidth, imageHeight) > maxSize) {
                val scale = maxSize.toFloat() / max(imageWidth, imageHeight)
                1 - (scale * scale)
            } else 0f
            
            estimatedSize = originalSize * (1 - sizeReduction) * (1 - scaleReduction)
        }
    }

    // 当参数改变时更新预估大小
    LaunchedEffect(quality, maxSize) {
        val originalSize = fileSize / 1024f
        val sizeReduction = (100 - quality) / 100f
        val scaleReduction = if (max(imageWidth, imageHeight) > maxSize) {
            val scale = maxSize.toFloat() / max(imageWidth, imageHeight)
            1 - (scale * scale)
        } else 0f
        
        estimatedSize = originalSize * (1 - sizeReduction) * (1 - scaleReduction)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑表情包") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onPickImage) {
                        Icon(Icons.Default.Add, contentDescription = "选择新图片")
                    }
                }
            )
        },
        bottomBar = {
            // 底部按钮栏
            BottomAppBar(
                modifier = Modifier.height(80.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 预览按钮
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                previewBitmap = generatePreview(
                                    context,
                                    imageUri,
                                    quality,
                                    maxSize
                                )
                                showPreview = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Preview,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("预览效果")
                    }

                    // 分享按钮
                    Button(
                        onClick = { onShare(quality, maxSize, compressionLevel) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("分享")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 预览区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showImageDetail = true },
                contentAlignment = Alignment.Center
            ) {
                GlideImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // 图片信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    InfoRow("文件名", fileName)
                    InfoRow("尺寸", "${imageWidth}x${imageHeight}")
                    InfoRow("大小", formatFileSize(fileSize))
                }
            }

            // 编辑选项
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "编辑选项",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 尺寸滑块
                    SliderOption(
                        title = "最大尺寸",
                        value = maxSize.toFloat(),
                        onValueChange = { maxSize = it.toInt() },
                        valueRange = 256f..2048f,
                        icon = CustomIcons.PhotoSizeSelectLarge,
                        suffix = "px",
                        description = "调整图片的最大边长"
                    )

                    // 压缩质量滑块
                    SliderOption(
                        title = "压缩质量",
                        value = quality.toFloat(),
                        onValueChange = { quality = it.toInt() },
                        valueRange = 20f..100f,
                        icon = CustomIcons.CompressedSize,
                        suffix = "%",
                        description = "降低质量可以减小文件大小"
                    )

                    // 预估大小显示
                    Text(
                        text = "预估大小: ${formatFileSize(estimatedSize)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }

    // 图片详情对话框
    if (showImageDetail) {
        Dialog(
            onDismissRequest = { showImageDetail = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showImageDetail = false }
            ) {
                GlideImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // 图片信息覆盖层
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = fileName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "尺寸: ${imageWidth}x${imageHeight}",
                        color = Color.White
                    )
                    Text(
                        text = "大小: ${formatFileSize(fileSize)}",
                        color = Color.White
                    )
                }
            }
        }
    }

    // 压缩预览对话框
    if (showPreview && previewBitmap != null) {
        Dialog(
            onDismissRequest = { 
                showPreview = false
                previewBitmap?.recycle()
                previewBitmap = null
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "压缩预览",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 原图
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("原图", fontWeight = FontWeight.Medium)
                            GlideImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(formatFileSize(fileSize))
                        }
                        
                        // 压缩后
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("压缩后", fontWeight = FontWeight.Medium)
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(formatFileSize(estimatedSize))
                        }
                    }
                    
                    Button(
                        onClick = { 
                            showPreview = false
                            previewBitmap?.recycle()
                            previewBitmap = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("关闭预览")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SliderOption(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: ImageVector,
    suffix: String,
    description: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(title)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${value.toInt()}$suffix",
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
} 