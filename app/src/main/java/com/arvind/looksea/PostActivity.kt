package com.arvind.looksea

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.FileUtils
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.core.view.isVisible
import com.arvind.looksea.databinding.ActivityPostBinding
import com.arvind.looksea.models.Link
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.toObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import org.json.JSONObject
import java.io.File
import java.nio.file.Paths

private const val TAG = "PostActivity"

class PostActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var userId: String? = ""
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
        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("artifacts")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)

                val posttime = intent.getStringExtra(EXTRA_POSTTIME)
                firestoreDb.collection("artifacts")
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
                        binding.tvLikes.text = post?.likes.toString()
                        if (userId == post?.userId) {
                            binding.etDescription.isEnabled = true
                            binding.btnSubmit.isVisible = true
                            binding.btnDelete.isVisible = true
                            binding.btnAnalyse.text = "Suggest"
                            binding.btnSubmit.setOnClickListener {
                                handleSubmitButtonClick()
                            }
                            binding.btnDelete.setOnClickListener {
                                deletePost()
                            }
                        }

                        binding.btnAnalyse.setOnClickListener {
                            handleAnalysis()
                        }

                        binding.btnLike.setOnClickListener {
                            likePost()
                        }

                        binding.fabLink.setOnClickListener {
                            handleLinkButtonClick()
                        }

                    }
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }
    }

    private fun handleLinkButtonClick() {
        val id = postId
        val intent = Intent(this, LinkActivity::class.java)
        intent.putExtra(EXTRA_ARTIFACTID, id)
        startActivity(intent)
    }

    private fun handleSubmitButtonClick() {
        if (binding.etDescription.text.isBlank()) {
            Toast.makeText(this, "No changes made...", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false

        val editedPost = post?.creationTimeMs?.let {
            Post(
                post?.creationTimeMs!!,
                binding.etDescription.text.toString(),
                post?.type.toString(),
                post?.likes!!,
                post?.fileUrl.toString(),
                post?.location as GeoPoint,
                post?.userId.toString(),
                post?.username.toString()
            )
        }
        postId?.let {
            if (editedPost != null) {
                firestoreDb.collection("artifacts").document(it)
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

    private fun likePost() {
        val likes = post?.likes!!
        postId?.let {
            var isLiked = true
            firestoreDb.collection("likedposts").document(userId as String)
                .collection("artifacts").document(postId as String)
                .get()
                .addOnSuccessListener { likeSnapshot ->
                    Log.i(TAG, "Liked? ${likeSnapshot}")
                    if (likeSnapshot.data == null) {
                        isLiked = false
                    }
                    Log.i(TAG, "isLiked? ${isLiked}")

                    if (!isLiked) {
                        firestoreDb.collection("artifacts").document(postId!!)
                            .update("likes", likes+1)
                            .addOnCompleteListener {
                                Log.i(TAG, "Likes ${likes+1}")
                                Toast.makeText(this, "Liked", Toast.LENGTH_SHORT).show()
                                firestoreDb.collection("likedposts").document(userId as String)
                                    .collection("artifacts").document(postId as String).set(post!!)
                            }
                    } else {
                        firestoreDb.collection("artifacts").document(postId!!)
                            .update("likes", likes-1)
                            .addOnCompleteListener {
                                Log.i(TAG, "Likes ${likes-1}")
                                Toast.makeText(this, "Unliked", Toast.LENGTH_SHORT).show()
                                firestoreDb.collection("likedposts").document(userId as String)
                                    .collection("artifacts").document(postId as String).delete()
                            }
                    }
                }
        }
        this.recreate()
    }

    private fun handleAnalysis() {
        var tagString = ""
        Glide.with(this).asBitmap().load(post?.fileUrl).into(object : CustomTarget<Bitmap?>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                val image = InputImage.fromBitmap(resource, 0)
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        if (userId == post?.userId) {
                            for (label in labels) {
                                tagString += "#${label.text.lowercase()} "
                                Log.i(TAG, "${label.index}. ${label.text}: ${label.confidence}")
                            }
                            tagString = tagString.dropLast(1)
                            Log.i(TAG, tagString)
                            binding.etDescription.setText(tagString)
                        } else {
                            for (label in labels) {
                                Toast.makeText(this@PostActivity, "${label.index}. ${label.text}: ${label.confidence}", Toast.LENGTH_SHORT).show()
                                Log.i(TAG, "${label.index}. ${label.text}: ${label.confidence}")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "$e")
                    }
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }

    private fun deletePost() {
        val pid = postId as String
        val item = post
        var fpath = ""
        if (item?.type.toString() == "image") {
            fpath = "images/${item?.creationTimeMs}-photo.jpg"
        } else if (item?.type.toString() == "video") {
            fpath = "videos/${item?.creationTimeMs}-video.mp4"
        } else {
            fpath = "audio/${item?.creationTimeMs}-audio.mp3"
        }
        val fileRef = storageReference.child(fpath)
        Log.i(TAG, "fpath is: ${fpath}")
        var linkList = mutableListOf<String>()

        firestoreDb.collection("artifacts").document(pid).delete().addOnCompleteListener {
            firestoreDb.collection("tags").document(userId as String).collection(pid).get().addOnSuccessListener { tags ->
                tags.forEach { tag ->
                    firestoreDb.collection("tags").document(userId as String).collection(pid).document(tag.id).delete()
                }
                firestoreDb.collection("links").document(pid).collection("link").get().addOnSuccessListener { linkSnapshots ->
                    linkSnapshots.forEach { link ->
                        linkList.add(link.id)
                        firestoreDb.collection("links").document(pid).collection("link").document(link.id).delete()
                    }
                    Log.i(TAG, "linkList is: $linkList")

                    linkList.forEach { item ->
                        firestoreDb.collection("links").document(item).collection("link").document(pid).delete()
                    }
                    fileRef.delete().addOnCompleteListener {
                        Toast.makeText(this, "Deleted post...", Toast.LENGTH_SHORT).show()
                        finish()
                        val intent = Intent(this, ProfileActivity::class.java)
                        intent.putExtra(EXTRA_USERNAME, signedInUser?.username)
                        startActivity(intent)
                    }
                }
            }
        }
    }

}