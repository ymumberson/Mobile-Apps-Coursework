package com.example.myapplication

import java.io.Serializable

/**
 * Data class to model reviews
 */
data class ReviewHelperClass(
    var reviewID: String="",
    var userID: String="",
    var restaurantID: String="",
    var rating: Float=0f,
    var title: String="",
    var description: String="",
    var dateTime: String="",
    var foodPhoto: String=""
) : Serializable