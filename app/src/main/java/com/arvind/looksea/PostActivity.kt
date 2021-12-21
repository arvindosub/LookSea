package com.arvind.looksea

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.core.view.isVisible
import com.arvind.looksea.databinding.ActivityPostBinding
import com.bumptech.glide.Glide
import com.google.firebase.firestore.GeoPoint

private const val TAG = "PostActivity"

class PostActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var post: Post? = null
    private var postId: String? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityPostBinding
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)

                val posttime = intent.getStringExtra(EXTRA_POSTTIME)
                firestoreDb.collection("posts")
                    .whereEqualTo("creation_time_ms", posttime?.toLong())
                    .get()
                    .addOnSuccessListener { response ->
                        response.forEach { doc ->
                            postId = doc.id
                            post = doc.toObject(Post::class.java)
                        }
                        // post = response.toObjects(Post::class.java)[0]
                        binding.imageView.isVisible = true
                        binding.videoView.isVisible = false

                        Log.i(TAG, "$post")

                        Glide.with(this).load(post?.fileUrl).into(binding.imageView)
                        binding.etDescription.hint = post?.description.toString()
                        if (signedInUser == post?.user) {
                            binding.etDescription.isEnabled = true
                            binding.btnSubmit.isVisible = true
                            binding.btnSubmit.setOnClickListener {
                                handleSubmitButtonClick()
                            }
                        }
                    }
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }
    }

    private fun handleSubmitButtonClick() {
        if (binding.etDescription.text.isBlank()) {
            Toast.makeText(this, "No changes made...", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false

        val editedPost = post?.creationTimeMs?.let {
            Post(
                post?.filename.toString(),
                binding.etDescription.text.toString(),
                post?.type.toString(),
                post?.fileUrl.toString(),
                it,
                post?.location as GeoPoint,
                post?.user
            )
        }
        postId?.let {
            if (editedPost != null) {
                firestoreDb.collection("posts").document(it)
                    .set(editedPost)
                    .addOnCompleteListener { postCreationTask ->
                        binding.btnSubmit.isEnabled = true
                        if (!postCreationTask.isSuccessful) {
                            Log.e(TAG, "Exception during Firebase operations", postCreationTask.exception)
                            Toast.makeText(this, "Failed to update post...", Toast.LENGTH_SHORT).show()
                        }

                        Toast.makeText(this, "Post updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
        }
    }
}