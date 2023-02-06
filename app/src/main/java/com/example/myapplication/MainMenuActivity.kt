package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainMenuActivity : AppCompatActivity(), OnMapReadyCallback {
    val mAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    val restaurants = ArrayList<RestaurantHelperClass>()
    private var isGuest = false
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)

        //Attaches the toolbar to this activity
        val myToolbar = findViewById<Toolbar>(R.id.mainMenuToolbar)
        setSupportActionBar(myToolbar)

        //Closes this activity if the user signs out
        mAuth.addAuthStateListener {
            if (mAuth.currentUser == null) {
                this.finish()
            }
        }

        //Checks if the user is a guest
        if (mAuth.currentUser!!.isAnonymous) {
            Log.d("MainMenu", "User is guest")
            isGuest = true
        } else {
            Log.d("MainMenu", "User is not guest")
        }

        //Gets the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loadReviews()
        loadRestaurants()
    }

    /**
     * Inflates toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Handles actions for the toolbar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //Start profile activity
            R.id.action_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }

            //Log user out
            R.id.action_logout -> {
                mAuth.signOut()
            }

            //Start search activity
            R.id.action_search -> {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
            }

            //Refresh current activity
            R.id.action_home -> {
                finish();
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Loads all review into the recyclerview
     */
    private fun loadReviews() {
        val list = ArrayList<ReviewHelperClass>()
        val reviewTableRef = database.getReference("reviews").ref
        reviewTableRef.get().addOnSuccessListener {
            for (postSnapshot in it.children) {
                val review = postSnapshot.getValue(ReviewHelperClass::class.java)
                if (review != null) {
                    list.add(review)
                    Log.d("PRINTING_REVIEWS",review.toString())
                }
            }
            val recyclerView = findViewById<View>(R.id.mainMenuRecyclerView) as RecyclerView
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = layoutManager
            val reviewAdapter = ReviewAdapter(list)
            recyclerView.adapter = reviewAdapter
        }
    }

    /**
     * Loads all restaurants and populates the map with markers
     */
    private fun loadRestaurants() {
        val restaurantTableRef = database.getReference("restaurants").ref
        restaurantTableRef.get().addOnSuccessListener {
            Log.d("LOADING_REVIEWS", "Loaded Restaurants: ${it.value}")
            for (postSnapshot in it.children) {
                val rest = postSnapshot.getValue(RestaurantHelperClass::class.java)
                if (rest != null) {
                    restaurants.add(rest)
                    val name = rest.name
                    val loc = rest.location.split(" ")
                    if (loc.size >= 2) {
                        val lat = loc[0].toDouble()
                        val long = loc[1].toDouble()
                        val location = LatLng(lat,long)
                        mMap.addMarker(MarkerOptions().position(location).title(name))
                    } else {
                        Log.d("Loading Restaurants", "loc failed: $loc")
                    }
                }
            }
        }
    }

    /**
     * Called when map is ready.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Handles opening restaurant activity when clicking a marker info window
        mMap.setOnInfoWindowClickListener {
            for (i in 0 until restaurants.size) {
                if (restaurants[i].name == it.title) {
                    val restaurantID = restaurants[i].restaurantID
                    val intent = Intent(this, RestaurantActivity::class.java)
                    intent.putExtra("restaurantID",restaurantID)
                    startActivity(intent)
                }
            }
        }

        //Handles zooming into a marker when clicked
        mMap.setOnMarkerClickListener {
            it.showInfoWindow()
            val camUp = CameraUpdateFactory.newLatLngZoom(it.position,15f)
            mMap.animateCamera(camUp)
            return@setOnMarkerClickListener true
        }

        getLastLocation()
    }

    /**
     * Gets the last location of the user.
     * Only called once as live updates aren't necessary.
     */
    private fun getLastLocation() {
        if (isLocationEnabled()) {
            // checking location permission
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE
                )
                return
            }
            //Get last location of user
            mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                val location: Location? = task.result
                if (location == null) {
                    getLastLocation()
                } else {
                    val lat = location.latitude
                    val long = location.longitude

                    Log.i("LocLatLocation", "$lat and $long")

                    val lastLoc = LatLng(lat, long)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLoc,10f))
                }
            }
        } else {
            //If permission denied then request permission
            val mRootView = findViewById<View>(R.id.map)
            val locSnack = Snackbar.make(mRootView, "R.string.location_switch", Snackbar.LENGTH_LONG)
            locSnack.show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    /**
     * Checks if location is enabled.
     * Tom's method from lectures.
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    companion object {
        const val LOCATION_PERMISSION_REQ_CODE = 42
    }
}