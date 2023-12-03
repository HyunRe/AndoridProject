package com.example.andoridproject

data class ChatItem (
    val time: String,
    val senderId: String,
    val message: String,
    val key: Long
){
    constructor():this("","","", 0L)
}