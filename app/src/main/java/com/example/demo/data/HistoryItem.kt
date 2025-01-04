package com.example.demo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val fileName: String,
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val timestamp: Date,
    val compressedSize: Long? = null
) 