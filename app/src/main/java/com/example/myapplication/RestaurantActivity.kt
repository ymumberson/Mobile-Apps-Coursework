package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

class RestaurantActivity : AppCompatActivity() {
    val mAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    var isGuest = false
    lateinit var restaurantID: String
    lateinit var restaurant: RestaurantHelperClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant)

        //RestaurantID is passed in so we know which restaurant to display
        restaurantID = intent.getStringExtra("restaurantID").toString()

        //Attaches the toolbar to the activity
        val myToolbar = findViewById<Toolbar>(R.id.restaurantToolbar)
        setSupportActionBar(myToolbar)

        //Closes the activity if the user logs out
        mAuth.addAuthStateListener {
            if (mAuth.currentUser == null) {
                this.finish()
            }
        }

        //Checks if current user is a guest
        if (mAuth.currentUser!!.isAnonymous) {
            isGuest = true
        }

        loadRestaurant()
        loadReviews()

        //Hides option to create a review if user is a guest,
        //otherwise the button opens the create review activity
        val createReviewBtn = findViewById<Button>(R.id.restaurantLeaveReviewButton)
        if (isGuest) {
            createReviewBtn.isVisible = false
        } else {
            createReviewBtn.setOnClickListener {
                run {
                    val intent = Intent(this, CreateReviewActivity::class.java)

                    intent.putExtra("restaurantID", restaurantID)
                    intent.putExtra("restaurantImage", restaurant.image)
                    intent.putExtra("restaurantName", restaurant.name)
                    intent.putExtra("restaurantLocation", restaurant.location)

                    Log.d(LOG_TAG,"Launching Create Review Activity!")
                    startActivity(intent)
                }
            }
        }

        //Shows button to edit restaurant if user is the restaurant owner
        val editRestaurantButton = findViewById<Button>(R.id.restaurantEditRestaurantButton)
        editRestaurantButton.setOnClickListener {
            val intent = Intent(this, CreateRestaurantActivity::class.java)
            intent.putExtra("userID",restaurant.ownerID)
            intent.putExtra("restaurant",restaurant)
            startActivity(intent)
        }

        //Open maps button opens the maps activity and passes in the current restaurant
        val openMapButton = findViewById<Button>(R.id.restaurantSeeLocationOnMapButton)
        openMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("restaurant",restaurant)
            startActivity(intent)
        }
    }

    /**
     * On resume, refreshes data
     */
    override fun onResume() {
        super.onResume()
        loadRestaurant()
        loadReviews()
    }

    /**
     * Inflates the toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Handles toolbar actions
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //Signs the user out
            R.id.action_logout -> {
                mAuth.signOut()
            }

            //Launches the home page activity
            R.id.action_home -> {
                val intent = Intent(this, MainMenuActivity::class.java)
                startActivity(intent)
            }

            //Launches the profile activity
            R.id.action_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Loads the restaurant for the restaurantID passed into the activity
     */
    private fun loadRestaurant() {
        Log.d(LOG_TAG, "Populating data")
        val picture = findViewById<ImageView>(R.id.restaurantImage)
        val name = findViewById<TextView>(R.id.restaurantName)
        val rating = findViewById<RatingBar>(R.id.restaurantRatingBar)
        val description = findViewById<TextView>(R.id.restaurantDescription)

        val restaurantTable = database.getReference("restaurants").ref
        restaurantTable.orderByChild("restaurantID").equalTo(restaurantID).get().addOnSuccessListener {
            Log.d(LOG_TAG, "it= $it")
            val rest = it.children.elementAt(0).getValue(RestaurantHelperClass::class.java)
            if (rest != null) {
                restaurant = rest
                Log.d(LOG_TAG, "Restaurant found! $rest")
                Picasso.get().load(rest.image).into(picture)
                name.text = rest.name
                rating.rating = rest.averageRating
                description.text = rest.description

                if (!isGuest) {
                    checkIfUserIsRestaurantOwner()
                }
            } else {
                Log.d(LOG_TAG, "Restaurant not found!")
            }
        }.addOnFailureListener {
            Log.d(LOG_TAG, "Restaurant query failed for $restaurantID")
        }
    }

    /**
     * Loads all reviews for the current restaurant
     */
    private fun loadReviews() {
        val list = ArrayList<ReviewHelperClass>()
        val reviewTableRef = database.getReference("reviews").ref
        reviewTableRef.orderByChild("restaurantID").equalTo(restaurantID).get().addOnSuccessListener {
            for (postSnapshot in it.children) {
                val review = postSnapshot.getValue(ReviewHelperClass::class.java)
                if (review != null) {
                    list.add(review)
                    Log.d("PRINTING_REVIEWS",review.toString())
                }
            }
            val recyclerView = findViewById<View>(R.id.restaurantRecyclerView) as RecyclerView
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = layoutManager
            val reviewAdapter = ReviewAdapter(list)
            recyclerView.adapter = reviewAdapter
        }
    }

    /**
     * Checks if the user is the restaurant owner to give permission to edit the restaurant
     */
    private fun checkIfUserIsRestaurantOwner(){
        val editRestaurantButton = findViewById<Button>(R.id.restaurantEditRestaurantButton)
        val leaveReviewButton = findViewById<Button>(R.id.restaurantLeaveReviewButton)

        val users = database.getReference("userTable")
        val currentUserEmail = mAuth.currentUser?.email.toString()
        users.orderByChild("email").equalTo(currentUserEmail).get().addOnSuccessListener {
            Log.d(FullReviewActivity.LOG_TAG, "UserID success")
            val value = it.children.elementAt(0).getValue<UserHelperClass>()
            editRestaurantButton.isVisible = (value != null && restaurant.ownerID != null && value.userID == restaurant.ownerID)
            leaveReviewButton.isVisible = !editRestaurantButton.isVisible
        }.addOnFailureListener {
            Log.d(FullReviewActivity.LOG_TAG, "UserID failure")
            editRestaurantButton.isVisible = false
        }
    }

    companion object {
        const val LOG_TAG = "Restaurant Page"
    }
}