package com.arvind.looksea

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.arvind.looksea.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "LoginActivity"

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val auth = FirebaseAuth.getInstance()

        // Check if user is signed in (non-null) and update UI accordingly.
        if (auth.currentUser != null) {
            goHomeActivity()
        }

        binding.btnLogin.setOnClickListener {
            binding.btnLogin.isEnabled = false
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Email/Password cannot be empty!", Toast.LENGTH_SHORT).show()
                binding.btnLogin.isEnabled = true
                return@setOnClickListener
            }

            // Perform login
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                binding.btnLogin.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Logged-In Successfully!", Toast.LENGTH_SHORT).show()
                    goHomeActivity()
                } else {
                    Log.e(TAG, "signInWithEmail: failure", task.exception)
                    Toast.makeText(this, "Authentication Failed...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            goRegisterActivity()
        }

    }

    private fun goHomeActivity() {
        Log.i(TAG, "goHomeActivity")
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goRegisterActivity() {
        Log.i(TAG, "goRegisterActivity")
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}