package com.arvind.looksea

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arvind.looksea.databinding.ActivityRegisterBinding
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "RegisterActivity"

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firestoreDb: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        binding.btnRegister.setOnClickListener {
            binding.btnRegister.isEnabled = false
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "All fields must be filled in!", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                return@setOnClickListener
            }

            // Create user
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { regn ->
                binding.btnRegister.isEnabled = true
                if (regn.isSuccessful) {
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { login ->
                        if (login.isSuccessful) {
                            val userId = auth.currentUser?.uid as String
                            val user = User(
                                "newUser",
                                "",
                                "",
                                "user"
                            )
                            firestoreDb.collection("artifacts")
                                .document(userId)
                                .set(user)
                                .addOnCompleteListener { userCreationTask ->
                                    if (!userCreationTask.isSuccessful) {
                                        Log.e(TAG, "createUserInFirestore: failure", regn.exception)
                                        Toast.makeText(this, "User Creation Failed...", Toast.LENGTH_SHORT).show()
                                    }
                                    Log.i(TAG, "User Created and Logged-In!")
                                    Toast.makeText(this, "User Created and Logged-In!", Toast.LENGTH_SHORT).show()
                                    val profileIntent = Intent(this, ProfileActivity::class.java)
                                    profileIntent.putExtra(EXTRA_USERNAME, "newUser")
                                    startActivity(profileIntent)
                                    finish()
                                }
                        }
                    }
                } else {
                    Log.e(TAG, "registerUser: failure", regn.exception)
                    Toast.makeText(this, "Registration Failed...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}