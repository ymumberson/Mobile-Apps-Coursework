package com.example.myapplication.adapter

import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.ProfileFragment
import com.example.myapplication.MyRestaurantFragment

class ProfileTabAdapter (activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return ProfileFragment()
            1 -> return MyRestaurantFragment()
        }
        return ProfileFragment()
    }
}