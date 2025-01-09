package com.example.demo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.demo.data.AppDatabase
import com.example.demo.data.HistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "app_database"
    ).build()

    private val historyDao = database.historyDao()

    val historyItems = historyDao.getAllHistory()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch {
            historyDao.deleteAll()
        }
    }

    fun insertHistory(item: HistoryItem) {
        viewModelScope.launch {
            historyDao.insert(item)
        }
    }

    fun deleteItems(itemIds: Set<Long>) {
        viewModelScope.launch {
            historyDao.deleteByIds(itemIds.toList())
        }
    }
} 