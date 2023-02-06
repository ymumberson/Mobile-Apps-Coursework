package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SearchActivity : AppCompatActivity() {
    val mAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    val allRestaurants = ArrayList<RestaurantHelperClass>()
    val searchList = ArrayList<RestaurantHelperClass>()
    val restaurantAdapter = RestaurantAdapter(searchList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        //Attaches toolbar to activity
        val myToolbar = findViewById<Toolbar>(R.id.searchToolbar)
        setSupportActionBar(myToolbar)

        //If user logs out then close activity
        mAuth.addAuthStateListener {
            if (mAuth.currentUser == null) {
                this.finish()
            }
        }

        loadRestaurants()
    }

    /**
     * Inflates the toolbar and adds search functionality.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        if (menu != null) {

            //Gets searchview from the toolbar
            val searchView = menu.findItem(R.id.searchIcon).actionView as SearchView

            //Listener for queries
            searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

                //Handles queries being submitted.
                //Updates restaurant list to show only restaurants with names containing the query string
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d("Search","Submitted $query")
                    if (query.isNullOrEmpty()) { //If query empty the reset restaurant list
                        searchList.clear()
                        searchList.addAll(allRestaurants)
                    } else { //Otherwise filter restaurant list
                        searchList.clear()
                        for (i in 0 until allRestaurants.size) {
                            if (allRestaurants[i].name.contains(query.toString(),ignoreCase = true)) {
                                searchList.add(allRestaurants[i])
                            }
                        }
                    }
                    restaurantAdapter.notifyDataSetChanged()
                    return true
                }

                //Handles query text changing
                override fun onQueryTextChange(newText: String?): Boolean {
                    Log.d("Search","new text: $newText")
                    if (newText.isNullOrEmpty()) {
                        this.onQueryTextSubmit("")
                    }
                    return true
                }
            })
        }


        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Loads all restaurants into recyclerview
     */
    private fun loadRestaurants() {
        val restaurantTableRef = database.getReference("restaurants").ref
        restaurantTableRef.get().addOnSuccessListener {
            Log.d("LOADING_REVIEWS", "Loaded Restaurants: ${it.value}")
            for (postSnapshot in it.children) {
                val rest = postSnapshot.getValue(RestaurantHelperClass::class.java)
                if (rest != null) {
                    searchList.add(rest)
                    allRestaurants.add(rest)
//                    Log.d("PRINTING_REVIEWS",rest.toString())
                }
            }
            val recyclerView = findViewById<View>(R.id.searchRecyclerView) as RecyclerView
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = restaurantAdapter
            restaurantAdapter.notifyDataSetChanged()
        }
    }
}