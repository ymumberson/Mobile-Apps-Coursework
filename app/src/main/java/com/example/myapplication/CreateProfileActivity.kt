package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CreateProfileActivity : AppCompatActivity() {
    /**
     * Reference to firebase authentication
     */
    val mAuth = FirebaseAuth.getInstance()

    /**
     * Reference to firebase database
     */
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_profile)

        //Assigns cancel button to close the activity
        val cancelButton = findViewById<Button>(R.id.createProfileCancelButton)
        cancelButton.setOnClickListener {
            run { this.finish() }
        }

        //Assign submit button to create a new user account with attached authentication
        val submitButton = findViewById<Button>(R.id.createProfileSubmitButton)
        submitButton.setOnClickListener {
            run {
                val email = findViewById<TextView>(R.id.createProfileEmail).text.toString()
                val password = findViewById<TextView>(R.id.createProfilePassword).text.toString()
                val confirmPassword = findViewById<TextView>(R.id.createProfileConfirmPassword).text.toString()
                val forename = findViewById<TextView>(R.id.createProfileForename).text
                val surname = findViewById<TextView>(R.id.createProfileSurname).text

                //Checks if email and password are valid
                if (validateEmail(email) &&
                    validatePassword(password,confirmPassword)) {
                        if (hasEnteredName(forename, surname)) {
                            createProfile(email,password, "$forename $surname")
                        } else {
                            Snackbar.make(it,getString(R.string.noNameEntered),Snackbar.LENGTH_LONG).show()
                        }
                } else {
                    Snackbar.make(it,getString(R.string.invalidEmailOrPassword),Snackbar.LENGTH_LONG).show()
                }
            }
        }


    }

    /**
     * Checks if given email is valid
     */
    private fun validateEmail(email: String) : Boolean {
        if (!email.isNullOrEmpty()) {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
        return false
    }

    private fun hasEnteredName(forename: CharSequence, surname: CharSequence): Boolean {
        return !forename.isNullOrEmpty() && !surname.isNotEmpty()
    }


    /**
     * Checks two passwords are valid and match
     */
    private fun validatePassword(password: String, confirmPassword: String) : Boolean {
        if (validatePassword(password) && !confirmPassword.isNullOrEmpty()) {
            return password == confirmPassword
        }
        return false
    }

    /**
     * Checks if a given password is valid
     */
    private fun validatePassword(password: String) : Boolean {
        if (!password.isNullOrEmpty()) {
            var foundUpperChar = false
            var foundNum = false
            for (c in password) {
                if (c.isUpperCase()) {
                    foundUpperChar = true
                } else if (c.digitToIntOrNull() != null) {
                    foundNum = true
                }
            }
            return foundUpperChar && foundNum
        }
        return false
    }

    /**
     * Creates a new user account with attached authenticationCreates
     */
    private fun createProfile(email: String, password: String, name: String) {
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) { //If authentication creation is successful
                val users = database.getReference("userTable").push()
                val defaultProfilePicURL = "https://firebasestorage.googleapis.com/v0/b/mobileappscoursework-b20e5.appspot.com/o/glossyBunny3.PNG?alt=media&token=370fffb0-75b9-4627-9a12-4c7ddbf429f6"
                val defaultBio = "This is where the user's bio will go -> This is a default bio assigned when profiles are created."
                val newProfile = UserHelperClass(userID=users.key.toString(),email=email,name=name,bio=defaultBio, profilePicture=defaultProfilePicURL)
                users.setValue(newProfile).addOnCompleteListener { task2 ->
                    if (task2.isSuccessful) { //If profile creation is successful
                        Toast.makeText(this,"Successfully created profile.", Toast.LENGTH_LONG).show()
                        this.finish()
                    } else {
                        Toast.makeText(this,"User authentication created, but profile creation failed", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this,"Failed to create profile: Email already in use.", Toast.LENGTH_LONG).show()
            }
        }
    }
}