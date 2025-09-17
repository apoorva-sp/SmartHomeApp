package com.example.myhome.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.myhome.R
import com.example.myhome.UserInterface.AppliancesActivity
import com.example.myhome.UserInterface.LoginSignupActivity
import com.example.myhome.UserInterface.ProfileActivity
import com.google.android.material.navigation.NavigationView

open class NavigationBarActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navigationView: NavigationView
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.navigation_bar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val drawerToggleBtn = findViewById<ImageButton>(R.id.drawerToggle)
        drawerToggleBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START) // ✅ Open drawer when clicked
        }

        setupDrawer()
        setupHeader()
    }

    /**
     * Inject username into navigation drawer header
     */
    private fun setupHeader() {
        val headerView = navigationView.getHeaderView(0)
        val tvUsername = headerView.findViewById<TextView>(R.id.profileName)

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var username = prefs.getString("username", "Guest") // fallback if null/empty
        if (username.isNullOrBlank()) {
            username = "Guest"
        }
        tvUsername.text = username
    }

    private fun setupDrawer() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, AppliancesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }

                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }

                R.id.nav_logout -> {
                    handleLogout()
                    true
                }

                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun handleLogout() {
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show()
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("phone")
            .remove("password")
            .remove("is_logged_in")
            .remove("username")
            .apply()

        val intent = Intent(this, LoginSignupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun refreshHeaderUsername() {
        val headerView = navigationView.getHeaderView(0)
        val tvUsername = headerView.findViewById<TextView>(R.id.profileName)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "Guest") ?: "Guest"
        tvUsername.text = username
    }

}
