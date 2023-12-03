package com.example.andoridproject

data class Filters(
    val ascendingOrder: Boolean,
    val descendingOrder: Boolean,
    val excludeSoldOut: Boolean,
    val minPrice: Int,
    val maxPrice: Int
)