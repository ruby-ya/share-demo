package com.example.demo.data

data class EmoticonItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val category: String,
    val isFavorite: Boolean = false
) 