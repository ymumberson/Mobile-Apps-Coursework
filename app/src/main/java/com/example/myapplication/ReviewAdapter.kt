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
 * Used by any recyclerview showing lists of reviews
 */
class ReviewAdapter (private val reviewArrayList: MutableList<ReviewHelperClass>) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    /*
     * Inflate our views using the layout defined in row_layout.xml
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.row_layout, parent, false)

        return ViewHolder(v)
    }

    /*
     * Bind the data to the child views of the ViewHolder
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = reviewArrayList[position]

        holder.reviewID = info.reviewID
        holder.reviewDesc.text = info.description
        holder.reviewRating.rating = info.rating
        holder.reviewTitle.text = info.title

        if (info.foodPhoto.isNullOrEmpty()) {
            holder.reviewPic.isVisible = false
        } else {
            Picasso.get().load(info.foodPhoto).into(holder.reviewPic)
        }
    }

    /*
     * Get the maximum size of the
     */
    override fun getItemCount(): Int {
        return reviewArrayList.size
    }

    /*
     * The parent class that handles layout inflation and child view use
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var reviewPic = itemView.findViewById<View>(R.id.reviewPic) as ImageView
        var reviewDesc = itemView.findViewById<View>(R.id.reviewDescription) as TextView
        var reviewRating = itemView.findViewById<View>(R.id.reviewRatingBar) as RatingBar
        var reviewTitle = itemView.findViewById<View>(R.id.reviewTitle) as TextView
        var seeFullBtn = itemView.findViewById<View>(R.id.reviewSeeFullButton) as Button
        var reviewID: String? = null

        init {
            itemView.setOnClickListener(this)
            //Launches full review activity on button click
            seeFullBtn.setOnClickListener {
                val intent = Intent(it.context, FullReviewActivity::class.java)
                intent.putExtra("reviewID",reviewID)
                it.context.startActivity(intent)
            }
        }

        /**
         * Doesn't do anything on click
         */
        override fun onClick(v: View) {

        }
    }
}