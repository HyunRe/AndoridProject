package com.example.andoridproject

data class User(
    var userId : String,
    var userNickname: String,
    var userEmail : String,
    var userName : String,
    val birthDay: String,
    var password : String,
    var imageUrl : String
){
    constructor(): this("","","","","", "","")

    constructor(userId: String, userNickname: String) : this(
        userId = userId,
        userNickname = userNickname,
        userEmail = "",
        userName = "",
        birthDay = "",
        password = "",
        imageUrl = ""
    )
}