package com.arvind.looksea

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.arvind.looksea.databinding.ActivityCreateBinding
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityPostsBinding
import com.arvind.looksea.databinding.ActivitySocialBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import java.io.File

private const val TAG = "SocialActivity"

class SocialActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivitySocialBinding

    private lateinit var friend: MutableList<User>
    private lateinit var adapterFriends: UsersAdapter
    private lateinit var friendList: MutableList<User>

    private lateinit var request: MutableList<User>
    private lateinit var adapterRequests: UsersAdapter
    private lateinit var requestList: MutableList<User>

    private fun loadFriends() {
        friend = mutableListOf()
        adapterFriends = UsersAdapter(this, friend)
        binding.rvFriends.adapter = adapterFriends
        binding.rvFriends.layoutManager = LinearLayoutManager(this)
        firestoreDb.collection("friendlists").document(signedInUser?.username as String)
            .collection("myfriends")
            .get()
            .addOnSuccessListener { requestSnapshots ->
                friendList = requestSnapshots.toObjects((User::class.java))
                Log.i(TAG, "FRIENDS: $friend")
                friend.clear()
                friend.addAll(friendList)
                adapterFriends.notifyDataSetChanged()

                adapterFriends.setOnItemClickListener(object : UsersAdapter.onItemClickListener {
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

    private fun loadRequests() {
        request = mutableListOf()
        adapterRequests = UsersAdapter(this, request)
        binding.rvRequests.adapter = adapterRequests
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        firestoreDb.collection("friendrequests").document(signedInUser?.username as String)
            .collection("received")
            .get()
            .addOnSuccessListener { receivedOnly ->
                requestList = receivedOnly.toObjects((User::class.java))
                request.clear()
                request.addAll(requestList)
                firestoreDb.collection("friendrequests").document(signedInUser?.username as String)
                    .collection("sent")
                    .get()
                    .addOnSuccessListener { requestSnapshots ->
                        requestList = requestSnapshots.toObjects((User::class.java))
                        request.addAll(requestList)
                        Log.i(TAG, "REQUESTS: $request")
                        adapterRequests.notifyDataSetChanged()

                        adapterRequests.setOnItemClickListener(object : UsersAdapter.onItemClickListener {
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
        firestoreDb = FirebaseFirestore.getInstance()

        firestoreDb.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")

                loadRequests()
                loadFriends()

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

    private fun logout() {
        Log.i(TAG, "Logging out...")
        FirebaseAuth.getInstance().signOut()
        val logoutIntent = Intent(this, LoginActivity::class.java)
        logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(logoutIntent)
    }
}