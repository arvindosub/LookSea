package com.arvind.looksea

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityProfileBinding
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private const val TAG = "ProfileActivity"
class ProfileActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private var currUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: PostsAdapter
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        posts = mutableListOf()
        adapter = PostsAdapter(this, posts)
        binding.profilePosts.adapter = adapter
        binding.profilePosts.layoutManager = LinearLayoutManager(this)

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }

        var postsReference = firestoreDb
            .collection("posts")
            .limit(20)
            .orderBy("creation_time_ms", Query.Direction.DESCENDING)

        val username = intent.getStringExtra(EXTRA_USERNAME)
        if (username != null) {
            binding.profileName.text = username
            postsReference = postsReference.whereEqualTo("user.username", username)

            firestoreDb.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    currUser = userSnapshot.toObjects((User::class.java))[0]
                    binding.profileAge.text = "age:   " + currUser?.age.toString()
                    binding.profileDescription.text ="about me:   " + currUser?.description
                    Glide.with(applicationContext).load(currUser?.picture).into(binding.profilePicture)
                    Log.i(TAG, "Current User: $currUser")
                }
        }

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
            val intent = Intent(this, PostsActivity::class.java)
            startActivity(intent)
        }

        if (item.itemId == R.id.menu_social) {
            val intent = Intent(this, SocialActivity::class.java)
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