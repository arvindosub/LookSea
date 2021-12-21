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

private const val TAG = "SearchActivity"

class SearchActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivitySearchBinding

    private lateinit var search: MutableList<User>
    private lateinit var adapterSearch: UserAdapter
    private lateinit var searchList: MutableList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Create the layout file which represents one post - DONE
        // Create data source - DONE
        search = mutableListOf()
        // Create the adapter
        adapterSearch = UserAdapter(this, search)
        // Bind the adapter and layout manager to the RV
        binding.rvSearch.adapter = adapterSearch
        binding.rvSearch.layoutManager = LinearLayoutManager(this)

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")

                binding.etSearch.addTextChangedListener {
                    if (it.toString() == "") {
                        search.clear()
                        adapterSearch.notifyDataSetChanged()
                    } else {
                        firestoreDb.collection("users")
                            .whereGreaterThanOrEqualTo("username", it.toString())
                            .get()
                            .addOnSuccessListener { querySnapshots ->
                                searchList = querySnapshots.toObjects((User::class.java))
                                Log.i(TAG, "$searchList")
                                search.clear()
                                search.addAll(searchList)
                                adapterSearch.notifyDataSetChanged()
                            }
                    }
                }
                adapterSearch.setOnItemClickListener(object : UserAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        val person = searchList[position]
                        Log.i(TAG, "$person")
                        val intent = Intent(this@SearchActivity, ProfileActivity::class.java)
                        intent.putExtra(EXTRA_USERNAME, person.username)
                        startActivity(intent)
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