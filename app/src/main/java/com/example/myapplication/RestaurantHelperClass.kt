package com.example.myapplication

import java.io.Serializable

/**
 * Data class for restaurants
 */
data class RestaurantHelperClass(
    val restaurantID: String="",
    val ownerID: String="",
    var name: String="",
    var cuisine: String="",
    var location: String="",
    var averageRating: Float=0f,
    var description: String="",
    var image: String=""
) : Serializable