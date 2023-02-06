package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class FullReviewActivity : AppCompatActivity() {
    /**
     * Reference to firebase authentication
     */
    val mAuth = FirebaseAuth.getInstance()

    /**
     * Reference to firebase database
     */
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")

    /**
     * UserID for the current review
     */
    var reviewUserID: String? = null

    /**
     * Reference to the restaurant attached to the review, used for creating intents.
     */
    var restaurant: RestaurantHelperClass? = null

    /**
     * Stores if the user is a guest account or not
     */
    var isGuest = false

    /**
     * ReviewID of the current review
     */
    lateinit var reviewID: String

    /**
     * Reference to the current review
     */
    lateinit var review: ReviewHelperClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_review)

        //Gets the reviewID used to fetch the review from the database
        reviewID = intent.getStringExtra("reviewID").toString()

        //Sets the toolbar of the activity
        val myToolbar = findViewById<Toolbar>(R.id.fullReviewToolbar)
        setSupportActionBar(myToolbar)
        myToolbar.setNavigationOnClickListener {
            this.finish()
        }

        //Setting edit button to open an activity to allow the review owner to edit the review
        val editReviewButton = findViewById<Button>(R.id.fullReviewEditReviewButton)
        editReviewButton.setOnClickListener {
            val intent = Intent(this, CreateReviewActivity::class.java)
            intent.putExtra("review",review)
            startActivity(intent)
        }

        //Closes this activity if the user signs out
        mAuth.addAuthStateListener {
            if (mAuth.currentUser == null) {
                this.finish()
            }
        }

        //Checks if current user is using a guest account
        if (mAuth.currentUser!!.isAnonymous) {
            isGuest = true
        }

        loadReview()
    }

    /**
     * Inflates toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.full_review_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Handles actions for the toolbar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //Allows the user to share images from reviews
            R.id.action_share -> {
                if (restaurant != null) {

                    //Only tried to share image if the review has an image attached
                    val hasImage = !review.foodPhoto.isNullOrEmpty()
                    var uri: Uri? = null
                    if (hasImage) {
                        //Converts imageview to Uri by saving it locally
                        val bitmap = findViewById<ImageView>(R.id.fullReviewFoodImage).drawable.toBitmap()
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                        val path = MediaStore.Images.Media.insertImage(this.contentResolver, bitmap, "Title", null)
                        uri = Uri.parse(path.toString())
                    }

                    //Gets the review description
                    val revDesc = findViewById<TextView>(R.id.fullReviewDescription).text.toString()

                    //Gets the review map link
                    val latlong = restaurant!!.location.split(" ")
                    val mapUri = "http://maps.google.com/maps?daddr=${latlong[0].toDouble()},${latlong[1].toDouble()}"

                    //Creates intent to share
                    val sendIntent: Intent = Intent().apply {
                        if (hasImage) { //Checks if review has an image attached
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM,uri)
                        }
                        type = "image/png"
                        putExtra(Intent.EXTRA_TEXT,"$revDesc\n$mapUri")
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent,"Some text goes here"))
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Loads the review from the database and populates text and image fields etc
     */
    private fun loadReview() {
        val reviews = database.getReference("reviews")
        //Find review in table
        reviews.orderByChild("reviewID").equalTo(reviewID).get().addOnSuccessListener {
            //Load review
            val rev = it.children.elementAt(0).getValue<ReviewHelperClass>()
            if (rev != null) {
                Log.d(LOG_TAG, "fine loading review")
                //Populate text and image fields
                val title = findViewById<TextView>(R.id.fullReviewTitle)
                val desc = findViewById<TextView>(R.id.fullReviewDescription)
                val rating = findViewById<RatingBar>(R.id.fullReviewRatingBar)
                val foodImage = findViewById<ImageView>(R.id.fullReviewFoodImage)
                val dateTime = findViewById<TextView>(R.id.fullReviewDateTime)

                review = rev
                title.text = rev.title
                desc.text = rev.description
                rating.rating = rev.rating
                dateTime.text = rev.dateTime
                reviewUserID = rev.userID

                //Only attempt to update imageView if the review contains an image
                if (!rev.foodPhoto.isNullOrEmpty()) {
                    Picasso.get().load(rev.foodPhoto).into(foodImage)
                } else {
                    foodImage.isVisible = false
                }

                //Check if the user is the author owner
                if (!isGuest) {
                    checkIfUserIsReviewAuthor()
                }

                loadRestaurant(rev.restaurantID)
            } else {
                Log.d(LOG_TAG, "Review was null for reviewID: $reviewID")
            }
        }.addOnFailureListener {
            Log.d(LOG_TAG, "Unable to load review")
        }
    }

    /**
     * Loads the restaurant attached to the review from the database
     */
    private fun loadRestaurant(restaurantID: String) {
        Log.d(RestaurantActivity.LOG_TAG, "Populating data")

        val restaurantTable = database.getReference("restaurants").ref
        restaurantTable.orderByChild("restaurantID").equalTo(restaurantID).get().addOnSuccessListener {
            Log.d(RestaurantActivity.LOG_TAG, "it= $it")
            val rest = it.children.elementAt(0).getValue(RestaurantHelperClass::class.java)
            if (rest != null) {
                restaurant = rest
                val name = findViewById<TextView>(R.id.fullReviewRestaurantName)
                val image = findViewById<ImageView>(R.id.fullReviewRestaurantImage)
                val rating = findViewById<RatingBar>(R.id.fullReviewRestaurantRatingBar)
                val seeLocation = findViewById<Button>(R.id.fullReviewRestaurantSeeLocationOnMapButton)

                name.text = restaurant!!.name
                rating.rating = restaurant!!.averageRating
                Picasso.get().load(restaurant!!.image).into(image)
                seeLocation.setOnClickListener {
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra("restaurant",restaurant)
                    startActivity(intent)
                }
            } else {
                Log.d(RestaurantActivity.LOG_TAG, "Restaurant not found!")
            }
        }.addOnFailureListener {
            Log.d(RestaurantActivity.LOG_TAG, "Restaurant query failed for $restaurantID")
        }
    }

    /**
     * Checks if the current user is the review author and toggles the visibility
     * of the edit review button appropriately
     */
    private fun checkIfUserIsReviewAuthor(){
        val editReviewButton = findViewById<Button>(R.id.fullReviewEditReviewButton)
        val users = database.getReference("userTable")
        val currentUserEmail = mAuth.currentUser?.email.toString()
        users.orderByChild("email").equalTo(currentUserEmail).get().addOnSuccessListener {
            Log.d(LOG_TAG, "UserID success")
            val value = it.children.elementAt(0).getValue<UserHelperClass>()
            editReviewButton.isVisible = (value != null && reviewUserID != null && value.userID == reviewUserID)
        }.addOnFailureListener {
            Log.d(LOG_TAG, "UserID failure")
            editReviewButton.isVisible = false
        }
    }

    companion object {
        const val LOG_TAG = "Full Review"
    }
}