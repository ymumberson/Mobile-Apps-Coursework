package com.example.myapplication

/**
 * Data class to model a user
 */
data class UserHelperClass(
    val userID: String = "",
    var email: String = "",
    var name: String = "",
    var bio: String = "",
    var profilePicture: String = ""
)