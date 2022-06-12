package com.arvind.looksea

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.widget.addTextChangedListener
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivitySocialBinding
import com.arvind.looksea.models.Item
import com.google.firebase.firestore.FieldPath

private const val TAG = "SocialActivity"

class SocialActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private var userId: String? = ""
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivitySocialBinding

    private lateinit var search: MutableList<User>
    private lateinit var adapterSearch: UserAdapter
    private var searchList = mutableListOf<User>()

    private lateinit var friend: MutableList<User>
    private lateinit var adapterFriends: UserAdapter
    private lateinit var friendList: MutableList<User>

    private lateinit var request: MutableList<User>
    private lateinit var adapterRequests: UserAdapter
    private lateinit var requestList: MutableList<User>

    private fun loadFriends() {
        friend = mutableListOf()
        adapterFriends = UserAdapter(this, friend)
        binding.rvFriends.adapter = adapterFriends
        binding.rvFriends.layoutManager = LinearLayoutManager(this)
        firestoreDb.collection("links")
            .document(userId as String)
            .collection("friend")
            .get()
            .addOnSuccessListener { snapshots ->
                var allFriends = mutableListOf<String>()
                allFriends.add("null")
                snapshots.forEach { doc ->
                    allFriends.add(doc.id)
                }
                firestoreDb.collection("artifacts")
                    .whereIn(FieldPath.documentId(), allFriends)
                    .get()
                    .addOnSuccessListener { friendSnapshots ->
                        friendList = friendSnapshots.toObjects(User::class.java)
                        friend.clear()
                        friend.addAll(friendList)
                        adapterFriends.notifyDataSetChanged()

                        adapterFriends.setOnItemClickListener(object : UserAdapter.onItemClickListener {
                            override fun onItemClick(position: Int) {
                                val person = friend[position]
                                Log.i(TAG, "$person")
                                val intent = Intent(this@SocialActivity, ProfileActivity::class.java)
                                intent.putExtra(EXTRA_USERNAME, person.username)
                                startActivity(intent)
                            }
                        })
                    }
            }
    }

    private fun loadRequests() {
        request = mutableListOf()
        adapterRequests = UserAdapter(this, request)
        binding.rvRequests.adapter = adapterRequests
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        firestoreDb.collection("links").document(userId as String)
            .collection("receivedrequest")
            .get()
            .addOnSuccessListener { receivedOnly ->
                requestList = receivedOnly.toObjects((User::class.java))
                request.clear()
                request.addAll(requestList)
                firestoreDb.collection("links").document(userId as String)
                    .collection("sentrequest")
                    .get()
                    .addOnSuccessListener { requestSnapshots ->
                        requestList = requestSnapshots.toObjects((User::class.java))
                        request.addAll(requestList)
                        Log.i(TAG, "REQUESTS: $request")
                        adapterRequests.notifyDataSetChanged()

                        adapterRequests.setOnItemClickListener(object : UserAdapter.onItemClickListener {
                            override fun onItemClick(position: Int) {
                                val person = request[position]
                                Log.i(TAG, "$person")
                                val intent = Intent(this@SocialActivity, ProfileActivity::class.java)
                                intent.putExtra(EXTRA_USERNAME, person.username)
                                startActivity(intent)
                            }
                        })
                    }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialBinding.inflate(layoutInflater)
        setContentView(binding.root)
        search = mutableListOf()
        adapterSearch = UserAdapter(this, search)
        binding.rvSearch.adapter = adapterSearch
        binding.rvSearch.layoutManager = LinearLayoutManager(this)
        firestoreDb = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb.collection("artifacts")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")

                loadRequests()
                loadFriends()

                binding.etSearch.addTextChangedListener {
                    if (it.toString() == "") {
                        search.clear()
                        adapterSearch.notifyDataSetChanged()
                    } else {
                        searchList.clear()
                        firestoreDb.collection("artifacts")
                            .whereIn("type", mutableListOf("user"))
                            .get()
                            .addOnSuccessListener { querySnapshots ->
                                querySnapshots.forEach { doc ->
                                    var queryItem = doc.toObject((User::class.java))
                                    if (queryItem.username.contains(it.toString(), ignoreCase = true) && !searchList.contains(doc.toObject((User::class.java)))) {
                                        searchList.add(queryItem)
                                    }
                                }
                                search.clear()
                                search.addAll(searchList)
                                adapterSearch.notifyDataSetChanged()
                                Log.i(TAG, "zz $searchList")
                            }
                    }
                }
                adapterSearch.setOnItemClickListener(object : UserAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        val person = searchList[position]
                        Log.i(TAG, "$person")
                        val intent = Intent(this@SocialActivity, ProfileActivity::class.java)
                        intent.putExtra(EXTRA_USERNAME, person.username)
                        startActivity(intent)
                    }
                })

            }.addOnFailureListener { exception ->
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