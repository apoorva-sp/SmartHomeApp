package com.example.myhome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginSignup : AppCompatActivity() {

    private lateinit var btnSignin: Button
    private lateinit var edtPhone: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtUsername: EditText
    private lateinit var tvToggle: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var tvInstruction: TextView
    private lateinit var tvUsernameLabel: TextView

    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            startActivity(Intent(this, HomePage::class.java))
            finish()
            return
        }

        setContentView(R.layout.login_v2)

        // Initialize views
        edtPhone = findViewById(R.id.etPhone)
        edtPassword = findViewById(R.id.etPassword)
        edtUsername = findViewById(R.id.etUsername)
        btnSignin = findViewById(R.id.btnSignIn)
        tvToggle = findViewById(R.id.tvSignUp)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvInstruction = findViewById(R.id.tvInstruction)
        tvUsernameLabel = findViewById(R.id.tvUsernameLabel)

        btnSignin.setOnClickListener {
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val username = edtUsername.text.toString().trim()

            if (isSignUpMode) {
                if (phone.isNotEmpty() && password.isNotEmpty()  && username.isNotEmpty()) {
                    // Save signup data after api call and validation
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("phone", phone)
                        .putString("password", password)
                        .putString("username", username)
                        .apply()
                    Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show()

//                    startActivity(Intent(this, MainActivity::class.java))
//                    finish()
                }
            } else {
                if (phone.isNotEmpty() && password.isNotEmpty()) {
                    // Save login data
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("phone", phone)
                        .putString("password", password)
                        .apply()
                    Toast.makeText(this, "Loging in...", Toast.LENGTH_SHORT).show()

//                    startActivity(Intent(this, MainActivity::class.java))
//                    finish()
                }
            }
        }

        tvToggle.setOnClickListener {
            toggleLoginSignup()
        }
    }

    private fun toggleLoginSignup() {
        isSignUpMode = !isSignUpMode
        if (isSignUpMode) {
            // Show extra fields
            edtUsername.visibility = View.VISIBLE
            tvUsernameLabel.visibility = View.VISIBLE
            tvWelcome.text = "Create Account"
            tvInstruction.text = "Fill in the details below to sign up"
            btnSignin.text = "Sign Up"
            tvToggle.text = "Already have an account? Login"
        } else {
            // Hide extra fields
            edtUsername.visibility = View.GONE
            tvUsernameLabel.visibility = View.GONE
            tvWelcome.text = "Welcome Back"
            tvInstruction.text = "Enter your phone number and password to sign in"
            btnSignin.text = "Login"
            tvToggle.text = "Don't have an account? Sign up"
        }
    }
}
