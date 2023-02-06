package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.net.URI

class ProfileFragment : Fragment() {
    val mAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    var currentUser: UserHelperClass? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_profile,container,false)

        updateUI(v)

        //Launches activity to allow the user to edit their profile
        val editProfileBtn = v.findViewById<Button>(R.id.profileEditProfileButton)
        editProfileBtn.setOnClickListener {
            run {
                val intent = Intent(v.context, EditProfileActivity::class.java)
                startActivity(intent)
            }
        }
        return v
    }

    /**
     * Updates UI on resume
     */
    override fun onResume() {
        super.onResume()
        updateUI(requireView())
    }

    /**
     * Updates the UI with user's data
     */
    private fun updateUI(v: View) {
        var users = database.getReference("userTable")
        val currentUserEmail = mAuth.currentUser?.email.toString()
        val userNameText = v.findViewById<TextView>(R.id.profileUserName)
        val userEmailText = v.findViewById<TextView>(R.id.profileEmail)
        val userBioText = v.findViewById<TextView>(R.id.profileBio)
        val userProfilePicture = v.findViewById<ImageView>(R.id.profileProfilePicture)

        //Populates activity with data for current user
        if (currentUserEmail != null) {
            users.orderByChild("email").equalTo(currentUserEmail).get().addOnSuccessListener {
                val value = it.children.elementAt(0).getValue<UserHelperClass>()
                Log.d("TAG", "Value is: $value")
                if (value != null) {
                    currentUser = value

                    userNameText.text = value.name
                    userEmailText.text = value.email
                    userBioText.text = value.bio
                    Picasso.get().load(value.profilePicture).into(userProfilePicture)

                    loadMyReviews(v)
                }
            }
        }
    }

    /**
     * Loads reviews published by the current user into the recyclerview
     */
    private fun loadMyReviews(v: View) {
        if (currentUser != null) {
            val list = ArrayList<ReviewHelperClass>()
            val reviewTableRef = database.getReference("reviews").ref
            reviewTableRef.orderByChild("userID").equalTo(currentUser!!.userID).get().addOnSuccessListener {
                Log.d("LOADING_REVIEWS", "Loaded reviews: ${it.value}")
                for (postSnapshot in it.children) {
                    val review = postSnapshot.getValue(ReviewHelperClass::class.java)
                    if (review != null) {
                        list.add(review)
                        Log.d("PRINTING_REVIEWS",review.toString())
                    }
                }
                val recyclerView = v.findViewById<View>(R.id.profileMyReviewsRecycleView) as RecyclerView
                val layoutManager = LinearLayoutManager(v.context)
                recyclerView.layoutManager = layoutManager
                val reviewAdapter = ReviewAdapter(list)
                recyclerView.adapter = reviewAdapter
            }
        }
    }
}