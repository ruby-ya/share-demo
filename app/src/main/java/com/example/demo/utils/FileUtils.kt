package com.example.demo.utils

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.1f KB", size / 1024f)
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024f * 1024f))
        else -> String.format("%.1f GB", size / (1024f * 1024f * 1024f))
    }
}

fun formatFileSize(sizeInKB: Float): String {
    val sizeInBytes = (sizeInKB * 1024).toLong()
    return formatFileSize(sizeInBytes)
} 