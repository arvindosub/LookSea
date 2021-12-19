package com.arvind.looksea

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.arvind.looksea.databinding.ActivityCreateBinding
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import java.io.File
import java.lang.ref.Reference

private const val TAG = "CreateActivity"

class CreateActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityCreateBinding
    private lateinit var storageReference: StorageReference
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var fileReference: StorageReference
    private lateinit var fileUploadUri: Uri
    private var location: GeoPoint = GeoPoint(0.0, 0.0)
    private var photoUri: Uri? = null
    private var videoUri: Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.imageView.isVisible = true
            binding.videoView.isVisible = false
            binding.imageView.setImageURI(photoUri)
        }
    }
    private fun createImageFile(): File {
        return File.createTempFile("cameraImage", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
    }
    private val videoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            binding.imageView.isVisible = false
            binding.videoView.isVisible = true
            binding.videoView.setVideoURI(videoUri)
            binding.videoView.setMediaController(MediaController(this))
            binding.videoView.start()
        }
    }
    private fun createVideoFile(): File {
        return File.createTempFile("cameraVideo", ".mp4", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
    }
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent(), ActivityResultCallback {
            binding.imageView.isVisible = true
            binding.videoView.isVisible = false
            binding.imageView.setImageURI(it)
            Log.i(TAG, "photoUri $it")
            photoUri = it
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

        binding = ActivityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnTakeVideo.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                Log.i(TAG, "Opening up camera app on device")
                videoUri = FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".provider", createVideoFile())
                videoLauncher.launch(videoUri)
            }
            fetchLocation()
        }

        binding.btnTakePicture.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                Log.i(TAG, "Opening up camera app on device")
                photoUri = FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".provider", createImageFile())
                cameraLauncher.launch(photoUri)
            }
            fetchLocation()
        }

        binding.btnPickImage.setOnClickListener {
            Log.i(TAG, "Opening up file browser on device")
            getImage.launch("image/*")
        }

        binding.btnSubmit.setOnClickListener {
            handleSubmitButtonClick()
        }
    }

    private fun handleSubmitButtonClick() {
        if (photoUri == null && videoUri == null) {
            Toast.makeText(this, "No video/image selected", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.etDescription.text.isBlank()) {
            Toast.makeText(this, "Description cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (signedInUser == null) {
            Toast.makeText(this, "No signed-in user...", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        if ((binding.videoView.isVisible) && !(binding.imageView.isVisible)) {
            fileUploadUri = videoUri as Uri
            fileReference = storageReference.child("videos/${System.currentTimeMillis()}-video.mp4")
            Log.i(TAG, "$fileReference, $fileUploadUri")
        } else if ((binding.imageView.isVisible) && !(binding.videoView.isVisible)) {
            fileUploadUri = photoUri as Uri
            fileReference = storageReference.child("images/${System.currentTimeMillis()}-photo.jpg")
        }
        // Upload file to Firebase Storage
        fileReference.putFile(fileUploadUri)
            .continueWithTask { fileUploadTask ->
                Log.i(TAG, "uploaded bytes: ${fileUploadTask.result?.bytesTransferred}")
                // Retrieve url of uploaded file to Firestore
                fileReference.downloadUrl
            }.continueWithTask { downloadUrlTask ->
                // Create a post object with the file url and add it to posts collection
                val post = Post(
                    binding.etDescription.text.toString(),
                    downloadUrlTask.result.toString(),
                    System.currentTimeMillis(),
                    location,
                    signedInUser)
                firestoreDb.collection("posts").add(post)
            }.addOnCompleteListener { postCreationTask ->
                binding.btnSubmit.isEnabled = true
                if (!postCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception during Firebase operations", postCreationTask.exception)
                    Toast.makeText(this, "Failed to save post...", Toast.LENGTH_SHORT).show()
                }
                binding.etDescription.text.clear()
                binding.imageView.setImageResource(0)
                binding.videoView.setVideoURI(null)
                binding.imageView.isVisible = true
                binding.videoView.isVisible = false
                Toast.makeText(this, "File uploaded!", Toast.LENGTH_SHORT).show()
                val profileIntent = Intent(this, ProfileActivity::class.java)
                profileIntent.putExtra(EXTRA_USERNAME, signedInUser?.username)
                startActivity(profileIntent)
                finish()
            }
    }
}