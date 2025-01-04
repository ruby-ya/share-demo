package com.example.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.data.EmoticonItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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