package com.arvind.looksea

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.isVisible
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivitySearchBinding
import com.arvind.looksea.models.Item
import com.google.firebase.firestore.FieldPath

private const val TAG = "SearchActivity"

class SearchActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivitySearchBinding
    private lateinit var search: MutableList<Item>
    private lateinit var adapterSearch: ItemAdapter
    private var searchList = mutableListOf<Item>()
    private var searchIdList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        search = mutableListOf()
        adapterSearch = ItemAdapter(this, search)
        binding.rvSearch.adapter = adapterSearch
        binding.rvSearch.layoutManager = LinearLayoutManager(this)
        var userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("artifacts")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")

                binding.etSearch.addTextChangedListener {
                    if (it.toString() == "") {
                        searchList.clear()
                        searchIdList.clear()
                        search.clear()
                        adapterSearch.notifyDataSetChanged()
                    } else if (it.toString().contains("/friendswith ")) {
                        searchList.clear()
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
                                            var idList = mutableListOf<String>()
                                            querySnapshots.forEach { doc ->
                                                idList.add(doc.id)
                                            }
                                            firestoreDb.collection("artifacts")
                                                .whereIn(FieldPath.documentId(), idList)
                                                .get()
                                                .addOnSuccessListener { friendSnapshots ->
                                                    searchList = friendSnapshots.toObjects((Item::class.java))
                                                    friendSnapshots.forEach { friend ->
                                                        searchIdList.add(friend.id)
                                                    }
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
                        searchIdList.clear()
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
                                            var idList = mutableListOf<String>()
                                            querySnapshots.forEach { doc ->
                                                idList.add(doc.id)
                                            }
                                            firestoreDb.collection("artifacts")
                                                .whereIn(FieldPath.documentId(), idList)
                                                .get()
                                                .addOnSuccessListener { docSnapshots ->
                                                    searchList = docSnapshots.toObjects((Item::class.java))
                                                    docSnapshots.forEach { doc ->
                                                        searchIdList.add(doc.id)
                                                    }
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
                        searchIdList.clear()
                        var searchType = it.toString().split(' ')[0].drop(1)
                        var searchTerm = it.toString().split(' ')[1]
                        if (searchType == "user") {
                            firestoreDb.collection("artifacts")
                                .whereIn("type", mutableListOf(searchType))
                                .whereGreaterThanOrEqualTo("username", searchTerm)
                                .get()
                                .addOnSuccessListener { querySnapshots ->
                                    searchList = querySnapshots.toObjects((Item::class.java))
                                    querySnapshots.forEach { query ->
                                        searchIdList.add(query.id)
                                    }
                                    search.clear()
                                    search.addAll(searchList)
                                    adapterSearch.notifyDataSetChanged()
                                    Log.i(TAG, "zz $searchList")
                                }
                        } else if (searchTerm != "") {
                            searchList.clear()
                            firestoreDb.collection("artifacts")
                                .whereIn("type", mutableListOf(searchType))
                                .get()
                                .addOnSuccessListener { querySnapshots ->
                                    querySnapshots.forEach { doc ->
                                        var descList = doc.toObject((Item::class.java)).description.split(' ')
                                        for (desc in descList) {
                                            if (desc.contains(searchTerm, ignoreCase = true) && !searchList.contains(doc.toObject((Item::class.java)))) {
                                                searchList.add(doc.toObject((Item::class.java)))
                                                searchIdList.add(doc.id)
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
                        searchIdList.clear()
                        firestoreDb.collection("artifacts")
                            .whereIn("type", mutableListOf("user", "image", "video", "text", "audio"))
                            .get()
                            .addOnSuccessListener { querySnapshots ->
                                querySnapshots.forEach { doc ->
                                    var queryItem = doc.toObject((Item::class.java))
                                    for (desc in queryItem.description.split(' ')) {
                                        if (desc.contains(it.toString(), ignoreCase = true) && !searchList.contains(doc.toObject((Item::class.java)))) {
                                            searchList.add(queryItem)
                                            searchIdList.add(doc.id)
                                        }
                                    }
                                    if (queryItem.username.toString().contains(it.toString(), ignoreCase = true) && !searchList.contains(doc.toObject((Item::class.java)))) {
                                        searchList.add(queryItem)
                                        searchIdList.add(doc.id)
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
                        val item = searchList[position]
                        Log.i(TAG, "$item")
                        if (item.type == "user") {
                            val intent = Intent(this@SearchActivity, ProfileActivity::class.java)
                            intent.putExtra(EXTRA_USERNAME, item.username)
                            startActivity(intent)
                        } else {
                            if (item.privacy == "public" || item.userId == userId) {
                                val intent = Intent(this@SearchActivity, PostActivity::class.java)
                                intent.putExtra(EXTRA_POSTTIME, item.creationTimeMs.toString())
                                startActivity(intent)
                            } else {
                                var allFriends = mutableListOf<String>()
                                var readList: MutableList<String> = ArrayList()
                                firestoreDb.collection("links")
                                    .document(item.userId as String)
                                    .collection("friend")
                                    .get()
                                    .addOnSuccessListener { friendSnapshots ->
                                        friendSnapshots.forEach { doc ->
                                            allFriends.add(doc.id)
                                        }

                                        firestoreDb.collection("links")
                                            .document(userId!!)
                                            .collection("read")
                                            .get()
                                            .addOnSuccessListener { updateDocs ->
                                                updateDocs.forEach { rd ->
                                                    if (rd != null) {
                                                        readList.add(rd.id)
                                                    }
                                                }

                                                Log.i(TAG, "$readList")

                                                if (userId in allFriends || searchIdList[position] in readList) {
                                                    val intent = Intent(this@SearchActivity, PostActivity::class.java)
                                                    intent.putExtra(EXTRA_POSTTIME, item.creationTimeMs.toString())
                                                    startActivity(intent)
                                                } else {
                                                    Toast.makeText(this@SearchActivity, "You do not have access to view this post!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                            }
                        }
                    }
                })
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_profile) {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra(EXTRA_USERNAME, signedInUser?.username)
            startActivity(intent)
        }

        if (item.itemId == R.id.menu_home) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        if (item.itemId == R.id.menu_social) {
            val intent = Intent(this, SocialActivity::class.java)
            startActivity(intent)
        }

        if (item.itemId == R.id.menu_search) {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        if (item.itemId == R.id.menu_survey) {
            val intent = Intent(this, SurveyActivity::class.java)
            startActivity(intent)
        }

        if (item.itemId == R.id.menu_logout) {
            logout()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun logout() {
        Log.i(TAG, "Logging out...")
        FirebaseAuth.getInstance().signOut()
        val logoutIntent = Intent(this, LoginActivity::class.java)
        logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(logoutIntent)
    }
}