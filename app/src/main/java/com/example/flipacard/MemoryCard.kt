package com.example.flipacard

import android.graphics.Bitmap

data class MemoryCard(
    val id: Int,
    val image: Bitmap,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)
