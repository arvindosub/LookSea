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
import java.io.File

private const val TAG = "SocialActivity"

class SocialActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var users: MutableList<User>
    private lateinit var adapter: UsersAdapter
    private lateinit var binding: ActivitySocialBinding
    private lateinit var userList: MutableList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Create the layout file which represents one post - DONE
        // Create data source - DONE
        users = mutableListOf()
        // Create the adapter
        adapter = UsersAdapter(this, users)
        // Bind the adapter and layout manager to the RV
        binding.rvSearch.adapter = adapter
        binding.rvSearch.layoutManager = LinearLayoutManager(this)
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

        binding.etSearch.addTextChangedListener {
            if (it.toString() == "") {
                users.clear()
                adapter.notifyDataSetChanged()
            } else {
                firestoreDb.collection("users")
                    .whereGreaterThanOrEqualTo("username", it.toString())
                    .get()
                    .addOnSuccessListener { querySnapshots ->
                        userList = querySnapshots.toObjects((User::class.java))
                        Log.i(TAG, "$userList")
                        users.clear()
                        users.addAll(userList)
                        adapter.notifyDataSetChanged()
                    }
            }
        }
        adapter.setOnItemClickListener(object : UsersAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val person = userList[position]
                Log.i(TAG, "$person")
                val intent = Intent(this@SocialActivity, ProfileActivity::class.java)
                intent.putExtra(EXTRA_USERNAME, person.username)
                startActivity(intent)
            }
        })
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