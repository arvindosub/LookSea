package com.arvind.looksea

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityHomeBinding
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private const val TAG = "HomeActivity"
const val EXTRA_USERNAME = "EXTRA_USERNAME"
const val EXTRA_POSTTIME = "EXTRA_POSTTIME"

open class HomeActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private var userId: String? = ""
    private var currUser: User? = null
    private var currUserId: String? = ""
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: PostAdapter
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Create the layout file which represents one post - DONE
        // Create data source - DONE
        posts = mutableListOf()
        // Create the adapter
        adapter = PostAdapter(this, posts)
        // Bind the adapter and layout manager to the RV
        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)
        firestoreDb = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb.collection("users")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")

                var postsReference = firestoreDb
                    .collection("posts")
                    .limit(20)
                    .orderBy("creation_time_ms", Query.Direction.DESCENDING)

                val username = intent.getStringExtra(EXTRA_USERNAME)
                // supportActionBar?.title = username
                //filter out posts not from self or friends in home page
                if (username != null) {
                    postsReference = postsReference.whereEqualTo("user.username", username)

                    postsReference.addSnapshotListener { snapshot, exception ->
                        if (exception != null || snapshot == null) {
                            Log.e(TAG, "Exception when querying posts", exception)
                            return@addSnapshotListener
                        }
                        val postList = snapshot.toObjects(Post::class.java)
                        posts.clear()
                        posts.addAll(postList)
                        adapter.notifyDataSetChanged()
                        for (post in postList) {
                            Log.i(TAG, "Post $post")
                        }
                    }

                } else {
                    var friendList: MutableList<String> = ArrayList()
                    friendList.add(userId!!)
                    firestoreDb.collection("friendlists")
                        .document(userId as String)
                        .collection("myfriends")
                        .get()
                        .addOnSuccessListener { friends ->
                            friends.forEach { fr ->
                                if (fr != null) {
                                    friendList.add(fr.id)
                                }
                            }
                            Log.i(TAG, "Friends List: $friendList")
                            postsReference = postsReference.whereIn("user", friendList)

                            postsReference.addSnapshotListener { snapshot, exception ->
                                if (exception != null || snapshot == null) {
                                    Log.e(TAG, "Exception when querying posts", exception)
                                    return@addSnapshotListener
                                }
                                val postList = snapshot.toObjects(Post::class.java)
                                posts.clear()
                                posts.addAll(postList)
                                adapter.notifyDataSetChanged()
                                for (post in postList) {
                                    Log.i(TAG, "Post $post")
                                }
                            }
                        }
                }
                adapter.setOnItemClickListener(object : PostAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        val clickedPost = posts[position]
                        Log.i(TAG, "$clickedPost")
                        val intent = Intent(this@HomeActivity, PostActivity::class.java)
                        intent.putExtra(EXTRA_POSTTIME, clickedPost.creationTimeMs.toString())
                        startActivity(intent)
                    }
                })

            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }

        binding.fabCreate.setOnClickListener {
            val intent = Intent(this, CreateActivity::class.java)
            startActivity(intent)
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