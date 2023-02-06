package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

class CreateRestaurantActivity : AppCompatActivity() {
    private val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    private val storageRef = FirebaseStorage.getInstance()
    private var hasUploadedImage = false
    private var hasLoadedRestaurant = false
    private var rest: RestaurantHelperClass? = null
    private lateinit var userID: String

    /**
     * Handles results from intents.
     * Handles the returned location from the maps activity.
     */
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("Getting location","Location returned -> ${result.data}")
                val intent = result.data
                val msgExtras = intent?.extras
                if (msgExtras != null) {
                    Log.d("Getting location","1")
                    val lat = msgExtras.getString("latitude")
                    val long = msgExtras.getString("longitude")
                    Log.d("Getting location","$lat $long")

                    //Handles if result returned is latitude and longitude
                    if (!lat.isNullOrEmpty() && !long.isNullOrEmpty()) {
                        val locationText = findViewById<TextView>(R.id.createRestaurantLocation)
                        locationText.text = "$lat $long"
                        Log.d("Getting location","Setting location")
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_restaurant)

        //UserID is passed in to be used as restaurant owner ID
        userID = intent.getStringExtra("userID").toString()

        //If editing an existing restaurant then a restaurant object is passed in
        if (intent.extras != null) {
            val temp = intent.extras!!.get("restaurant")
            if (temp != null) {
                val restaurant = temp as RestaurantHelperClass
                rest = restaurant
                loadRestaurant()
            }
        }

        //Attaches the toolbar to the activity
        val myToolbar = findViewById<Toolbar>(R.id.createRestaurantToolbar)
        setSupportActionBar(myToolbar)

        //For creating the restaurant and uploading it to the database
        val createRestaurantButton = findViewById<Button>(R.id.createRestaurantCreateRestaurantButton)
        createRestaurantButton.setOnClickListener {
            createRestaurant()
        }

        //For uploading an image for the restaurant
        val uploadImageButton = findViewById<Button>(R.id.createRestaurantAddImageButton)
        uploadImageButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePhotoIntent, CAMERA_REQUEST_CODE)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
        }

        //For attaching a location to the restaurant
        val locationButton = findViewById<Button>(R.id.createRestaurantAddLocationButton)
        locationButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startForResult.launch(intent)
        }
    }

    /**
     * Handles asking for camera permission
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
     * Handles an image being returned from the camera
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                val uploadedPic = findViewById<ImageView>(R.id.createRestaurantImage)
                val pic: Bitmap = data!!.extras!!.get("data") as Bitmap
                uploadedPic.setImageBitmap(pic)
                hasUploadedImage = true
            }
        }
    }

    /**
     * Loads the restaurant data from the passed in restaurant object into the text and image fields
     */
    private fun loadRestaurant() {
        if (rest != null) {
            val name = findViewById<TextView>(R.id.createRestaurantName)
            val cuisine = findViewById<TextView>(R.id.createRestaurantCuisine)
            val description = findViewById<TextView>(R.id.createRestaurantDescription)
            val location = findViewById<TextView>(R.id.createRestaurantLocation)
            val picture = findViewById<ImageView>(R.id.createRestaurantImage)

            name.text = rest!!.name
            cuisine.text = rest!!.cuisine
            description.text = rest!!.description
            location.text = rest!!.location
            Picasso.get().load(rest!!.image).into(picture)

            val createButton = findViewById<Button>(R.id.createRestaurantCreateRestaurantButton)
            createButton.text = getString(R.string.submit_changes)

            hasLoadedRestaurant = true
        }
    }

    /**
     * Creates restaurant object from user entered data
     */
    private fun createRestaurant() {
        val restaurantRef: DatabaseReference
        var restaurantID = ""
        var rating = 0f

        //If editing a restaurant then use existing data
        if (rest != null) {
            restaurantID = rest!!.restaurantID
            restaurantRef = database.getReference("restaurants").child(restaurantID).ref
            rating = rest!!.averageRating
        } else { //Else use new values
            restaurantRef = database.getReference("restaurants").push()
            restaurantID = restaurantRef.key.toString()
        }

        val name = findViewById<TextView>(R.id.createRestaurantName).text.toString()
        val cuisine = findViewById<TextView>(R.id.createRestaurantCuisine).text.toString()
        val description = findViewById<TextView>(R.id.createRestaurantDescription).text.toString()
        val location = findViewById<TextView>(R.id.createRestaurantLocation).text.toString()
        val ownerID = userID

        //Checks entered data is valid
        if (validData(name,cuisine,description,location)) {
            var restaurant = RestaurantHelperClass(restaurantID,ownerID,name,cuisine,location,rating,description,"")
            uploadImage(restaurantRef,restaurant)
        } else {
            Log.d(LOG_TAG, "Invalid Data")
        }
    }

    /**
     * Checks data is valid.
     * No fields can be empty and an image must be uploaded
     */
    private fun validData(name:String, cuisine:String, description:String, location:String) : Boolean {
        return (!name.isNullOrEmpty() && !cuisine.isNullOrEmpty() && !description.isNullOrEmpty()
                && (hasUploadedImage || hasLoadedRestaurant) && !location.isNullOrEmpty())
    }

    /**
     * Uploads image to the database and gets a reference to the image's URL
     */
    private fun uploadImage(restaurantPathRef: DatabaseReference, restaurant: RestaurantHelperClass){
        if (hasUploadedImage) { //Only call if the user uploaded a new image
            val imgPicView = findViewById<ImageView>(R.id.createRestaurantImage)
            val pic = imgPicView.drawable.toBitmap()
            val stream = ByteArrayOutputStream()
            pic.compress(Bitmap.CompressFormat.PNG,90,stream)
            val img = stream.toByteArray()

            //Gets the URL for the uploaded image
            val storageLocation = storageRef.getReference(restaurant.restaurantID)
            storageLocation.putBytes(img).addOnSuccessListener {
                storageLocation.downloadUrl.addOnSuccessListener {
                    val picURL = it.toString()
                    restaurant.image = picURL
                    uploadRestaurant(restaurantPathRef,restaurant)
                }
            }
        //If the user is using the current existing image for the restaurant
        } else  if (hasLoadedRestaurant) {
            restaurant.image = rest!!.image
            uploadRestaurant(restaurantPathRef,restaurant)
        }
    }

    /**
     * Uploads restaurant into the database
     */
    private fun uploadRestaurant(restaurantPathRef: DatabaseReference, restaurant: RestaurantHelperClass) {
        restaurantPathRef.setValue(restaurant).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(LOG_TAG, "Created restaurant")
                this.finish()
            } else {
                Log.d(LOG_TAG, "Failed to create restaurant")
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
        private const val LOG_TAG = "Create Restaurant"
    }
}