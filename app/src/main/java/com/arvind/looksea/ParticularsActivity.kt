package com.arvind.looksea

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.arvind.looksea.models.Post
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.util.FileUtil.delete
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.nio.file.Files.delete

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
    private var deleted: Boolean = false
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
            firestoreDb.collection("artifacts")
                .document(it)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    signedInUser = userSnapshot.toObject(User::class.java)
                    Log.i(TAG, "Signed-In User: $signedInUser")

                    binding.etUsername.setText(signedInUser?.username)
                    binding.etAbout.setText(signedInUser?.description)

                }
                .addOnFailureListener { exception ->
                    Log.i(TAG, "Failed to fetch signed-in user", exception)
                }
                .addOnCompleteListener {
                    if (imageUri == null && signedInUser?.file_url != "") {
                        Glide.with(this).load(signedInUser?.file_url).into(binding.profilePic)
                    } else {
                        Glide.with(this).load("https://firebasestorage.googleapis.com/v0/b/looksea-43f7d.appspot.com/o/profilepics%2Fdefault_icon.png?alt=media&token=7e6d6755-726d-4f02-ae75-f74cda6dd748").into(binding.profilePic)
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

        binding.btnRemovePic.setOnClickListener {
            imageUri = null
            deleted = true
            Glide.with(this).load("https://firebasestorage.googleapis.com/v0/b/looksea-43f7d.appspot.com/o/profilepics%2Fdefault_icon.png?alt=media&token=7e6d6755-726d-4f02-ae75-f74cda6dd748").into(binding.profilePic)
        }

        binding.btnSuggest.setOnClickListener {
            handleAnalysis()
        }

        binding.btnSubmit.setOnClickListener {
            handleSubmitButtonClick()
        }

    }

    private fun handleAnalysis() {
        var tagString = ""
        if (imageUri != null) {
            Glide.with(this).asBitmap().load(imageUri).into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    val image = InputImage.fromBitmap(resource, 0)
                    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                    labeler.process(image)
                        .addOnSuccessListener { labels ->
                            for (label in labels) {
                                tagString += "#${label.text.lowercase()} "
                                Log.i(TAG, "${label.index}. ${label.text}: ${label.confidence}")
                            }
                            tagString = tagString.dropLast(1)
                            Log.i(TAG, tagString)
                            binding.etAbout.setText(tagString)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "$e")
                        }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
        } else {
            Glide.with(this).asBitmap().load(signedInUser?.file_url).into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    val image = InputImage.fromBitmap(resource, 0)
                    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                    labeler.process(image)
                        .addOnSuccessListener { labels ->
                            for (label in labels) {
                                tagString += "#${label.text.lowercase()} "
                                Log.i(TAG, "${label.index}. ${label.text}: ${label.confidence}")
                            }
                            tagString = tagString.dropLast(1)
                            Log.i(TAG, tagString)
                            binding.etAbout.setText(tagString)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "$e")
                        }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
        }
    }

    private fun handleSubmitButtonClick() {
        if (imageUri == null && binding.etAbout.text.isBlank() && binding.etUsername.text.isBlank()) {
            Toast.makeText(this, "No changes made...", Toast.LENGTH_SHORT).show()
            return
        }

        var newDesc = ""
        var newUrl = ""
        var newUsername = ""
        var oldUsername = signedInUser?.username.toString()

        if (binding.etAbout.text.isBlank()) {
            newDesc = signedInUser?.description.toString()
        } else {
            newDesc = binding.etAbout.text.toString()
        }

        if (binding.etUsername.text.isBlank()) {
            newUsername = oldUsername
        } else {
            newUsername = binding.etUsername.text.toString()
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
                            newUsername,
                            newDesc,
                            downloadUrlTask.result.toString()
                        )
                        firestoreDb.collection("artifacts").document("${userId}").set(user)
                    }.addOnCompleteListener { userUpdateTask ->
                        binding.btnSubmit.isEnabled = true
                        if (!userUpdateTask.isSuccessful) {
                            Log.e(
                                TAG,
                                "Exception during Firebase operations",
                                userUpdateTask.exception
                            )
                            Toast.makeText(this, "Failed to update user...", Toast.LENGTH_SHORT).show()
                        } else {
                            firestoreDb.collection("artifacts")
                                .whereEqualTo("username", oldUsername)
                                .get()
                                .addOnSuccessListener { posts ->
                                    Log.i(TAG, "got postlist")
                                    posts.forEach { post ->
                                        Log.i(TAG, "${post.id}")
                                        firestoreDb.collection("artifacts")
                                            .document(post.id as String)
                                            .update(mapOf(
                                                "username" to newUsername
                                            ))
                                            .addOnCompleteListener {
                                                Log.i(TAG, "updated ${post.id}")
                                            }
                                    }
                                    var tagList : Array<String> = emptyArray()
                                    if (!binding.etAbout.text.isBlank()) {
                                        var tagList : Array<String> = newDesc!!.split(" ").toTypedArray()

                                        for (item in tagList) {
                                            var tag = item
                                            var value = ""
                                            if (item.contains("=")) {
                                                tag = item.split("=").toTypedArray()[0]
                                                value = item.split("=").toTypedArray()[1]
                                            }
                                            Log.i(TAG, "Tag: $tag, Value: $value")

                                            val tagVal = hashMapOf(
                                                "value" to value
                                            )
                                            val nullVal = hashMapOf(
                                                "value" to null
                                            )

                                            if (value == "") {
                                                firestoreDb.collection("tags").document(userId as String)
                                                    .collection(userId as String).document(tag).set(nullVal)
                                            } else {
                                                firestoreDb.collection("tags").document(userId as String)
                                                    .collection(userId as String).document(tag).set(tagVal)
                                            }
                                        }
                                    }
                                    Toast.makeText(this, "User profile updated!", Toast.LENGTH_SHORT).show()
                                    val profileIntent = Intent(this, ProfileActivity::class.java)
                                    profileIntent.putExtra(EXTRA_USERNAME, newUsername)
                                    startActivity(profileIntent)
                                    finish()
                                }
                        }
                    }
            }
        } else {
            if (!deleted) {
                newUrl = signedInUser?.file_url.toString()
            } else {
                newUrl = ""
            }

            val user = User(
                newUsername,
                newDesc,
                newUrl
            )
            firestoreDb.collection("artifacts").document("${userId}").set(user)
                .addOnCompleteListener { userUpdateTask ->
                    binding.btnSubmit.isEnabled = true
                    if (!userUpdateTask.isSuccessful) {
                        Log.e(
                            TAG,
                            "Exception during Firebase operations",
                            userUpdateTask.exception
                        )
                        Toast.makeText(this, "Failed to update user...", Toast.LENGTH_SHORT).show()
                    } else {
                        firestoreDb.collection("artifacts")
                            .whereEqualTo("username", oldUsername)
                            .get()
                            .addOnSuccessListener { posts ->
                                Log.i(TAG, "got postlist")
                                posts.forEach { post ->
                                    Log.i(TAG, "${post.id}")
                                    firestoreDb.collection("artifacts")
                                        .document(post.id as String)
                                        .update(mapOf(
                                            "username" to newUsername
                                        ))
                                        .addOnCompleteListener {
                                            Log.i(TAG, "updated ${post.id}")
                                        }
                                }
                                var tagList : Array<String> = emptyArray()
                                if (!binding.etAbout.text.isBlank()) {
                                    var tagList : Array<String> = newDesc!!.split(" ").toTypedArray()

                                    for (item in tagList) {
                                        var tag = item
                                        var value = ""
                                        if (item.contains("=")) {
                                            tag = item.split("=").toTypedArray()[0]
                                            value = item.split("=").toTypedArray()[1]
                                        }
                                        Log.i(TAG, "Tag: $tag, Value: $value")

                                        val tagVal = hashMapOf(
                                            "value" to value
                                        )
                                        val nullVal = hashMapOf(
                                            "value" to null
                                        )

                                        if (value == "") {
                                            firestoreDb.collection("tags").document(userId as String)
                                                .collection(userId as String).document(tag).set(nullVal)
                                        } else {
                                            firestoreDb.collection("tags").document(userId as String)
                                                .collection(userId as String).document(tag).set(tagVal)
                                        }
                                    }
                                }
                                Toast.makeText(this, "User profile updated!", Toast.LENGTH_SHORT).show()
                                val profileIntent = Intent(this, ProfileActivity::class.java)
                                profileIntent.putExtra(EXTRA_USERNAME, newUsername)
                                startActivity(profileIntent)
                                finish()
                            }
                    }
                }
        }
    }

}