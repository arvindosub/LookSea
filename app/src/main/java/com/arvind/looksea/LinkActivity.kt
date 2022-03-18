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
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityLinkBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.GeoPoint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.nio.file.Paths

private const val TAG = "LinkActivity"

class LinkActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var userId: String? = ""
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityLinkBinding
    private lateinit var search: MutableList<Post>
    private lateinit var adapterSearch: PostAdapter
    private lateinit var searchList: MutableList<Post>
    private var idList: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        search = mutableListOf()
        adapterSearch = PostAdapter(this, search)
        binding.rvSearch.adapter = adapterSearch
        binding.rvSearch.layoutManager = LinearLayoutManager(this)

        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("artifacts")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)

                binding.etSearch.addTextChangedListener {
                    if (it.toString() == "") {
                        search.clear()
                        adapterSearch.notifyDataSetChanged()
                    } else {
                        firestoreDb.collection("artifacts")
                            .whereGreaterThanOrEqualTo("description", "#"+it.toString())
                            .get()
                            .addOnSuccessListener { querySnapshots ->
                                searchList = querySnapshots.toObjects((Post::class.java))
                                querySnapshots.forEach { doc ->
                                    idList.add(doc.id)
                                }
                                Log.i(TAG, "$idList")
                                Log.i(TAG, "$searchList")
                                search.clear()
                                search.addAll(searchList)
                                adapterSearch.notifyDataSetChanged()
                            }
                    }
                }
                adapterSearch.setOnItemClickListener(object : PostAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        val artifactId= idList[position]
                        binding.etLinkedItem.setText(artifactId)
                        Log.i(TAG, "$artifactId")
                    }
                })

                binding.btnSubmit.setOnClickListener {
                    handleSubmitButtonClick()
                }

            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }
    }

    private fun handleSubmitButtonClick() {
        if (binding.etLinkName.text.isBlank() || binding.etLinkedItem.text.isBlank()) {
            Toast.makeText(this, "Missing link name/linked item...", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        val postId = intent.getStringExtra(EXTRA_ARTIFACTID)
        val linkName = binding.etLinkName.text.toString()
        val searchId = binding.etLinkedItem.text.toString()

        firestoreDb.collection("links").document(userId as String)
            .collection(postId as String).document(linkName).set(hashMapOf("0" to "0"))
            .addOnCompleteListener {
                firestoreDb.collection("links").document(userId as String)
                    .collection(postId as String).document(linkName).get()
                    .addOnSuccessListener { document ->
                        var ind = document.data!!.size
                        Log.i(TAG, "$ind")
                        val linkVal = hashMapOf(
                            "$ind" to searchId
                        )

                        firestoreDb.collection("links").document(userId as String)
                            .collection(postId as String).document(linkName).set(linkVal)
                            .addOnCompleteListener { linkCreationTask ->
                                binding.btnSubmit.isEnabled = true
                                if (!linkCreationTask.isSuccessful) {
                                    Log.e(TAG, "Exception during Firebase operations", linkCreationTask.exception)
                                    Toast.makeText(this, "Failed to link post...", Toast.LENGTH_SHORT).show()
                                }
                                Toast.makeText(this, "Post linked!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }

            }

    }

}