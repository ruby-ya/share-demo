package com.example.demo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intent = intent
        if (Intent.ACTION_SEND == intent.action && intent.type?.startsWith("image/") == true) {
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (imageUri != null) {
                setContent {
                    ShareImageScreen(imageUri)
                }
            }
        } else {
            setContent {
                AlertDialogContent()
            }
        }
    }


}

@Composable
fun AlertDialogContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("仅分享可用")
    }
}

@Composable
fun ShareImageScreen(imageUri: Uri) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf(0L) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
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
                        if (mimeTypeIndex != -1) {
                            fileType = it.getString(mimeTypeIndex) ?: ""
                        }
                    }
                }
                bitmap = BitmapFactory.decodeStream(inputStream)?.also { bmp ->
                    imageWidth = bmp.width
                    imageHeight = bmp.height
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "文件名: $fileName", textAlign = TextAlign.Left, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
//                        .aspectRatio(imageWidth.toFloat() / imageHeight) // 保持宽高比
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "图片类型: $fileType, 图片大小: ${fileSize / 1024} KB, 图片尺寸: ${imageWidth}x${imageHeight}")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    bitmap?.let { bmp ->
                        val gifFile = convertBitmapToGif(context, bmp, fileName)
                        shareGif(context, gifFile)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "转为GIF并分享")
            }
        }
    }
}

fun convertBitmapToGif(context: android.content.Context, bitmap: Bitmap, fileName: String): File {
    val filePrefix = if (fileName.lastIndexOf(".") > 0) {
        fileName.substring(0, fileName.lastIndexOf("."))
    } else fileName
    val file = File(context.cacheDir, "${filePrefix}.gif")
    ByteArrayOutputStream().use { byteArrayOutputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        FileOutputStream(file).use { fileOutputStream ->
            fileOutputStream.write(byteArrayOutputStream.toByteArray())
        }
    }
    return file
}

fun shareGif(context: android.content.Context, gifFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", gifFile)
    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/gif"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "分享GIF"))
}