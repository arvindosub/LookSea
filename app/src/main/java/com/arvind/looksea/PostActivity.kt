package com.arvind.looksea

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityPostBinding
import com.arvind.looksea.models.Link
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.GeoPoint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

private const val TAG = "PostActivity"

class PostActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var userId: String? = ""
    private var post: Post? = null
    private var postId: String? = null
    private var privacy: String? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityPostBinding
    private lateinit var storageReference: StorageReference
    private lateinit var comments: MutableList<Link>
    private lateinit var adapter: CommentAdapter
    private var commentList = mutableListOf<Link>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference
        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        comments = mutableListOf()
        adapter = CommentAdapter(this, comments, userId!!)
        binding.rvComments.adapter = adapter
        binding.rvComments.layoutManager = LinearLayoutManager(this)

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

                        firestoreDb.collection("links")
                            .document(postId!!)
                            .collection("commented")
                            .get()
                            .addOnSuccessListener { commentSnapshots ->
                                commentList = commentSnapshots.toObjects(Link::class.java)

                                comments.clear()
                                comments.addAll(commentList)
                                adapter.notifyDataSetChanged()

                                var updateList: MutableList<String> = ArrayList()
                                var deleteList: MutableList<String> = ArrayList()
                                var configList: MutableList<String> = ArrayList()
                                firestoreDb.collection("links")
                                    .document(userId!!)
                                    .collection("update")
                                    .get()
                                    .addOnSuccessListener { updateDocs ->
                                        updateDocs.forEach { ud ->
                                            if (ud != null) {
                                                updateList.add(ud.id)
                                            }
                                        }

                                        firestoreDb.collection("links")
                                            .document(userId!!)
                                            .collection("delete")
                                            .get()
                                            .addOnSuccessListener { deleteDocs ->
                                                deleteDocs.forEach { dd ->
                                                    if (dd != null) {
                                                        deleteList.add(dd.id)
                                                    }
                                                }

                                                firestoreDb.collection("links")
                                                    .document(userId!!)
                                                    .collection("configure")
                                                    .get()
                                                    .addOnSuccessListener { configDocs ->
                                                        configDocs.forEach { cd ->
                                                            if (cd != null) {
                                                                configList.add(cd.id)
                                                            }
                                                        }

                                                        // post = response.toObjects(Post::class.java)[0]
                                                        binding.imageView.isVisible = true
                                                        binding.videoView.isVisible = false
                                                        binding.tvPrivacy.text = "Privacy"

                                                        Log.i(TAG, "$post")
                                                        Log.i(TAG, "$updateList")
                                                        Log.i(TAG, "$deleteList")
                                                        Log.i(TAG, "$configList")

                                                        Glide.with(this).load(post?.fileUrl)
                                                            .into(binding.imageView)
                                                        binding.etDescription.hint =
                                                            post?.description.toString()
                                                        binding.tvLikes.text =
                                                            post?.likes.toString()

                                                        if (userId == post?.userId || postId in deleteList) {
                                                            binding.btnDelete.isVisible = true
                                                            binding.btnDelete.setOnClickListener {
                                                                deletePost()
                                                            }
                                                        }

                                                        if (userId == post?.userId || postId in configList) {
                                                            binding.rgPrivacy.isVisible = true
                                                            if (post?.privacy == "public") {
                                                                binding.rgbPublic.isChecked = true
                                                                privacy = "public"
                                                            } else {
                                                                binding.rgbFriends.isChecked = true
                                                                privacy = "private"
                                                            }
                                                            binding.btnSubmit.isVisible = true
                                                        }

                                                        if (userId == post?.userId || postId in updateList) {
                                                            binding.etDescription.isEnabled = true
                                                            binding.etDescription.setText(post?.description)
                                                            binding.btnSubmit.isVisible = true
                                                            binding.btnAnalyse.text = "Suggest"
                                                            binding.btnSubmit.setOnClickListener {
                                                                handleSubmitButtonClick()
                                                            }
                                                        }

                                                        binding.btnComment.setOnClickListener {
                                                            uploadComment()
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

                                                        binding.fabAccess.setOnClickListener {
                                                            handleAccessButtonClick()
                                                        }
                                                    }

                                            }
                                    }

                            }
                    }

                adapter.setOnUserClickListener(object : CommentAdapter.onUserClickListener {
                    override fun onUserClick(position: Int) {
                        val thisUser = commentList[position].owner
                        firestoreDb.collection("artifacts")
                            .document(thisUser)
                            .get()
                            .addOnSuccessListener { doc ->
                                var myUser = doc.toObject(User::class.java)
                                val intent = Intent(this@PostActivity, ProfileActivity::class.java)
                                intent.putExtra(EXTRA_USERNAME, myUser!!.username)
                                startActivity(intent)
                            }
                    }
                })

                adapter.setOnDeleteClickListener(object : CommentAdapter.onDeleteClickListener {
                    override fun onDeleteClick(position: Int) {
                        val thisUser = commentList[position].owner
                        firestoreDb.collection("links").document(thisUser)
                            .collection("commented").document(postId as String).delete()
                            .addOnSuccessListener {
                                firestoreDb.collection("links").document(postId as String)
                                    .collection("commented").document(thisUser).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this@PostActivity, "Comment Deleted!", Toast.LENGTH_SHORT).show()
                                        this@PostActivity.recreate()
                                    }
                            }
                    }
                })

                adapter.setOnCommentClickListener(object : CommentAdapter.onCommentClickListener {
                    override fun onCommentClick(position: Int) {
                        val thisUser = commentList[position].owner
                        firestoreDb.collection("artifacts")
                            .document(thisUser)
                            .get()
                            .addOnSuccessListener { doc ->
                                var myUser = doc.toObject(User::class.java)
                                val intent = Intent(this@PostActivity, ProfileActivity::class.java)
                                intent.putExtra(EXTRA_USERNAME, myUser!!.username)
                                startActivity(intent)
                            }
                    }
                })
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }
    }

    private fun uploadComment() {
        binding.btnSubmit.isEnabled = false

        var linkVal = Link(
            "${binding.etComment.text}",
            "$userId"
        )

        firestoreDb.collection("links").document(userId!!)
            .collection("commented").document(postId!!).set(linkVal)
            .addOnCompleteListener {
                firestoreDb.collection("links").document(postId!!)
                    .collection("commented").document(userId!!).set(linkVal)
                    .addOnCompleteListener { linkCreationTask ->
                        binding.btnSubmit.isEnabled = true
                        if (!linkCreationTask.isSuccessful) {
                            Log.e(TAG, "Exception during Firebase operations", linkCreationTask.exception)
                            Toast.makeText(this, "Failed to add comment...", Toast.LENGTH_SHORT).show()
                        }
                        Toast.makeText(this, "Comment Added!", Toast.LENGTH_SHORT).show()
                        this@PostActivity.recreate()
                    }
            }
    }

    private fun handleLinkButtonClick() {
        val id = postId
        val intent = Intent(this, LinkActivity::class.java)
        intent.putExtra(EXTRA_ARTIFACTID, id)
        startActivity(intent)
    }

    private fun handleAccessButtonClick() {
        val id = postId
        val intent = Intent(this, AccessActivity::class.java)
        intent.putExtra(EXTRA_ARTIFACTID, id)
        startActivity(intent)
    }

    private fun handleSubmitButtonClick() {
        if (binding.rgbFriends.isChecked) {
            privacy = "friends"
        } else {
            privacy = "public"
        }

        if (binding.etDescription.text.toString() == post?.description && post?.privacy == privacy) {
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
                post?.username.toString(),
                privacy.toString()
            )
        }
        postId?.let {
            if (editedPost != null) {
                firestoreDb.collection("artifacts").document(it)
                    .set(editedPost)
                    .addOnCompleteListener { postCreationTask ->
                        if (binding.etDescription.text.isNotBlank() && binding.etDescription.text.contains("#")) {
                            var tagList : Array<String> = binding.etDescription.text.split(" ").toTypedArray()

                            for (item in tagList) {
                                if (item.startsWith("#")) {
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
                                            .collection(postId!!).document(tag).set(nullVal)
                                    } else {
                                        firestoreDb.collection("tags").document(userId as String)
                                            .collection(postId!!).document(tag).set(tagVal)
                                    }
                                }
                            }
                        }
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
            firestoreDb.collection("links").document(userId as String)
                .collection("liked").document(postId as String)
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
                                var link = Link(
                                    "liked",
                                    "$userId"
                                )
                                firestoreDb.collection("links").document(userId as String)
                                    .collection("liked").document(postId as String).set(link)
                                    .addOnCompleteListener {
                                        firestoreDb.collection("links").document(postId as String)
                                            .collection("liked").document(userId as String).set(link)
                                    }
                            }
                    } else {
                        firestoreDb.collection("artifacts").document(postId!!)
                            .update("likes", likes-1)
                            .addOnCompleteListener {
                                Log.i(TAG, "Likes ${likes-1}")
                                Toast.makeText(this, "Unliked", Toast.LENGTH_SHORT).show()
                                firestoreDb.collection("links").document(userId as String)
                                    .collection("liked").document(postId as String).delete()
                                    .addOnCompleteListener {
                                        firestoreDb.collection("links").document(postId as String)
                                            .collection("liked").document(userId as String).delete()
                                    }
                            }
                    }
                }
        }
        this.recreate()
    }

    private fun handleAnalysis() {
        var tagString = ""
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        // Real-time contour detection
        /*val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()*/
        Glide.with(this).asBitmap().load(post?.fileUrl).into(object : CustomTarget<Bitmap?>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                val image = InputImage.fromBitmap(resource, 0)
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                val detector = FaceDetection.getClient(highAccuracyOpts)
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        var smileProb = 0.0
                        var numFaces = 0
                        for (face in faces) {
                            smileProb += face.smilingProbability!!
                            numFaces += 1
                            Log.i(TAG, "Face: $face")
                        }
                        smileProb /= numFaces
                        if (faces.isEmpty()) {
                            Log.i(TAG, "No Faces Detected")
                        } else {
                            Log.i(TAG, "Total Number of Faces: $numFaces")
                            Log.i(TAG, "Average Smile Probability: $smileProb")
                        }
                        labeler.process(image)
                            .addOnSuccessListener { labels ->
                                for (label in labels) {
                                    tagString += "#${label.text.lowercase()} "
                                    Log.i(TAG, "${label.index}. ${label.text}: ${label.confidence}")
                                }
                                tagString += "#faces=$numFaces "
                                if (smileProb >= 0.5) {
                                    tagString += "#emotion=happy "
                                } else if (smileProb < 0.5 && smileProb > -0.5) {
                                    tagString += "#emotion=serious "
                                } else if (smileProb <= -0.5) {
                                    tagString += "#emotion=sad "
                                }
                                tagString += "#location=${post?.location.toString()}"
                                //tagString = tagString.dropLast(1)
                                Log.i(TAG, tagString)
                                if (binding.etDescription.isEnabled) {
                                    binding.etDescription.setText(tagString)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "$e")
                            }
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
        var likeList = mutableListOf<String>()
        var commentList = mutableListOf<String>()

        firestoreDb.collection("artifacts").document(pid).delete().addOnCompleteListener {
            firestoreDb.collection("links").document(userId as String).collection("owned").document(pid).delete()
            firestoreDb.collection("links").document(pid).collection("owned").document(userId as String).delete()

            firestoreDb.collection("links").document(userId as String).collection("liked").document(pid).delete()
            firestoreDb.collection("links").document(pid).collection("liked").document(userId as String).delete()

            firestoreDb.collection("tags").document(userId as String).collection(pid).get().addOnSuccessListener { tags ->
                tags.forEach { tag ->
                    firestoreDb.collection("tags").document(userId as String).collection(pid).document(tag.id).delete()
                }
                firestoreDb.collection("links").document(pid).collection("linked").get().addOnSuccessListener { linkSnapshots ->
                    linkSnapshots.forEach { link ->
                        linkList.add(link.id)
                        firestoreDb.collection("links").document(pid).collection("linked").document(link.id).delete()
                    }
                    Log.i(TAG, "linkList is: $linkList")

                    linkList.forEach { item ->
                        firestoreDb.collection("links").document(item).collection("linked").document(pid).delete()
                    }
                }.addOnSuccessListener {
                    firestoreDb.collection("links").document(pid).collection("commented").get().addOnSuccessListener { commentSnapshots ->
                        commentSnapshots.forEach { comment ->
                            commentList.add(comment.id)
                            firestoreDb.collection("links").document(pid).collection("commented")
                                .document(comment.id).delete()
                        }
                        commentList.forEach { item ->
                            firestoreDb.collection("links").document(item).collection("commented")
                                .document(pid).delete()
                        }

                        firestoreDb.collection("links").document(pid).collection("liked").get().addOnSuccessListener { likeSnapshots ->
                            likeSnapshots.forEach { like ->
                                likeList.add(like.id)
                                firestoreDb.collection("links").document(pid)
                                    .collection("liked")
                                    .document(like.id).delete()
                            }
                            Log.i(TAG, "likeList is: $likeList")

                            likeList.forEach { item ->
                                firestoreDb.collection("links").document(item)
                                    .collection("liked")
                                    .document(pid).delete()
                            }
                            fileRef.delete().addOnCompleteListener {
                                Toast.makeText(this, "Deleted post...", Toast.LENGTH_SHORT)
                                    .show()
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
    }

}