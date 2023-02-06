package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {
    val mAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    private val storageRef = FirebaseStorage.getInstance()
    var currentUser: UserHelperClass? = null
    var updatedProfilePicture = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        //attaches toolbar to activity
        val myToolbar = findViewById<Toolbar>(R.id.editProfileToolbar)
        setSupportActionBar(myToolbar)
        myToolbar.setNavigationOnClickListener {
            run { this.finish() }
        }

        //Initialising variables to be used in code below
        val users = database.getReference("userTable")
        val currentUserEmail = mAuth.currentUser?.email.toString()
        val profilePicture = findViewById<ImageView>(R.id.editProfileProfilePicture)
        val forenameText = findViewById<EditText>(R.id.editProfileForename)
        val surnameText = findViewById<EditText>(R.id.editProfileSurname)
        val bioText = findViewById<EditText>(R.id.editProfileBio)

        //Populates activity with data for current user
        var currentUserRef = users
        if (currentUserEmail != null) {
            users.orderByChild("email").equalTo(currentUserEmail).get().addOnSuccessListener {
                currentUserRef = it.ref
                val value = it.children.elementAt(0).getValue<UserHelperClass>()
                Log.d("TAG", "Value is: $value")
                if (value != null) {
                    currentUser = value
                    val name = value.name.split(" ")
                    forenameText.setText(name[0])
                    surnameText.setText(name[1])
                    bioText.setText(value.bio)
                    Picasso.get().load(value.profilePicture).into(profilePicture)
                }
            }
        }

        //Updates the user's profile in the database
        val submitBtn = findViewById<Button>(R.id.editProfileSubmitButton)
        submitBtn.setOnClickListener {
            run {
                if (currentUser != null) {
                    val name = "${forenameText.text} ${surnameText.text}"
                    val bio = bioText.text.toString()
                    currentUser!!.name = name
                    currentUser!!.bio = bio

                    //If changed profile picture then upload it to the database
                    if (updatedProfilePicture) {
                        val pic = profilePicture.drawable.toBitmap()
                        val stream = ByteArrayOutputStream()
                        pic.compress(Bitmap.CompressFormat.PNG,90,stream)
                        val img = stream.toByteArray()

                        //Uploading profile picture to database
                        val storageLocation = storageRef.getReference(currentUser!!.userID)
                        storageLocation.putBytes(img).addOnSuccessListener {
                            Log.d("testing", "Uploaded image")

                            //Getting download URL for uploaded profile picture
                            storageLocation.downloadUrl.addOnSuccessListener {
                                val picURL = it.toString()
                                currentUser!!.profilePicture = picURL
                                Log.d("testing", "Uploaded Image at $picURL")

                                //Uploading profile to database
                                uploadProfile(currentUserRef)
                            }
                        }.addOnFailureListener {
                            Log.d("testing", "Failed to upload image")
                        }

                    //If not changed profile picture
                    } else {
                        //Uploading profile to database
                        uploadProfile(currentUserRef)
                    }
                }
            }
        }

        //Allows the user to close the activity
        val cancelBtn = findViewById<Button>(R.id.editProfileCancelButton)
        cancelBtn.setOnClickListener {
            run { this.finish() }
        }

        //Handles requesting an image from the user using the camera intent
        val changePictureBtn = findViewById<Button>(R.id.editProfileChangePicture)
        changePictureBtn.setOnClickListener {
            run {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(takePhotoIntent, CAMERA_REQUEST_CODE)
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
                }
            }
        }
    }

    /**
     * Handles requests for permissions.
     * Handles request for camera permission.
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
     * Handles results from intents.
     * Handles pictures returned from camera intents.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                val profilePic = findViewById<ImageView>(R.id.editProfileProfilePicture)
                val pic: Bitmap = data!!.extras!!.get("data") as Bitmap
                profilePic.setImageBitmap(pic)
                updatedProfilePicture = true
            }
        }
    }

    /**
     * Uploads profile picture to the database
     */
    private fun uploadProfile(currentUserRef: DatabaseReference) {
        currentUserRef.setValue(currentUser).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this,"Successfully updated profile.", Toast.LENGTH_LONG).show()
                Log.d("testing", "Successfully updated profile")
                this.finish()
            } else {
                Toast.makeText(this,"Failed to update profile.", Toast.LENGTH_LONG).show()
                Log.d("testing", "Failed to update profile")
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
    }
}