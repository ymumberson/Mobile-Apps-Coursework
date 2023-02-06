package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethod
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        val emailText = findViewById<TextInputEditText>(R.id.loginEmailTextInput)
        val passwordText = findViewById<TextInputEditText>(R.id.loginPasswordTextInput)
        val loginButton = findViewById<Button>(R.id.loginSubmitButton)

        //Authentication performed when pressing the login button
        loginButton.setOnClickListener { view ->
            val email = emailText.text
            val password = passwordText.text
            //Only check credentials if email and password contain data
            if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
                mAuth.signInWithEmailAndPassword(
                    email.toString(),
                    password.toString()
                )
                    .addOnCompleteListener(this) { task -> //If successful
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("loginAttempt", "Login successful")
                            val user = mAuth.currentUser
                            val intent = Intent(this, MainMenuActivity::class.java)
                            startActivity(intent)
                        } else { //If unsuccessful
                            // If sign in fails, display a message to the user.
                            Log.d("loginAttempt", "Login unsuccessful")
                            closeKeyBoard()
                            val mySnackBar = Snackbar.make(
                                view,
                                "Invalid username or password",
                                Snackbar.LENGTH_SHORT
                            )
                            mySnackBar.show()
                        }
                    }
            }
        }

        //Opens the create profile activity
        val createProfileBtn = findViewById<Button>(R.id.loginCreateProfileButton)
        createProfileBtn.setOnClickListener {
            run {
                val intent = Intent(this, CreateProfileActivity::class.java)
                startActivity(intent)
            }
        }

        //Logs the user in as a guest
        val guestBtn = findViewById<Button>(R.id.loginGuestButton)
        guestBtn.setOnClickListener {
            mAuth.signInAnonymously().addOnSuccessListener {
                val temp = mAuth.currentUser!!.isAnonymous
                Log.d("LOGINSCREEN","usersignedinasguest: $temp")
                val intent = Intent(this, MainMenuActivity::class.java)
                startActivity(intent)
            }.addOnFailureListener {
                Log.d("MainActivity", "Guest login failed.")
            }

        }
    }

    /**
     * Ensures that upon returning to this activity the text fields are empty
     */
    override fun onResume() {
        super.onResume()
        val emailText = findViewById<TextInputEditText>(R.id.loginEmailTextInput)
        val passwordText = findViewById<TextInputEditText>(R.id.loginPasswordTextInput)

        emailText.text = null
        passwordText.text = null
    }

    /**
     * Closes the keyboard.
     * Tom's code from lectures.
     */
    private fun closeKeyBoard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}