package com.arvind.looksea

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.arvind.looksea.databinding.ActivityParticularsBinding
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import java.io.File

private const val TAG = "ParticularsActivity"

class ParticularsActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private var userId: String? = FirebaseAuth.getInstance().currentUser?.uid as String
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityParticularsBinding
    private lateinit var storageReference: StorageReference
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var fileReference: StorageReference
    private lateinit var fileUploadUri: Uri
    private var location: GeoPoint = GeoPoint(0.0, 0.0)
    private var imageUri: Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.profilePic.setImageURI(imageUri)
        }
    }
    private fun createImageFile(): File {
        return File.createTempFile("cameraImage", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
    }

    private val getFile = registerForActivityResult(ActivityResultContracts.GetContent(), ActivityResultCallback {
        binding.profilePic.setImageURI(it)
        Log.i(TAG, "imageUri $it")
        imageUri = it
    })

    private fun fetchLocation() {
        val task = fusedLocationProviderClient.lastLocation

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        task.addOnSuccessListener {
            Log.i(TAG, "${it.latitude}, ${it.longitude}")
            location = GeoPoint(it.latitude, it.longitude)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityParticularsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference

        firestoreDb = FirebaseFirestore.getInstance()
        userId?.let {
            firestoreDb.collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    signedInUser = userSnapshot.toObject(User::class.java)
                    Log.i(TAG, "Signed-In User: $signedInUser")

                    binding.tvUsername.text = signedInUser?.username.toString()
                    binding.etAge.hint = signedInUser?.age.toString()
                    binding.etAbout.hint = signedInUser?.description.toString()

                }
                .addOnFailureListener { exception ->
                    Log.i(TAG, "Failed to fetch signed-in user", exception)
                }
                .addOnCompleteListener {
                    if (imageUri == null && signedInUser?.picture != "") {
                        Glide.with(this).load(signedInUser?.picture).into(binding.profilePic)
                    }
                }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnChoosePic.setOnClickListener {
            Log.i(TAG, "Opening up image file browser on device")
            getFile.launch("image/*")
        }

        binding.btnTakePic.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                Log.i(TAG, "Opening up camera app on device")
                imageUri = FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".provider", createImageFile())
                cameraLauncher.launch(imageUri)
            }
            fetchLocation()
        }

        binding.btnSubmit.setOnClickListener {
            handleSubmitButtonClick()
        }
    }

    private fun handleSubmitButtonClick() {
        if (imageUri == null && binding.etAbout.text.isBlank() && binding.etAge.text.isBlank()) {
            Toast.makeText(this, "No changes made...", Toast.LENGTH_SHORT).show()
            return
        }

        var newAge = 0
        var newDesc = ""
        var newUrl = ""

        if (binding.etAge.text.isBlank()) {
            newAge = signedInUser?.age!!
        } else {
            newAge = binding.etAge.text.toString().toInt()
        }

        if (binding.etAbout.text.isBlank()) {
            newDesc = signedInUser?.description.toString()
        } else {
            newDesc = binding.etAbout.text.toString()
        }

        binding.btnSubmit.isEnabled = false
        if (imageUri != null) {
            fileUploadUri = imageUri as Uri
            fileReference = storageReference.child("profilepics/${userId}-photo.jpg")

            // Upload file to Firebase Storage
            fileReference.delete().addOnCompleteListener {
                fileReference.putFile(fileUploadUri)
                    .continueWithTask { fileUploadTask ->
                        Log.i(TAG, "uploaded bytes: ${fileUploadTask.result?.bytesTransferred}")
                        // Retrieve url of uploaded file to Firestore
                        fileReference.downloadUrl
                    }.continueWithTask { downloadUrlTask ->
                        // Edit user details
                        val user = User(
                            signedInUser?.username.toString(),
                            newAge,
                            newDesc,
                            downloadUrlTask.result.toString()
                        )
                        firestoreDb.collection("users").document("${userId}").set(user)
                    }.addOnCompleteListener { userUpdateTask ->
                        binding.btnSubmit.isEnabled = true
                        if (!userUpdateTask.isSuccessful) {
                            Log.e(
                                TAG,
                                "Exception during Firebase operations",
                                userUpdateTask.exception
                            )
                            Toast.makeText(this, "Failed to update user...", Toast.LENGTH_SHORT).show()
                        }

                        Toast.makeText(this, "User profile updated!", Toast.LENGTH_SHORT).show()
                        val profileIntent = Intent(this, ProfileActivity::class.java)
                        profileIntent.putExtra(EXTRA_USERNAME, signedInUser?.username)
                        startActivity(profileIntent)
                        finish()
                    }
            }
        } else {
            newUrl = signedInUser?.picture.toString()
            val user = User(
                signedInUser?.username.toString(),
                newAge,
                newDesc,
                newUrl
            )
            firestoreDb.collection("users").document("${userId}").set(user)
                .addOnCompleteListener { userUpdateTask ->
                    binding.btnSubmit.isEnabled = true
                    if (!userUpdateTask.isSuccessful) {
                        Log.e(
                            TAG,
                            "Exception during Firebase operations",
                            userUpdateTask.exception
                        )
                        Toast.makeText(this, "Failed to update user...", Toast.LENGTH_SHORT).show()
                    }

                    Toast.makeText(this, "User profile updated!", Toast.LENGTH_SHORT).show()
                    val profileIntent = Intent(this, ProfileActivity::class.java)
                    profileIntent.putExtra(EXTRA_USERNAME, signedInUser?.username)
                    startActivity(profileIntent)
                    finish()
                }
        }
    }
}