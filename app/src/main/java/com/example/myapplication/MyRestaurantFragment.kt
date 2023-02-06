package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue

class MyRestaurantFragment : Fragment() {
    val mAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    var userID: String? = null
    private lateinit var restaurantAdapter: RestaurantAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_my_restaurant,container,false)

        //Find userID associated with currently logged in user
        findUserID(v)

        //Handles adding a new restaurant to the user's profile
        val addRestaurantButton = v.findViewById<FloatingActionButton>(R.id.myRestaurantAddRestaurantButton)
        addRestaurantButton.setOnClickListener {
            if (!userID.isNullOrEmpty()) {
                val intent = Intent(v.context, CreateRestaurantActivity::class.java)
                intent.putExtra("userID",userID)
                startActivity(intent)
            }
        }

        return v
    }

    /**
     * On resume updates the recycler view
     */
    override fun onResume() {
        super.onResume()
        restaurantAdapter.notifyDataSetChanged()
    }

    /**
     * Find userID associated with currently logged in user
     */
    private fun findUserID(v: View) {
        val users = database.getReference("userTable")
        val currentUserEmail = mAuth.currentUser?.email.toString()
        users.orderByChild("email").equalTo(currentUserEmail).get().addOnSuccessListener {
            Log.d(LOG_TAG, "UserID success")
            val value = it.children.elementAt(0).getValue<UserHelperClass>()
            if (value != null) {
                userID = value.userID
                loadRestaurants(v,value)
            }
        }.addOnFailureListener {
            Log.d(LOG_TAG, "UserID failure")
        }
    }

    /**
     * Loads all restaurants owned by the current user
     */
    private fun loadRestaurants(v: View, currentUser: UserHelperClass?) {
        if (currentUser != null) {
            val list = ArrayList<RestaurantHelperClass>()
            val restaurantTableRef = database.getReference("restaurants").ref
            restaurantTableRef.orderByChild("ownerID").equalTo(userID).get().addOnSuccessListener {
                Log.d("LOADING_REVIEWS", "Loaded Restaurants: ${it.value}")
                for (postSnapshot in it.children) {
                    val rest = postSnapshot.getValue(RestaurantHelperClass::class.java)
                    if (rest != null) {
                        list.add(rest)
                        Log.d("PRINTING_REVIEWS",rest.toString())
                    }
                }
                val recyclerView = v.findViewById<View>(R.id.myRestaurantRecycleView) as RecyclerView
                val layoutManager = LinearLayoutManager(v.context)
                recyclerView.layoutManager = layoutManager
                restaurantAdapter = RestaurantAdapter(list)
                recyclerView.adapter = restaurantAdapter
            }
        }
    }

    companion object {
        const val LOG_TAG = "My Restaurants"
    }
}