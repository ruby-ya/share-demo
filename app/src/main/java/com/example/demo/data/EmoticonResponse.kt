package com.example.demo.data

data class EmoticonResponse(
    val code: Int,
    val message: String,
    val data: List<EmoticonItem>
)