package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.adapter.ProfileTabAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue

class ProfileActivity : AppCompatActivity() {
    val mAuth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://mobileappscoursework-b20e5-default-rtdb.europe-west1.firebasedatabase.app")
    var isGuest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //If the user is a guest then close the activity
        if (mAuth.currentUser!!.isAnonymous) {
            isGuest = true
            this.finish()
        }

        //Attaches the toolbar to the activity
        val myToolbar = findViewById<Toolbar>(R.id.profileToolbar)
        setSupportActionBar(myToolbar)

        val tabLayout = findViewById<TabLayout>(R.id.profileTabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.profileViewPager)

        //Attaches a tab adapter to the tab layout
        val tabTitles = resources.getStringArray(R.array.profileTabTitles)
        viewPager.adapter = ProfileTabAdapter(this)
        TabLayoutMediator(tabLayout,viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = tabTitles[0]
                1 -> tab.text = tabTitles[1]
            }
        }.attach()

        //Closes the activity if the user logs out
        mAuth.addAuthStateListener {
            if (mAuth.currentUser == null) {
                this.finish()
            }
        }
    }

    /**
     * Inflates the toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Handles toolbar actions
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //Logs the user out
            R.id.action_logout -> {
                mAuth.signOut()
            }

            //Returns the user home
            R.id.action_home -> {
                val intent = Intent(this, MainMenuActivity::class.java)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }
}