package com.arvind.looksea

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
    private lateinit var searchList: MutableList<Item>

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
                                                    search.clear()
                                                    search.addAll(searchList)
                                                    adapterSearch.notifyDataSetChanged()
                                                    Log.i(TAG, "zz $searchList")
                                                }

                                        }
                                }
                            }
                    }
                    else {
                        firestoreDb.collection("artifacts")
                            .whereIn("type", mutableListOf("image", "video", "audio", "text", "survey", "user"))
                            .whereGreaterThanOrEqualTo("description", "#" + it.toString())
                            .get()
                            .addOnSuccessListener { querySnapshots ->
                                searchList = querySnapshots.toObjects((Item::class.java))
                                search.clear()
                                search.addAll(searchList)
                                firestoreDb.collection("artifacts")
                                    .whereGreaterThanOrEqualTo("username", it.toString())
                                    .get()
                                    .addOnSuccessListener { query2Snapshots ->
                                        searchList = query2Snapshots.toObjects((Item::class.java))
                                        search.addAll(searchList)
                                        adapterSearch.notifyDataSetChanged()
                                        Log.i(TAG, "zz $searchList")
                                    }
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
                            val intent = Intent(this@SearchActivity, PostActivity::class.java)
                            intent.putExtra(EXTRA_POSTTIME, item.creationTimeMs.toString())
                            startActivity(intent)
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