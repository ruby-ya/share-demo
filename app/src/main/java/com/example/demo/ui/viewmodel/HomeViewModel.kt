package com.example.demo.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.demo.data.EmoticonItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel() : ViewModel() {
    private val _hotEmoticons = MutableStateFlow<List<EmoticonItem>>(emptyList())
    val hotEmoticons = _hotEmoticons.asStateFlow()

    private val _recentEmoticons = MutableStateFlow<List<EmoticonItem>>(emptyList())
    val recentEmoticons = _recentEmoticons.asStateFlow()

    init {
        loadHotEmoticons()
        loadRecentEmoticons()
    }

    private fun loadHotEmoticons() {

    }

    private fun loadRecentEmoticons() {

    }
} 