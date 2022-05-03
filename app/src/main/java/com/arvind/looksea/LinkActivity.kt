package com.arvind.looksea

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityLinkBinding
import com.arvind.looksea.models.Item
import com.arvind.looksea.models.Link
import com.google.firebase.firestore.FieldPath

private const val TAG = "LinkActivity"

class LinkActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var userId: String? = ""
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityLinkBinding
    private lateinit var search: MutableList<Item>
    private lateinit var adapterSearch: ItemAdapter
    private var searchList = mutableListOf<Item>()
    private var idList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        search = mutableListOf()
        adapterSearch = ItemAdapter(this, search)
        binding.rvSearch.adapter = adapterSearch
        binding.rvSearch.layoutManager = LinearLayoutManager(this)

        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("artifacts")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")

                binding.etSearch.addTextChangedListener {
                    if (it.toString() == "") {
                        searchList.clear()
                        idList.clear()
                        search.clear()
                        adapterSearch.notifyDataSetChanged()
                    } else if (it.toString().contains("/friendswith ")) {
                        searchList.clear()
                        idList.clear()
                        var searchTerm = it.toString().split(' ')[1]
                        firestoreDb.collection("artifacts")
                            .whereEqualTo("type", "user")
                            .whereEqualTo("username", searchTerm)
                            .get()
                            .addOnSuccessListener { userSnapshots ->
                                var searchUser = ""
                                userSnapshots.forEach { doc ->
                                    searchUser = doc.id
                                }

                                if (searchUser != "") {
                                    firestoreDb.collection("links")
                                        .document(searchUser)
                                        .collection("friend")
                                        .get()
                                        .addOnSuccessListener { querySnapshots ->
                                            querySnapshots.forEach { doc ->
                                                idList.add(doc.id)
                                            }
                                            firestoreDb.collection("artifacts")
                                                .whereIn(FieldPath.documentId(), idList)
                                                .get()
                                                .addOnSuccessListener { friendSnapshots ->
                                                    searchList = friendSnapshots.toObjects((Item::class.java))
                                                    search.clear()
                                                    search.addAll(searchList)
                                                    adapterSearch.notifyDataSetChanged()
                                                    Log.i(TAG, "zz $searchList")
                                                }

                                        }
                                }
                            }
                    } else if (it.toString().contains("/likedby ")) {
                        searchList.clear()
                        idList.clear()
                        var searchTerm = it.toString().split(' ')[1]
                        firestoreDb.collection("artifacts")
                            .whereEqualTo("type", "user")
                            .whereEqualTo("username", searchTerm)
                            .get()
                            .addOnSuccessListener { userSnapshots ->
                                var searchUser = ""
                                userSnapshots.forEach { doc ->
                                    searchUser = doc.id
                                }

                                if (searchUser != "") {
                                    firestoreDb.collection("links")
                                        .document(searchUser)
                                        .collection("liked")
                                        .get()
                                        .addOnSuccessListener { querySnapshots ->
                                            querySnapshots.forEach { doc ->
                                                idList.add(doc.id)
                                            }
                                            firestoreDb.collection("artifacts")
                                                .whereIn(FieldPath.documentId(), idList)
                                                .get()
                                                .addOnSuccessListener { friendSnapshots ->
                                                    searchList = friendSnapshots.toObjects((Item::class.java))
                                                    search.clear()
                                                    search.addAll(searchList)
                                                    adapterSearch.notifyDataSetChanged()
                                                    Log.i(TAG, "zz $searchList")
                                                }
                                        }
                                }
                            }
                    } else if (it.toString()[0].toString() == "/" && it.toString().contains(" ")) {
                        searchList.clear()
                        idList.clear()
                        var searchType = it.toString().split(' ')[0].drop(1)
                        var searchTerm = it.toString().split(' ')[1]
                        if (searchType == "user") {
                            firestoreDb.collection("artifacts")
                                .whereIn("type", mutableListOf(searchType))
                                .whereGreaterThanOrEqualTo("username", searchTerm)
                                .get()
                                .addOnSuccessListener { querySnapshots ->
                                    searchList = querySnapshots.toObjects((Item::class.java))
                                    search.clear()
                                    search.addAll(searchList)
                                    adapterSearch.notifyDataSetChanged()
                                    Log.i(TAG, "zz $searchList")
                                    querySnapshots.forEach { doc ->
                                        idList.add(doc.id)
                                    }
                                }
                        } else if (searchTerm != "") {
                            searchList.clear()
                            idList.clear()
                            firestoreDb.collection("artifacts")
                                .whereIn("type", mutableListOf(searchType))
                                .get()
                                .addOnSuccessListener { querySnapshots ->
                                    querySnapshots.forEach { doc ->
                                        var descList = doc.toObject((Item::class.java)).description.split(' ')
                                        for (desc in descList) {
                                            if (desc.contains(searchTerm, ignoreCase = true) && !searchList.contains(doc.toObject((Item::class.java)))) {
                                                searchList.add(doc.toObject((Item::class.java)))
                                                idList.add(doc.id)
                                            }
                                        }
                                    }
                                    search.clear()
                                    search.addAll(searchList)
                                    adapterSearch.notifyDataSetChanged()
                                    Log.i(TAG, "zz $searchList")
                                }
                        }
                    } else {
                        searchList.clear()
                        idList.clear()
                        firestoreDb.collection("artifacts")
                            .whereIn("type", mutableListOf("user", "image", "video", "text", "audio"))
                            .get()
                            .addOnSuccessListener { querySnapshots ->
                                querySnapshots.forEach { doc ->
                                    var queryItem = doc.toObject((Item::class.java))
                                    for (desc in queryItem.description.split(' ')) {
                                        if (desc.contains(it.toString(), ignoreCase = true) && !searchList.contains(doc.toObject((Item::class.java)))) {
                                            searchList.add(queryItem)
                                            idList.add(doc.id)
                                        }
                                    }
                                    if (queryItem.username.toString().contains(it.toString(), ignoreCase = true) && !searchList.contains(doc.toObject((Item::class.java)))) {
                                        searchList.add(queryItem)
                                        idList.add(doc.id)
                                    }
                                }
                                search.clear()
                                search.addAll(searchList)
                                adapterSearch.notifyDataSetChanged()
                                Log.i(TAG, "zz $searchList")
                            }
                    }
                }
                adapterSearch.setOnItemClickListener(object : ItemAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        val artifactId = idList[position]
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
        binding.btnSubmit.isEnabled = false
        var artifactId = intent.getStringExtra(EXTRA_ARTIFACTID)
        var searchId = binding.etLinkedItem.text.toString()
        var linkName = binding.etLinkName.text.toString()
        var linkCollName = "linked"
        if (searchId.length == 28 || artifactId!!.length == 28) {
            linkCollName = "liked"
        }
        if (binding.etLinkName.text.isBlank() || binding.etLinkedItem.text.isBlank()) {
            Toast.makeText(this, "Using default link names.", Toast.LENGTH_SHORT).show()
            linkName = "default"
            Log.i(TAG, "${searchId.length}, ${artifactId!!.length}")
            if (searchId.length == 28 || artifactId!!.length == 28) {
                linkName = "liked"
            }
        }
        var linkVal = Link(
            linkName,
            "$userId"
        )

        firestoreDb.collection("links").document(artifactId as String)
            .collection("$linkCollName").document(searchId).set(linkVal)
            .addOnCompleteListener {
                artifactId = searchId
                searchId = intent.getStringExtra(EXTRA_ARTIFACTID).toString()
                var linkVal = Link(
                    linkName,
                    "$userId"
                )
                firestoreDb.collection("links").document(artifactId as String)
                    .collection("$linkCollName").document(searchId).set(linkVal)
                    .addOnCompleteListener { linkCreationTask ->
                        if (artifactId!!.length == 28) {
                            firestoreDb.collection("artifacts").document(searchId).get()
                                .addOnSuccessListener { postSnapshot ->
                                    var post = postSnapshot.toObject(Post::class.java)
                                    firestoreDb.collection("artifacts").document(searchId).update("likes", post!!.likes+1)
                                }
                        } else if (searchId.length == 28) {
                            firestoreDb.collection("artifacts").document(artifactId!!).get()
                                .addOnSuccessListener { postSnapshot ->
                                    var post = postSnapshot.toObject(Post::class.java)
                                    firestoreDb.collection("artifacts").document(artifactId!!).update("likes", post!!.likes+1)
                                }
                        }
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