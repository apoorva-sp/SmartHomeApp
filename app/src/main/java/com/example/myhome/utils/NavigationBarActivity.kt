package com.example.myhome.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.myhome.R
import com.example.myhome.UserInterface.AppliancesActivity
import com.example.myhome.UserInterface.LoginSignupActivity
import com.example.myhome.UserInterface.ProfileActivity
import com.example.myhome.UserInterface.WifiUpdateActivity
import com.google.android.material.navigation.NavigationView

open class NavigationBarActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navigationView: NavigationView
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var requestQueue: com.android.volley.RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.navigation_bar)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        requestQueue = Volley.newRequestQueue(this)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val drawerToggleBtn = findViewById<ImageButton>(R.id.drawerToggle)
        drawerToggleBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START) // ✅ Open drawer when clicked
        }

        setupDrawer()
        setupHeader()
        setupConditionalMenuItems()
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
                    startActivity(intent)
                    true
                }
                R.id.update_wifi_cred ->{
                    val lanEnabled = prefs.getBoolean("LAN", false)
                    if(lanEnabled){
                        val hub_ip = prefs.getString("hub_ip", null)
                        if (!hub_ip.isNullOrEmpty()) {
                            hitOnApEndpoint(hub_ip) // 👈 simple one-line call
                        } else {
                            Toast.makeText(this, "No saved IP found.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "You are outside the network coverage.", Toast.LENGTH_SHORT).show()
                    }
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

    private fun setupConditionalMenuItems() {
        val lanEnabled = prefs.getBoolean("LAN", false)

        val menu = navigationView.menu
        val updateWifiItem = menu.findItem(R.id.update_wifi_cred)

        if (!lanEnabled) {
            val title = updateWifiItem.title.toString()
            val coloredTitle = android.text.SpannableString(title)

            val greyColor = androidx.core.content.ContextCompat.getColor(this, R.color.gray)
            coloredTitle.setSpan(
                android.text.style.ForegroundColorSpan(greyColor),
                0,
                title.length,
                android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )

            updateWifiItem.title = coloredTitle
            updateWifiItem.icon?.setTint(greyColor) // optional
        }
    }

    private fun hitOnApEndpoint(savedIp: String) {
        val url = "http://$savedIp/OnAP"

        val request = StringRequest(
            Request.Method.GET,
            url,
            { response ->
                Log.d("OnAP", "Response: $response")
                Toast.makeText(this, "Device switched to AP mode.", Toast.LENGTH_SHORT).show()

                // ✅ Launch Wi-Fi Update screen
                val intent = Intent(this, WifiUpdateActivity::class.java)
                startActivity(intent)
            },
            { error ->
                Log.e("OnAP", "Error: ${error.message}", error)
                Toast.makeText(this, "Error contacting device: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ).apply {
            retryPolicy = DefaultRetryPolicy(5000, 1, 1.0f)
        }

        // Show quick feedback
        Toast.makeText(this, "Connecting to device...", Toast.LENGTH_SHORT).show()

        // Add to queue
        requestQueue.add(request)
    }



    fun refreshHeaderUsername() {
        val headerView = navigationView.getHeaderView(0)
        val tvUsername = headerView.findViewById<TextView>(R.id.profileName)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "Guest") ?: "Guest"
        tvUsername.text = username
    }

}
