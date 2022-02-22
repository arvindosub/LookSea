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
import com.arvind.looksea.models.Post

private const val TAG = "SearchActivity"

class SearchActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivitySearchBinding

    private lateinit var search: MutableList<Post>
    private lateinit var adapterSearch: PostAdapter
    private lateinit var searchList: MutableList<Post>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        search = mutableListOf()
        adapterSearch = PostAdapter(this, search)
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
                        firestoreDb.collection("posts")
                            .whereGreaterThanOrEqualTo("description", it.toString())
                            .get()
                            .addOnSuccessListener { querySnapshots ->
                                searchList = querySnapshots.toObjects((Post::class.java))
                                Log.i(TAG, "$searchList")
                                search.clear()
                                search.addAll(searchList)
                                adapterSearch.notifyDataSetChanged()
                            }
                    }
                }
                adapterSearch.setOnItemClickListener(object : PostAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        val post= searchList[position]
                        Log.i(TAG, "$post")
                        val intent = Intent(this@SearchActivity, PostActivity::class.java)
                        intent.putExtra(EXTRA_POSTTIME, post.creationTimeMs.toString())
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