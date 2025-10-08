package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.util.Log
import com.example.myhome.Keys
import com.example.myhome.R
import com.example.myhome.network.UdpPortManager

class LoginSignupActivity : AppCompatActivity() {

    private lateinit var btnAction: Button
    private lateinit var edtPhone: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtUsername: EditText
    private lateinit var tvToggle: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var tvInstruction: TextView
    private lateinit var tvUsernameLabel: TextView
    private lateinit var requestQueue: com.android.volley.RequestQueue
    private var url = ""
    private var isSignUpMode = false
    private lateinit var prefs: android.content.SharedPreferences // ⭐ cache prefs
    private val TAG = "LoginSignup"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_esp32_setup_done",false)

        // If already logged in and esp32 setup also done  → go straight to Home
        if (prefs.getBoolean("is_logged_in", false) && prefs.getBoolean("is_esp32_setup_done",false)) {
            val intent = Intent(this, IpDiscoveryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        } else if (prefs.getBoolean("is_logged_in", false) && !prefs.getBoolean("is_esp32_setup_done",false)) {
            val intent = Intent(this, WifiLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.login_v2)




        // Initialize views
        edtPhone = findViewById(R.id.etPhone)
        edtPassword = findViewById(R.id.etPassword)
        edtUsername = findViewById(R.id.etUsername)
        btnAction = findViewById(R.id.btnSignIn)
        tvToggle = findViewById(R.id.tvSignUp)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvInstruction = findViewById(R.id.tvInstruction)
        tvUsernameLabel = findViewById(R.id.tvUsernameLabel)

        requestQueue = Volley.newRequestQueue(this)
        url = Keys.BaseURL +"userAPI.php"

        btnAction.setOnClickListener {
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val username = edtUsername.text.toString().trim()

            if (isSignUpMode) {
                if (phone.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                    signUp(username, password, phone)
                } else {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (phone.isNotEmpty() && password.isNotEmpty()) {
                    // ⭐ Check SharedPreferences first
                    val savedPhone = prefs.getString("phone", null)
                    val savedPassword = prefs.getString("password", null)

                    if (savedPhone == phone && savedPassword == password) {
                        Toast.makeText(this, "Login successful (local)!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, IpDiscoveryActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        login(phone, password) // fallback → call API
                    }
                } else {
                    Toast.makeText(this, "Phone and password required", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvToggle.setOnClickListener { toggleLoginSignup() }
    }

    private fun toggleLoginSignup() {
        isSignUpMode = !isSignUpMode
        if (isSignUpMode) {
            edtUsername.visibility = View.VISIBLE
            tvUsernameLabel.visibility = View.VISIBLE
            tvWelcome.text = "Create Account"
            tvInstruction.text = "Fill in the details below to sign up"
            btnAction.text = "Sign Up"
            tvToggle.text = "Already have an account? Login"
        } else {
            edtUsername.visibility = View.GONE
            tvUsernameLabel.visibility = View.GONE
            tvWelcome.text = "Welcome Back"
            tvInstruction.text = "Enter your phone number and password to sign in"
            btnAction.text = "Login"
            tvToggle.text = "Don't have an account? Sign up"
        }
    }

    private fun login(phone: String, password: String) {
        val jsonBody = JSONObject().apply {
            put("serviceID", 2)
            put("phonenumber", phone)
            put("password", password)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonBody,
            Response.Listener { response ->
                val code = response.optInt("code", -1)
                val message = response.optString("message", "Unknown error")
                val userId = response.optInt("userId", -1)
                val username = response.optString("username", "")

                if (code == 0) {
                    // ⭐ Save credentials for future offline login
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("phone", phone)
                        .putString("username", username)
                        .putString("password", password)
                        .putInt("userId", userId)
                        .apply()
                    Log.d(TAG, "Login response: code=$code, message=$message, userId=$userId, username=$username") // ⭐ log success

                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, WifiLoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Login failed: $message", Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Login API error: ${error.message}", error) // ⭐ log errors with stacktrace

                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        requestQueue.add(request)
    }

    private fun signUp(username: String, password: String, phone: String) {
        val jsonBody = JSONObject().apply {
            put("serviceID", 1)
            put("username", username)
            put("password", password)
            put("phonenumber", phone)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonBody,
            Response.Listener { response ->
                val code = response.optInt("code", -1)
                val message = response.optString("message", "Unknown error")
                val userId = response.optInt("userId", -1)

                if (code == 0) {
                    // ⭐ Save credentials locally
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("phone", phone)
                        .putString("username", username)
                        .putString("password", password)
                        .putInt("userId", userId)
                        .apply()
                    Log.d(TAG, "Signup response: code=$code, message=$message, userId=$userId") // ⭐ log success

                    Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, WifiLoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Signup failed: $message", Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Signup API error: ${error.message}", error) // ⭐ log errors with stacktrace

                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        requestQueue.add(request)
    }
}
