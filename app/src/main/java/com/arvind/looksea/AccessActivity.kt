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
import com.arvind.looksea.databinding.ActivityAccessBinding
import com.arvind.looksea.models.Item
import com.arvind.looksea.models.Link
import com.google.firebase.firestore.FieldPath

private const val TAG = "AccessActivity"

class AccessActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var userId: String? = ""
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityAccessBinding
    private lateinit var search: MutableList<Item>
    private lateinit var adapterSearch: ItemAdapter
    private var searchList = mutableListOf<Item>()
    private var idList = mutableListOf<String>()
    private var usernameList = mutableListOf<String>()
    private var selUserId: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccessBinding.inflate(layoutInflater)
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
                    } else {
                        searchList.clear()
                        idList.clear()
                        firestoreDb.collection("artifacts")
                            .whereEqualTo("type", "user")
                            .get()
                            .addOnSuccessListener { querySnapshots ->
                                querySnapshots.forEach { doc ->
                                    var queryItem = doc.toObject((Item::class.java))
                                    if (queryItem.username.toString().contains(it.toString(), ignoreCase = true) && !searchList.contains(doc.toObject((Item::class.java)))) {
                                        searchList.add(queryItem)
                                        idList.add(doc.id)
                                        usernameList.add(queryItem.username.toString())
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
                        selUserId = idList[position]
                        binding.tvSelectedUser.setText(usernameList[position])
                        Log.i(TAG, "${usernameList[position]}")
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
        var accessCode = binding.etAccessCode.text.toString()
        accessCode = accessCode.replace("\\s".toRegex(), "")
        var codeList = accessCode.split('/')
        Log.i(TAG, "$codeList")
        if (("read" in codeList) && ("update" in codeList) && ("delete" in codeList) && (codeList.size == 4)) {
            firestoreDb.collection("links").document(selUserId as String)
                .collection("read").document(artifactId as String).set(Link("read","$userId"))
            firestoreDb.collection("links").document(selUserId as String)
                .collection("update").document(artifactId).set(Link("update","$userId"))
            firestoreDb.collection("links").document(selUserId as String)
                .collection("delete").document(artifactId).set(Link("delete","$userId"))
        } else if (("read" in codeList) && ("update" in codeList) && (codeList.size == 3)) {
            firestoreDb.collection("links").document(selUserId as String)
                .collection("read").document(artifactId as String).set(Link("read","$userId"))
            firestoreDb.collection("links").document(selUserId as String)
                .collection("update").document(artifactId).set(Link("update","$userId"))
        } else if (("read" in codeList) && (codeList.size == 2)) {
            firestoreDb.collection("links").document(selUserId as String)
                .collection("read").document(artifactId as String).set(Link("read","$userId"))
        }
        binding.btnSubmit.isEnabled = true
        Toast.makeText(this, "Access Configured!", Toast.LENGTH_SHORT).show()
        finish()

    }

}