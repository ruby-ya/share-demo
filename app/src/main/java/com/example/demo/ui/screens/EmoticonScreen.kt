package com.example.demo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.demo.data.EmoticonItem

@Composable
fun EmoticonScreen() {
    var selectedCategory by remember { mutableStateOf(0) }
    var categories by remember { mutableStateOf(listOf("热门", "搞笑", "动漫", "明星", "其他")) }
    var currentEmoticons by remember { mutableStateOf(listOf<EmoticonItem>()) }
    
    Column {
        // 分类标签
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategory == index,
                    onClick = { selectedCategory = index },
                    text = { Text(category) }
                )
            }
        }
        
        // 表情包网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(currentEmoticons) { emoticon ->
                EmoticonItem(emoticon)
            }
        }
    }
}

@Composable
private fun EmoticonItem(emoticon: EmoticonItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        // TODO: 使用 Glide 或 Coil 加载网络图片
        Text(
            text = emoticon.title,
            modifier = Modifier.padding(8.dp)
        )
    }
} 