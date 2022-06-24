package com.arvind.looksea

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityHomeBinding
import com.arvind.looksea.models.Link
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private const val TAG = "HomeActivity"
const val EXTRA_USERNAME = "EXTRA_USERNAME"
const val EXTRA_POSTTIME = "EXTRA_POSTTIME"
const val EXTRA_ARTIFACTID = "EXTRA_ARTIFACTID"

open class HomeActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private var userId: String? = ""
    private var currUser: User? = null
    private var currUserId: String? = ""
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var postIds: MutableList<String>
    private lateinit var adapter: PostAdapter
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestoreDb = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Create the layout file which represents one post - DONE
        // Create data source - DONE
        posts = mutableListOf()
        postIds = mutableListOf()
        // Create the adapter
        adapter = PostAdapter(this, posts, userId!!)
        // Bind the adapter and layout manager to the RV
        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        firestoreDb.collection("artifacts")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")

                var postsReference = firestoreDb.collection("artifacts")
                    .limit(20)
                    .orderBy("creation_time_ms", Query.Direction.DESCENDING)

                val username = intent.getStringExtra(EXTRA_USERNAME)
                Log.i(TAG, "username: $username")
                if (username != null) {
                    postsReference = postsReference.whereEqualTo("username", username)
                    // username is always null
                } else {
                    var friendList: MutableList<String> = ArrayList()
                    var fofList: MutableList<String> = ArrayList()
                    var granularReadList: MutableList<String> = ArrayList()
                    friendList.add(userId!!)

                    firestoreDb.collection("links")
                        .document(userId as String)
                        .collection("friend")
                        .get()
                        .addOnSuccessListener { friends ->
                            friends.forEach { fr ->
                                if (fr != null) {
                                    friendList.add(fr.id)
                                }
                                firestoreDb.collection("links")
                                    .document(fr.id as String)
                                    .collection("friend")
                                    .get()
                                    .addOnSuccessListener { fofs ->
                                        fofs.forEach { fof ->
                                            if (fof != null) {
                                                fofList.add(fof.id)
                                            }
                                        }
                                    }
                            }

                            firestoreDb.collection("links")
                                .document(userId as String)
                                .collection("read")
                                .get()
                                .addOnSuccessListener { readDocs ->
                                    readDocs.forEach { rd ->
                                        if (rd != null) {
                                            granularReadList.add(rd.id)
                                        }
                                    }

                                    Log.i(TAG, "Friends List: $friendList")
                                    Log.i(TAG, "FoF List: $fofList")
                                    Log.i(TAG, "Granular Read List: $granularReadList")

                                    postsReference.addSnapshotListener { snapshot, exception ->
                                        if (exception != null || snapshot == null) {
                                            Log.e(TAG, "Exception when querying posts", exception)
                                            return@addSnapshotListener
                                        }
                                        var postList = mutableListOf<Post>()
                                        var postIdList = mutableListOf<String>()
                                        snapshot.forEach { doc ->
                                            var myPost = doc.toObject(Post::class.java)
                                            Log.i(TAG, "Post ID: ${doc.id}")
                                            Log.i(TAG, "Post: $myPost")
                                            if (myPost.privacy!!.contains("/pub2") || myPost.privacy!!.contains("/pub3") || myPost.privacy!!.contains("/pub4")) {
                                                if (doc.id !in postIdList) {
                                                    postList.add(myPost)
                                                    postIdList.add(doc.id)
                                                }
                                            }

                                            if (myPost.privacy!!.contains("/frds2") || myPost.privacy!!.contains("/frds3") || myPost.privacy!!.contains("/frds4")) {
                                                if (myPost.userId in friendList && doc.id !in postIdList) {
                                                    postList.add(myPost)
                                                    postIdList.add(doc.id)
                                                }
                                            }

                                            if (myPost.privacy!!.contains("/fof2") || myPost.privacy!!.contains("/fof3") || myPost.privacy!!.contains("/fof4")) {
                                                if (myPost.userId in fofList && doc.id !in postIdList) {
                                                    postList.add(myPost)
                                                    postIdList.add(doc.id)
                                                }
                                            }

                                            if (doc.id in granularReadList && doc.id !in postIdList) {
                                                postList.add(myPost)
                                                postIdList.add(doc.id)
                                            }
                                        }
                                        posts.clear()
                                        posts.addAll(postList)
                                        postIds.clear()
                                        postIds.addAll(postIdList)
                                        adapter.notifyDataSetChanged()
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

                adapter.setOnLikeClickListener(object : PostAdapter.onLikeClickListener {
                    override fun onLikeClick(position: Int) {
                        val likedPost = posts[position]
                        val likedPostId = postIds[position]
                        likePost(likedPost, likedPostId)
                    }
                })

                adapter.setOnUserClickListener(object : PostAdapter.onUserClickListener {
                    override fun onUserClick(position: Int) {
                        val thisUser = posts[position].username
                        val intent = Intent(this@HomeActivity, ProfileActivity::class.java)
                        intent.putExtra(EXTRA_USERNAME, thisUser)
                        startActivity(intent)
                    }
                })

                adapter.setOnCommentClickListener(object : PostAdapter.onCommentClickListener {
                    override fun onCommentClick(position: Int) {
                        val thisUser = posts[position].username
                        val intent = Intent(this@HomeActivity, ProfileActivity::class.java)
                        intent.putExtra(EXTRA_USERNAME, thisUser)
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

    private fun likePost(post: Post, postId: String) {
        val likes = post?.likes!!
        postId?.let {
            var isLiked = true
            firestoreDb.collection("links").document(userId as String)
                .collection("liked").document(postId as String)
                .get()
                .addOnSuccessListener { likeSnapshot ->
                    Log.i(TAG, "Liked? ${likeSnapshot}")
                    if (likeSnapshot.data == null) {
                        isLiked = false
                    }
                    Log.i(TAG, "isLiked? ${isLiked}")

                    if (!isLiked) {
                        firestoreDb.collection("artifacts").document(postId!!)
                            .update("likes", likes+1)
                            .addOnCompleteListener {
                                Log.i(TAG, "Likes ${likes+1}")
                                Toast.makeText(this, "Liked", Toast.LENGTH_SHORT).show()
                                var link = Link(
                                    "liked",
                                    "$userId"
                                )
                                firestoreDb.collection("links").document(userId as String)
                                    .collection("liked").document(postId as String).set(link)
                                    .addOnCompleteListener {
                                        firestoreDb.collection("links").document(postId as String)
                                            .collection("liked").document(userId as String).set(link)
                                    }
                            }
                    } else {
                        firestoreDb.collection("artifacts").document(postId!!)
                            .update("likes", likes-1)
                            .addOnCompleteListener {
                                Log.i(TAG, "Likes ${likes-1}")
                                Toast.makeText(this, "Unliked", Toast.LENGTH_SHORT).show()
                                firestoreDb.collection("links").document(userId as String)
                                    .collection("liked").document(postId as String).delete()
                                    .addOnCompleteListener {
                                        firestoreDb.collection("links").document(postId as String)
                                            .collection("liked").document(userId as String).delete()
                                    }
                            }
                    }
                }
        }
        //this.recreate()
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