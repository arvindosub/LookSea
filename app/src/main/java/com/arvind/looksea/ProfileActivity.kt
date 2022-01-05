package com.arvind.looksea

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.arvind.looksea.databinding.ActivityProfileBinding
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "ProfileActivity"
class ProfileActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private var userId: String? = ""
    private var currUser: User? = null
    private var currUserId: String? = ""
    private var requestReceiver: Boolean? = false
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityProfileBinding

    private lateinit var images: MutableList<Post>
    private lateinit var adapterImages: FileAdapter
    private lateinit var imageList: MutableList<Post>
    private lateinit var imageGridView: GridView
    private fun loadImages() {
        images = mutableListOf()
        imageGridView = binding.gvImages
        adapterImages = FileAdapter(this, images)
        imageGridView.adapter = adapterImages

        firestoreDb.collection("posts").whereEqualTo("user", currUserId)
            .whereEqualTo("type", "image")
            .get()
            .addOnSuccessListener { myPosts ->
                imageList = myPosts.toObjects((Post::class.java))
                Log.i(TAG, "$imageList")
                images.clear()
                images.addAll(imageList)
                adapterImages.notifyDataSetChanged()

                imageGridView.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        val intent = Intent(this@ProfileActivity, PostActivity::class.java)
                        intent.putExtra(EXTRA_POSTTIME, images[position].creationTimeMs.toString())
                        startActivity(intent)
                    }
            }
    }

    private lateinit var videos: MutableList<Post>
    private lateinit var adapterVideos: FileAdapter
    private lateinit var videoList: MutableList<Post>
    private lateinit var videoGridView: GridView
    private fun loadVideos() {
        videos = mutableListOf()
        videoGridView = binding.gvVideos
        adapterVideos = FileAdapter(this, videos)
        videoGridView.adapter = adapterVideos

        firestoreDb.collection("posts").whereEqualTo("user", currUserId)
            .whereEqualTo("type", "video")
            .get()
            .addOnSuccessListener { myPosts ->
                videoList = myPosts.toObjects((Post::class.java))
                Log.i(TAG, "$videoList")
                videos.clear()
                videos.addAll(videoList)
                adapterVideos.notifyDataSetChanged()

                videoGridView.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        val intent = Intent(this@ProfileActivity, PostActivity::class.java)
                        intent.putExtra(EXTRA_POSTTIME, videos[position].creationTimeMs.toString())
                        startActivity(intent)
                    }
            }
    }

    private lateinit var audio: MutableList<Post>
    private lateinit var adapterAudio: FileAdapter
    private lateinit var audioList: MutableList<Post>
    private lateinit var audioGridView: GridView
    private fun loadAudio() {
        audio = mutableListOf()
        audioGridView = binding.gvAudio
        adapterAudio = FileAdapter(this, audio)
        audioGridView.adapter = adapterAudio

        firestoreDb.collection("posts").whereEqualTo("user", currUserId)
            .whereEqualTo("type", "audio")
            .get()
            .addOnSuccessListener { myPosts ->
                audioList = myPosts.toObjects((Post::class.java))
                Log.i(TAG, "$audioList")
                audio.clear()
                audio.addAll(audioList)
                adapterAudio.notifyDataSetChanged()

                audioGridView.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        val intent = Intent(this@ProfileActivity, PostActivity::class.java)
                        intent.putExtra(EXTRA_POSTTIME, audio[position].creationTimeMs.toString())
                        startActivity(intent)
                    }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val username = intent.getStringExtra(EXTRA_USERNAME)
        Log.i(TAG, "$username")
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.profileName.text = username

        firestoreDb = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid as String
        firestoreDb.collection("users")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")
                firestoreDb.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        userSnapshot.forEach{ doc ->
                            currUser = doc.toObject((User::class.java))
                            currUserId = doc.id
                        }
                        binding.profileAge.text = "Age:   " + currUser?.age.toString()
                        binding.profileDescription.text = currUser?.description
                        Glide.with(applicationContext).load(currUser?.picture).into(binding.profilePicture)
                        Log.i(TAG, "Current User: $currUser")

                        firestoreDb.collection("friendrequests").document(userId as String)
                            .collection("sent").document(currUserId as String)
                            .get()
                            .addOnSuccessListener { sentResult ->
                                var contact = false
                                Log.i(TAG, "SENT: ${sentResult.data}")
                                if (sentResult.data != null) {
                                    contact = true
                                }

                                firestoreDb.collection("friendrequests").document(userId as String)
                                    .collection("received").document(currUserId as String)
                                    .get()
                                    .addOnSuccessListener { receivedResult ->
                                        Log.i(TAG, "RECEIVED: ${receivedResult.data}")
                                        if (receivedResult.data != null) {
                                            contact = true
                                            requestReceiver = true
                                        }

                                        firestoreDb.collection("friendlists").document(userId as String)
                                            .collection("myfriends").document(currUserId as String)
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
                                                        val createIntent = Intent(this, CreateActivity::class.java)
                                                        startActivity(createIntent)
                                                    }
                                                    val particularsIntent = Intent(this, ParticularsActivity::class.java)
                                                    binding.profilePicture.setOnClickListener {
                                                        startActivity(particularsIntent)
                                                    }
                                                    binding.profileAge.setOnClickListener {
                                                        startActivity(particularsIntent)
                                                    }
                                                    binding.profileDescription.setOnClickListener {
                                                        startActivity(particularsIntent)
                                                    }
                                                    binding.profileName.setOnClickListener {
                                                        startActivity(particularsIntent)
                                                    }
                                                } else if (friend) {
                                                    binding.btnProfile.text = "Unfriend"
                                                    binding.btnProfile.isEnabled = true
                                                    // remove friend option
                                                    binding.btnProfile.setOnClickListener {
                                                        removeFriend()
                                                    }
                                                } else if (contact) {
                                                    binding.btnProfile.text = "Pending"
                                                    binding.btnProfile.isEnabled = false

                                                    if (requestReceiver == true) {
                                                        binding.btnAccept.isVisible = true
                                                        binding.btnAccept.setOnClickListener {
                                                            acceptFriendRequest()
                                                        }
                                                        binding.btnReject.text = "Reject"
                                                        binding.btnReject.isVisible = true
                                                        binding.btnReject.setOnClickListener {
                                                            rejectFriendRequest()
                                                        }
                                                    } else {
                                                        binding.btnReject.text = "Cancel"
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

    private fun sendFriendRequest() {
        val curr = currUser
        val sign = signedInUser
        if (curr != null) {
            firestoreDb.collection("friendrequests").document(userId as String)
                .collection("sent").document(currUserId as String).set(curr)
        }
        if (sign != null) {
            firestoreDb.collection("friendrequests").document(currUserId as String)
                .collection("received").document(userId as String).set(sign)
        }

        Toast.makeText(this, "Friend Request Sent...", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(getIntent())
    }

    private fun rejectFriendRequest() {
        if (requestReceiver == true) {
            firestoreDb.collection("friendrequests").document(userId as String)
                .collection("received").document(currUserId as String).delete()

            firestoreDb.collection("friendrequests").document(currUserId as String)
                .collection("sent").document(userId as String).delete()

            Toast.makeText(this, "Friend Request Rejected...", Toast.LENGTH_SHORT).show()

        } else {
            firestoreDb.collection("friendrequests").document(userId as String)
                .collection("sent").document(currUserId as String).delete()

            firestoreDb.collection("friendrequests").document(currUserId as String)
                .collection("received").document(userId as String).delete()

            Toast.makeText(this, "Friend Request Cancelled...", Toast.LENGTH_SHORT).show()

        }
        finish()
        val intent = Intent(this, SocialActivity::class.java)
        startActivity(intent)
    }

    private fun acceptFriendRequest() {
        val curr = currUser
        val sign = signedInUser
        if (curr != null) {
            firestoreDb.collection("friendlists").document(userId as String)
                .collection("myfriends").document(currUserId as String).set(curr)
        }
        if (sign != null) {
            firestoreDb.collection("friendlists").document(currUserId as String)
                .collection("myfriends").document(userId as String).set(sign)
        }

        firestoreDb.collection("friendrequests").document(userId as String)
            .collection("received").document(currUserId as String).delete()

        firestoreDb.collection("friendrequests").document(currUserId as String)
            .collection("sent").document(userId as String).delete()

        Toast.makeText(this, "Friend Request Accepted...", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(getIntent())
    }

    private fun removeFriend() {
        val curr = currUser
        val sign = signedInUser
        if (curr != null) {
            firestoreDb.collection("friendlists").document(userId as String)
                .collection("myfriends").document(currUserId as String).delete()
        }
        if (sign != null) {
            firestoreDb.collection("friendlists").document(currUserId as String)
                .collection("myfriends").document(userId as String).delete()
        }
        Toast.makeText(this, "Unfriended...", Toast.LENGTH_SHORT).show()
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