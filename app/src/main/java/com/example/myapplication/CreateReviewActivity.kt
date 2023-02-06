package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CreateReviewActivity : AppCompatActivity() {
    val mAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    private val storageRef = FirebaseStorage.getInstance()
    var addedFoodImage = false
    var hasLoadedReview = false
    var loadedReview: ReviewHelperClass? = null
    lateinit var restaurantID: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_review)

        //Gets the review object passed into the activity
        if (intent.extras != null) {
            val temp = intent.extras!!.get("review")
            if (temp != null) {
                val rev = temp as ReviewHelperClass
                loadedReview = rev
                restaurantID = rev.restaurantID
                loadReview()
                loadRestaurantData()
            }
        }

        //If a review was passed in, then populate the text and image fields
        if (loadedReview == null) {
            val restaurantImage = findViewById<ImageView>(R.id.createReviewRestaurantImage)
            val restaurantName = findViewById<TextView>(R.id.createReviewRestaurantName)
            val restaurantLocation = findViewById<TextView>(R.id.createReviewRestaurantLocation)

            restaurantID = intent.getStringExtra("restaurantID").toString()
            Picasso.get().load(intent.getStringExtra("restaurantImage")).into(restaurantImage)
            restaurantName.text = intent.getStringExtra("restaurantName").toString()
            restaurantLocation.text = intent.getStringExtra("restaurantLocation").toString()
        }

        //Attaches the toolbar to the activity
        val myToolbar = findViewById<Toolbar>(R.id.createReviewToolbar)
        setSupportActionBar(myToolbar)
        myToolbar.setNavigationOnClickListener {
            run {
                this.finish()
            }
        }

        //Creates the review and uploads it to the database
        val createReviewBtn = findViewById<Button>(R.id.createReviewCreateReviewButton)
        createReviewBtn.setOnClickListener {
            if (mAuth.currentUser != null) {
                createReview()
            }
        }

        //Allows the rating bar value to be changed
        val ratingBar = findViewById<RatingBar>(R.id.createReviewRatingBar)
        ratingBar.setOnRatingBarChangeListener { ratingBar, fl, b ->
            run {
                ratingBar.rating = fl
            }
        }

        //Checks for camera permission and then requests an image from the user
        val foodImageButton = findViewById<Button>(R.id.createReviewAddImageButton)
        foodImageButton.setOnClickListener { view ->
            run {
                foodImageButton.isEnabled = false
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(takePhotoIntent, CAMERA_REQUEST_CODE)
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),CAMERA_PERMISSION_CODE)
                }
            }
        }
    }

    /**
     * Deals with camera permission requests
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePhotoIntent, CAMERA_REQUEST_CODE)
            }
        }
    }

    /**
     * Processes image returned from camera request and sets it as the food image
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                addedFoodImage = true
                val uploadedFoodPic = findViewById<ImageView>(R.id.createReviewFoodImage)
                val pic: Bitmap = data!!.extras!!.get("data") as Bitmap
                uploadedFoodPic.setImageBitmap(pic)
            }
        }
    }

    /**
     * Loads the restaurant associated with the current review
     */
    private fun loadRestaurantData() {
        if (loadedReview != null) {
            val restaurantTable = database.getReference("restaurants").ref
            restaurantTable.orderByChild("restaurantID").equalTo(restaurantID).get().addOnSuccessListener {
                val rest = it.children.elementAt(0).getValue(RestaurantHelperClass::class.java)

                if (rest != null) {
                    val restaurantImage = findViewById<ImageView>(R.id.createReviewRestaurantImage)
                    val restaurantName = findViewById<TextView>(R.id.createReviewRestaurantName)
                    val restaurantLocation = findViewById<TextView>(R.id.createReviewRestaurantLocation)

                    Picasso.get().load(rest.image).into(restaurantImage)
                    restaurantName.text = rest.name
                    restaurantLocation.text = rest.location
                }
            }
        }
    }

    /**
     * Loads the review if editing a review instead of creating a new review
     */
    private fun loadReview() {
        if (loadedReview != null) {
            val rating = findViewById<RatingBar>(R.id.createReviewRatingBar)
            val title = findViewById<TextView>(R.id.createReviewReviewTitle)
            val description = findViewById<TextView>(R.id.createReviewDescription)
            val picture = findViewById<ImageView>(R.id.createReviewFoodImage)

            title.text = loadedReview!!.title
            rating.rating = loadedReview!!.rating
            description.text = loadedReview!!.description
            if (!loadedReview!!.foodPhoto.isNullOrEmpty()) {
                Picasso.get().load(loadedReview!!.foodPhoto).into(picture)
            }
            hasLoadedReview = true
        }
    }

    /**
     * Uploads new image to the database
     */
    private fun uploadImage(reviewPathRef: DatabaseReference, review: ReviewHelperClass) {
        if (addedFoodImage) {
            val foodPicView = findViewById<ImageView>(R.id.createReviewFoodImage)
            val pic = foodPicView.drawable.toBitmap()
            val stream = ByteArrayOutputStream()
            pic.compress(Bitmap.CompressFormat.PNG, 90, stream)
            val img = stream.toByteArray()

            val storageLocation = storageRef.getReference(review.reviewID)
            storageLocation.putBytes(img).addOnSuccessListener {
                storageLocation.downloadUrl.addOnSuccessListener {
                    val picURL = it.toString()
                    review.foodPhoto = picURL
                    getUserID(reviewPathRef, review)
                }
            }
        } else if (hasLoadedReview) {
            review.foodPhoto = loadedReview!!.foodPhoto
            getUserID(reviewPathRef,review)
        }else {
            getUserID(reviewPathRef,review)
        }
    }

    /**
     * Gets the userID associated with the currently logged in email
     */
    private fun getUserID(reviewPathRef: DatabaseReference, review: ReviewHelperClass){
        val users = database.getReference("userTable")
        val currentUserEmail = mAuth.currentUser?.email.toString()
        users.orderByChild("email").equalTo(currentUserEmail).get().addOnSuccessListener {
            Log.d("Creating review", "UserID success")
            val value = it.children.elementAt(0).getValue<UserHelperClass>()
            if (value != null) {
                review.userID = value.userID
                uploadReview(reviewPathRef,review)
            }
        }.addOnFailureListener {
            Log.d("Creating review", "UserID failure")
            uploadReview(reviewPathRef,review)
        }
    }

    /**
     * Creates the review object and passes it to another function to be saved to the database
     */
    private fun createReview() {
        var reviewID = ""
        val reviewRef: DatabaseReference

        if (loadedReview != null) {
            reviewID = loadedReview!!.reviewID
            reviewRef = database.getReference("reviews").child(reviewID).ref
        } else {
            reviewRef = database.getReference("reviews").push()
            reviewID = reviewRef.key.toString()
        }

        val rating = findViewById<RatingBar>(R.id.createReviewRatingBar).rating
        val title = findViewById<TextView>(R.id.createReviewReviewTitle).text.toString()
        val description = findViewById<TextView>(R.id.createReviewDescription).text.toString()
        val dateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)).toString()
        } else { //Assign dateTime to empty if SDK version is too low
            ""
        }

        val review = ReviewHelperClass(reviewID,"",restaurantID,rating,title,description,dateTime,"")

        if (isValidReview(review)) {
            uploadImage(reviewRef,review)
        } else {
            Log.d("Creating Review", "Review is invalid, make sure you've written a title and a description")
            Toast.makeText(this,getString(R.string.invalidReviewMessage), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Checks if the review contains a title and a description
     */
    private fun isValidReview(rev: ReviewHelperClass): Boolean {
        return !rev.title.isNullOrEmpty() && !rev.description.isNullOrEmpty()
    }

    /**
     * Uploads the review object to the database
     */
    private fun uploadReview(reviewPathRef: DatabaseReference, review: ReviewHelperClass) {
        reviewPathRef.setValue(review).addOnCompleteListener { task ->
            if (task.isSuccessful) {
//                Toast.makeText(this,"Successfully created review.", Toast.LENGTH_LONG).show()
                calculateNewAverageRating() //Calls for average rating to be updates if review was successfully uploaded
                this.finish()
                val intent = Intent(this, RestaurantActivity::class.java)
                intent.putExtra("restaurantID",restaurantID)
                startActivity(intent)
            } else {
                Toast.makeText(this,"Failed to create review.", Toast.LENGTH_LONG).show()
            }
        }
        val foodImageButton = findViewById<Button>(R.id.createReviewAddImageButton)
        foodImageButton.isEnabled = true
    }

    /**
     * Updates the average review for a restaurant when uploading a new review
     */
    private fun updateAverageRating(newAvgRating: Float) {
        val restaurantTable = database.getReference("restaurants").ref
        restaurantTable.child(restaurantID).child("averageRating").setValue(newAvgRating).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("Updating Average Rating", "Successful")
            } else {
                Log.d("Updating Average Rating", "Unsuccessful")
            }
        }
    }

    /**
     * Calculates new average rating
     */
    private fun calculateNewAverageRating() {
        val reviewTableRef = database.getReference("reviews").ref
        reviewTableRef.orderByChild("restaurantID").equalTo(restaurantID).get().addOnSuccessListener {
            var numRevs = 0f
            var sum = 0f
            for (postSnapshot in it.children) {
                val review = postSnapshot.getValue(ReviewHelperClass::class.java)
                if (review != null) {
                    numRevs += 1
                    sum += review.rating
                }
            }
            val reviewRating = findViewById<RatingBar>(R.id.createReviewRatingBar).rating
            sum += reviewRating
            numRevs += 1
            val newAvgRating = sum/numRevs
            updateAverageRating(newAvgRating)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
    }
}