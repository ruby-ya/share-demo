package com.example.demo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.demo.R
import com.example.demo.data.EmoticonCategory
import com.example.demo.data.EmoticonItem
import com.example.demo.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val hotEmoticons by viewModel.hotEmoticons.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentEmoticons by viewModel.recentEmoticons.collectAsStateWithLifecycle(initialValue = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 热门推荐
        item {
            CategorySection(
                title = "热门推荐",
                emoticons = hotEmoticons
            )
        }

        // 最近使用
        item {
            CategorySection(
                title = "最近使用",
                emoticons = recentEmoticons
            )
        }

        // 分类浏览
        item {
            Text(
                text = "分类浏览",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            CategoryGrid()
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    emoticons: List<EmoticonItem>
) {
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
        ) {
            items(emoticons) { emoticon ->
                EmoticonCard(emoticon)
            }
        }
    }
}

@Composable
private fun CategoryGrid() {
    val categories = listOf(
        EmoticonCategory("搞笑", R.drawable.ic_funny),
        EmoticonCategory("动漫", R.drawable.ic_anime),
        EmoticonCategory("明星", R.drawable.ic_star),
        EmoticonCategory("萌宠", R.drawable.ic_pet),
        EmoticonCategory("游戏", R.drawable.ic_game),
        EmoticonCategory("文字", R.drawable.ic_text),
        EmoticonCategory("情感", R.drawable.ic_emotion),
        EmoticonCategory("其他", R.drawable.ic_more)
    )

    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .height(160.dp)
            .fillMaxWidth()
    ) {
        items(categories) { category ->
            CategoryCard(category)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun EmoticonCard(emoticon: EmoticonItem) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .aspectRatio(1f)
    ) {
        GlideImage(
            model = emoticon.imageUrl,
            contentDescription = emoticon.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun CategoryCard(category: EmoticonCategory) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(70.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = category.iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.name,
                fontSize = 14.sp
            )
        }
    }
} 