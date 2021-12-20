package com.arvind.looksea

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityProfileBinding
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

private const val TAG = "ProfileActivity"
class ProfileActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private var currUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityProfileBinding

    private lateinit var images: MutableList<Post>
    private lateinit var adapterImages: ProfileAdapter
    private lateinit var imageList: MutableList<Post>
    private lateinit var imageGridView: GridView
    private fun loadImages() {
        images = mutableListOf()
        imageGridView = binding.gvImages
        adapterImages = ProfileAdapter(this, images)
        imageGridView.adapter = adapterImages

        firestoreDb.collection("posts").whereEqualTo("user", currUser)
            .whereEqualTo("type", "image")
            .get()
            .addOnSuccessListener { myPosts ->
                imageList = myPosts.toObjects((Post::class.java))
                Log.i(TAG, "$imageList")
                images.clear()
                images.addAll(imageList)
                adapterImages.notifyDataSetChanged()
            }

        // to navigate to the page for each specific post. add later when CRUD is added for posts.
        // copy from SocialActivity
        /*
        adapterImages.setOnItemClickListener(object : ProfileAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val person = imageList[position]
                Log.i(TAG, "$person")
                val intent = Intent(this@ProfileActivity, ProfileActivity::class.java)
                intent.putExtra(EXTRA_USERNAME, person.username)
                startActivity(intent)
            }
        })
        */
    }

    private lateinit var videos: MutableList<Post>
    private lateinit var adapterVideos: ProfileAdapter
    private lateinit var videoList: MutableList<Post>
    private lateinit var videoGridView: GridView
    private fun loadVideos() {
        videos = mutableListOf()
        videoGridView = binding.gvVideos
        adapterVideos = ProfileAdapter(this, videos)
        videoGridView.adapter = adapterVideos

        firestoreDb.collection("posts").whereEqualTo("user", currUser)
            .whereEqualTo("type", "video")
            .get()
            .addOnSuccessListener { myPosts ->
                videoList = myPosts.toObjects((Post::class.java))
                Log.i(TAG, "$videoList")
                videos.clear()
                videos.addAll(videoList)
                adapterVideos.notifyDataSetChanged()
            }

        // to navigate to the page for each specific post. add later when CRUD is added for posts.
        // copy from SocialActivity
        /*
        adapterImages.setOnItemClickListener(object : ProfileAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val person = imageList[position]
                Log.i(TAG, "$person")
                val intent = Intent(this@ProfileActivity, ProfileActivity::class.java)
                intent.putExtra(EXTRA_USERNAME, person.username)
                startActivity(intent)
            }
        })
        */
    }

    private lateinit var audio: MutableList<Post>
    private lateinit var adapterAudio: ProfileAdapter
    private lateinit var audioList: MutableList<Post>
    private lateinit var audioGridView: GridView
    private fun loadAudio() {
        audio = mutableListOf()
        audioGridView = binding.gvAudio
        adapterAudio = ProfileAdapter(this, audio)
        audioGridView.adapter = adapterAudio

        firestoreDb.collection("posts").whereEqualTo("user", currUser)
            .whereEqualTo("type", "audio")
            .get()
            .addOnSuccessListener { myPosts ->
                audioList = myPosts.toObjects((Post::class.java))
                Log.i(TAG, "$audioList")
                audio.clear()
                audio.addAll(audioList)
                adapterAudio.notifyDataSetChanged()
            }

        // to navigate to the page for each specific post. add later when CRUD is added for posts.
        // copy from SocialActivity
        /*
        adapterImages.setOnItemClickListener(object : ProfileAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val person = imageList[position]
                Log.i(TAG, "$person")
                val intent = Intent(this@ProfileActivity, ProfileActivity::class.java)
                intent.putExtra(EXTRA_USERNAME, person.username)
                startActivity(intent)
            }
        })
        */
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val username = intent.getStringExtra(EXTRA_USERNAME)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.profileName.text = username

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")
                firestoreDb.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        currUser = userSnapshot.toObjects((User::class.java))[0]
                        binding.profileAge.text = "age:   " + currUser?.age.toString()
                        binding.profileDescription.text ="about me:   " + currUser?.description
                        Glide.with(applicationContext).load(currUser?.picture).into(binding.profilePicture)
                        Log.i(TAG, "Current User: $currUser")

                        firestoreDb.collection("friendrequests").document(signedInUser?.username as String)
                            .collection("sent").document(currUser?.username as String)
                            .get()
                            .addOnSuccessListener { sentResult ->
                                var contact = false
                                Log.i(TAG, "SENT: ${sentResult.data}")
                                if (sentResult.data != null) {
                                    contact = true
                                }

                                firestoreDb.collection("friendrequests").document(signedInUser?.username as String)
                                    .collection("received").document(currUser?.username as String)
                                    .get()
                                    .addOnSuccessListener { receivedResult ->
                                        Log.i(TAG, "RECEIVED: ${receivedResult.data}")
                                        var receiver = false
                                        if (receivedResult.data != null) {
                                            contact = true
                                            receiver = true
                                        }

                                        firestoreDb.collection("friendlists").document(signedInUser?.username as String)
                                            .collection("myfriends").document(currUser?.username as String)
                                            .get()
                                            .addOnSuccessListener { friendResult ->
                                                Log.i(TAG, "FRIEND: ${friendResult.data}")
                                                var friend = false
                                                if (friendResult.data != null) {
                                                    friend = true
                                                }

                                                if (username == signedInUser?.username) {
                                                    binding.btnProfile.text = "Create Post"
                                                    binding.btnProfile.isEnabled = true
                                                    binding.btnProfile.setOnClickListener {
                                                        val intent = Intent(this, CreateActivity::class.java)
                                                        startActivity(intent)
                                                    }
                                                } else if (friend) {
                                                    binding.btnProfile.text = "My Friend"
                                                    binding.btnProfile.isEnabled = false
                                                } else if (contact) {
                                                    binding.btnProfile.text = "Pending"
                                                    binding.btnProfile.isEnabled = false
                                                    if (receiver) {
                                                        binding.btnAccept.isVisible = true
                                                        binding.btnAccept.setOnClickListener {
                                                            acceptFriendRequest()
                                                        }
                                                        binding.btnReject.isVisible = true
                                                        binding.btnReject.setOnClickListener {
                                                            rejectFriendRequest()
                                                        }
                                                    }
                                                } else {
                                                    binding.btnProfile.text = "Add Friend"
                                                    binding.btnProfile.isEnabled = true
                                                    binding.btnProfile.setOnClickListener {
                                                        sendFriendRequest()
                                                    }
                                                }

                                                loadImages()
                                                loadVideos()
                                                loadAudio()

                                            }
                                    }
                            }
                    }
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
            val intent = Intent(this, PostsActivity::class.java)
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

    private fun sendFriendRequest() {
        val curr = currUser
        val sign = signedInUser
        if (curr != null) {
            firestoreDb.collection("friendrequests").document(signedInUser?.username as String)
                .collection("sent").document(currUser?.username as String).set(curr)
        }
        if (sign != null) {
            firestoreDb.collection("friendrequests").document(currUser?.username as String)
                .collection("received").document(signedInUser?.username as String).set(sign)
        }

        Toast.makeText(this, "Friend Request Sent...", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(getIntent())
    }

    private fun rejectFriendRequest() {
        firestoreDb.collection("friendrequests").document(signedInUser?.username as String)
            .collection("received").document(currUser?.username as String).delete()

        firestoreDb.collection("friendrequests").document(currUser?.username as String)
            .collection("sent").document(signedInUser?.username as String).delete()

        Toast.makeText(this, "Friend Request Rejected...", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(getIntent())
    }

    private fun acceptFriendRequest() {
        val curr = currUser
        val sign = signedInUser
        if (curr != null) {
            firestoreDb.collection("friendlists").document(signedInUser?.username as String)
                .collection("myfriends").document(currUser?.username as String).set(curr)
        }
        if (sign != null) {
            firestoreDb.collection("friendlists").document(currUser?.username as String)
                .collection("myfriends").document(signedInUser?.username as String).set(sign)
        }

        firestoreDb.collection("friendrequests").document(signedInUser?.username as String)
            .collection("received").document(currUser?.username as String).delete()

        firestoreDb.collection("friendrequests").document(currUser?.username as String)
            .collection("sent").document(signedInUser?.username as String).delete()

        Toast.makeText(this, "Friend Request Accepted...", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(getIntent())
    }

    private fun logout() {
        Log.i(TAG, "Logging out...")
        FirebaseAuth.getInstance().signOut()
        val logoutIntent = Intent(this, LoginActivity::class.java)
        logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(logoutIntent)
    }
}