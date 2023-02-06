package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.provider.Settings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.example.myapplication.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    val mAuth = FirebaseAuth.getInstance()
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var location: LatLng? =null
    private var restaurant: RestaurantHelperClass? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Takes in a passed in restaurant used to add a marker to the map
        // and launch the restaurant activity
        if (intent.extras != null) {
            val rest = intent.extras!!.get("restaurant")
            if (rest != null) {
                restaurant = rest as RestaurantHelperClass
                val loc = restaurant!!.location.split(" ")
                val lat = loc[0]
                val long = loc[1]
                if (!lat.isNullOrEmpty() && !long.isNullOrEmpty()) {
                    Log.d("Tag", "$lat $long")
                    location = LatLng(lat.toDouble(),long.toDouble())
                }
            }
        }

        //Closes this activity if the user signs out
        mAuth.addAuthStateListener {
            if (mAuth.currentUser == null) {
                this.finish()
            }
        }

        //Binds the map activity to the layout
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Add toolbar to activity
        val myToolbar = findViewById<Toolbar>(R.id.mapsToolbar)
        setSupportActionBar(myToolbar)
        myToolbar.setNavigationOnClickListener {
            this.finish()
        }
    }

    /**
     * Inflates the toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.maps_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Handles toolbar actions
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_confirm -> {
                //Returns location inside of finish()
                this.finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //If loading restaurant then show it on the map
        if (location != null) {
            mMap.addMarker(MarkerOptions().position(location!!).title(restaurant!!.name))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(location!!))
            mMap.setOnInfoWindowClickListener {
                val intent = Intent(this, RestaurantActivity::class.java)
                intent.putExtra("restaurantID",restaurant!!.restaurantID)
                startActivity(intent)
            }
        } else { //Otherwise capture a new location entered by the user
            mMap.setOnMapClickListener {
                location = it
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(location!!).title("Your click"))
            }
        }

        //Animate camera if marker pressed on
        mMap.setOnMarkerClickListener {
            it.showInfoWindow()
            val camUp = CameraUpdateFactory.newLatLngZoom(it.position,15f)
            mMap.animateCamera(camUp)
            return@setOnMarkerClickListener true
        }
    }

    /**
     * Called when activity finishes.
     * Returns a new location if one was entered by the user.
     */
    override fun finish() {
        if (location != null) {
            val data = Intent()
            Log.d("MapActivity:",location.toString())
            data.putExtra("latitude", location!!.latitude.toString())
            data.putExtra("longitude", location!!.longitude.toString())
            setResult(Activity.RESULT_OK, data)
        } else {
            Log.d("MapActivity:","Location was null")
        }
        super.finish()
    }
}