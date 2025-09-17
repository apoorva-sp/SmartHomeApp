package com.example.myhome.UserInterface

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myhome.R
import com.example.myhome.utils.ExitUtils
import com.example.myhome.utils.NavigationBarActivity
import org.json.JSONObject

class ProfileActivity : NavigationBarActivity() {

    private lateinit var requestQueue: RequestQueue
    private var userId: Int? = null
    private lateinit var prefs: android.content.SharedPreferences
    private var url = ""
    private lateinit var  usernameNote :TextView
    private lateinit var  passwordNote :TextView
    private var TAG ="profile"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate profile layout into container of NavigationBarActivity
        val container = findViewById<android.widget.FrameLayout>(R.id.container)
        layoutInflater.inflate(R.layout.account_settings, container, true)

        findViewById<TextView>(R.id.pageTitle).text = "Profile"

        // Init Volley
        requestQueue = Volley.newRequestQueue(this)
        url = "https://capstone.pivotpt.in/userAPI.php"

        // Retrieve user_id from SharedPreferences
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = prefs.getInt("userId", -1)



        // Views
        val editUsername = findViewById<EditText>(R.id.editUsername)
        usernameNote = findViewById(R.id.usernameNote)
        val btnUpdateUsername = findViewById<Button>(R.id.btnUpdateUsername)

        val editPassword = findViewById<EditText>(R.id.editPassword)
        val editConfirmPassword = findViewById<EditText>(R.id.editConfirmPassword)
        passwordNote = findViewById(R.id.passwordNote)
        val btnUpdatePassword = findViewById<Button>(R.id.btnUpdatePassword)

        // Username update handler
        btnUpdateUsername.setOnClickListener {
            val newUsername = editUsername.text.toString().trim()

            if (newUsername.isEmpty()) {
                usernameNote.text = "Please enter a username"
                usernameNote.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                return@setOnClickListener
            }

            usernameNote.text = "" // clear previous note
            updateUsername(newUsername)
        }

        // Password update handler
        btnUpdatePassword.setOnClickListener {
            val newPassword = editPassword.text.toString().trim()
            val confirmPassword = editConfirmPassword.text.toString().trim()
            var msg: String? = null

            when {
                newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                    msg = "Please fill in both password fields"
                }
                newPassword.length < 8 -> {
                    msg = "Password must be at least 8 characters"
                }
                newPassword != confirmPassword -> {
                    msg = "Passwords do not match"
                }
            }

            if (msg != null) {
                passwordNote.text = msg
                passwordNote.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                return@setOnClickListener
            }

            passwordNote.text = "" // clear previous note
            updatePassword(newPassword)
        }
    }

    private fun updateUsername(newUsername: String) {
         // 🔹 Replace with your API URL

        val params = JSONObject().apply {
            put("user_id", userId)
            put("username", newUsername)
            put("serviceID", 3)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            { response ->
                val code = response.optInt("code", -1)
                val message = response.optString("message", "Unknown error")

                if(code == 0){
                    Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show()
                    findViewById<EditText>(R.id.editUsername).text.clear()
                    prefs.edit()
                        .putString("username", newUsername)
                        .apply()
                    refreshHeaderUsername()

                }
                else{
                    usernameNote.text = message
                    usernameNote.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.d(TAG,"Error: ${error.message}" ) // ⭐ log success

            }
        )

        requestQueue.add(request)
    }

    private fun updatePassword(newPassword: String) {

        val params = JSONObject().apply {
            put("user_id", userId)
            put("password", newPassword)
            put("serviceID", 4)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            { response ->

                val code = response.optInt("code", -1)
                val message = response.optString("message", "Unknown error")

                if(code == 0){
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    findViewById<EditText>(R.id.editPassword).text.clear()
                    findViewById<EditText>(R.id.editConfirmPassword).text.clear()
                    prefs.edit()
                        .putString("password", newPassword)
                        .apply()
                }
                else{
                    passwordNote.text = message
                    passwordNote.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.d(TAG,"Error: ${error.message}" ) // ⭐ log success

            }
        )

        requestQueue.add(request)
    }

    override fun onBackPressed() {
        if (isTaskRoot) ExitUtils.showExitDialog(this)
        else super.onBackPressed()
    }
}
