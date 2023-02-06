package com.example.myapplication

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

/**
 * Used by any recyclerviews displaying lists of restaurants
 */
class RestaurantAdapter (private val restaurantArrayList: MutableList<RestaurantHelperClass>) : RecyclerView.Adapter<RestaurantAdapter.ViewHolder>() {

    /*
     * Inflate our views using the layout defined in row_layout.xml
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.restaurant_row_layout, parent, false)

        return ViewHolder(v)
    }

    /*
     * Bind the data to the child views of the ViewHolder
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = restaurantArrayList[position]

        holder.restaurantName.text = info.name
        holder.restaurantCuisine.text = info.cuisine
        holder.restaurantRating.rating = info.averageRating
        Picasso.get().load(info.image).into(holder.restaurantImage)
        holder.restaurantID = info.restaurantID
    }

    /*
     * Get the maximum size of the
     */
    override fun getItemCount(): Int {
        return restaurantArrayList.size
    }

    /*
     * The parent class that handles layout inflation and child view use
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        var restaurantName = itemView.findViewById<View>(R.id.myRestaurantTitle) as TextView
        var restaurantCuisine = itemView.findViewById<View>(R.id.myRestaurantCuisine) as TextView
        var restaurantRating = itemView.findViewById<View>(R.id.myRestaurantRatingBar) as RatingBar
        var restaurantImage = itemView.findViewById<View>(R.id.myRestaurantImage) as ImageView
        var seeFullBtn = itemView.findViewById<View>(R.id.myRestaurantSeeFullButton) as Button
        var restaurantID: String? = null

        init {
            itemView.setOnClickListener(this)
            //Opens restaurant activity for restaurant clicked
            seeFullBtn.setOnClickListener {
                if (restaurantID != null) {
                    val intent = Intent(it.context, RestaurantActivity::class.java)
                    intent.putExtra("restaurantID",restaurantID)
                    it.context.startActivity(intent)
                }
            }
        }

        /**
         * Onclick does nothing
         */
        override fun onClick(v: View) {

        }
    }
}