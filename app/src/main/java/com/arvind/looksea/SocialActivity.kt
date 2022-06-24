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
    private var searchIdList = mutableListOf<String>()

    private lateinit var friend: MutableList<User>
    private lateinit var adapterFriends: UserAdapter
    private lateinit var myFriends: MutableList<User>

    private lateinit var request: MutableList<User>
    private lateinit var adapterRequests: UserAdapter
    private lateinit var myRequests: MutableList<User>

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
                        myFriends = friendSnapshots.toObjects(User::class.java)
                        friend.clear()
                        friend.addAll(myFriends)
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
                myRequests = receivedOnly.toObjects((User::class.java))
                request.clear()
                request.addAll(myRequests)
                firestoreDb.collection("links").document(userId as String)
                    .collection("sentrequest")
                    .get()
                    .addOnSuccessListener { requestSnapshots ->
                        myRequests = requestSnapshots.toObjects((User::class.java))
                        request.addAll(myRequests)
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

                                binding.etSearch.addTextChangedListener {
                                    if (it.toString() == "") {
                                        searchList.clear()
                                        searchIdList.clear()
                                        search.clear()
                                        adapterSearch.notifyDataSetChanged()
                                    } else if (it.toString().contains("/friendswith ")) {
                                        searchList.clear()
                                        searchIdList.clear()
                                        var searchTerm = it.toString().split(' ')[1]
                                        firestoreDb.collection("artifacts")
                                            .whereEqualTo("type", "user")
                                            .whereEqualTo("username", searchTerm)
                                            .get()
                                            .addOnSuccessListener { userSnapshots ->
                                                var searchUser = ""
                                                userSnapshots.forEach { docUser ->
                                                    searchUser = docUser.id
                                                }

                                                if (searchUser != "") {
                                                    firestoreDb.collection("links")
                                                        .document(searchUser)
                                                        .collection("friend")
                                                        .get()
                                                        .addOnSuccessListener { querySnapshots ->
                                                            var idList = mutableListOf<String>()
                                                            querySnapshots.forEach { docIds ->
                                                                idList.add(docIds.id)
                                                            }

                                                            if (idList != null) {
                                                                firestoreDb.collection("artifacts")
                                                                    .whereIn(FieldPath.documentId(), idList)
                                                                    .get()
                                                                    .addOnSuccessListener { friendSnapshots ->
                                                                        friendSnapshots.forEach { doc ->
                                                                            var myUser = doc.toObject(User::class.java)
                                                                            Log.i(TAG, "Post ID: ${doc.id}")
                                                                            Log.i(TAG, "Post: $myUser")
                                                                            if ((myUser.privacy!!.contains("/pub2")) || (myUser.privacy!!.contains("/pub3")) || (myUser.privacy!!.contains("/pub4"))) {
                                                                                if (doc.id !in searchIdList) {
                                                                                    searchList.add(myUser)
                                                                                    searchIdList.add(doc.id)
                                                                                }
                                                                            }

                                                                            if ((myUser.privacy!!.contains("/frds2")) || (myUser.privacy!!.contains("/frds3")) || (myUser.privacy!!.contains("/frds4"))) {
                                                                                if (doc.id in friendList && doc.id !in searchIdList) {
                                                                                    searchList.add(myUser)
                                                                                    searchIdList.add(doc.id)
                                                                                }
                                                                            }

                                                                            if ((myUser.privacy!!.contains("/fof2")) || (myUser.privacy!!.contains("/fof3")) || (myUser.privacy!!.contains("/fof4"))) {
                                                                                if (doc.id in fofList && doc.id !in searchIdList) {
                                                                                    searchList.add(myUser)
                                                                                    searchIdList.add(doc.id)
                                                                                }
                                                                            }

                                                                            if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                                                searchList.add(myUser)
                                                                                searchIdList.add(doc.id)
                                                                            }
                                                                        }
                                                                        search.clear()
                                                                        search.addAll(searchList)
                                                                        adapterSearch.notifyDataSetChanged()
                                                                        Log.i(TAG, "zz $searchList")
                                                                    }
                                                            }
                                                        }
                                                }
                                            }
                                    } else if (it.toString().contains("/likedby ")) {
                                        searchList.clear()
                                        searchIdList.clear()
                                        var searchTerm = it.toString().split(' ')[1]
                                        firestoreDb.collection("artifacts")
                                            .whereEqualTo("type", "user")
                                            .whereEqualTo("username", searchTerm)
                                            .get()
                                            .addOnSuccessListener { userSnapshots ->
                                                var searchUser = ""
                                                userSnapshots.forEach { docUser ->
                                                    searchUser = docUser.id
                                                }

                                                if (searchUser != "") {
                                                    firestoreDb.collection("links")
                                                        .document(searchUser)
                                                        .collection("liked")
                                                        .get()
                                                        .addOnSuccessListener { querySnapshots ->
                                                            var idList = mutableListOf<String>()
                                                            querySnapshots.forEach { docIds ->
                                                                idList.add(docIds.id)
                                                            }
                                                            if (idList != null) {
                                                                firestoreDb.collection("artifacts")
                                                                    .whereIn(FieldPath.documentId(), idList)
                                                                    .get()
                                                                    .addOnSuccessListener { docSnapshots ->
                                                                        docSnapshots.forEach { doc ->
                                                                            var myUser = doc.toObject(User::class.java)
                                                                            Log.i(TAG, "Post ID: ${doc.id}")
                                                                            Log.i(TAG, "Post: $myUser")
                                                                            if ((myUser.privacy!!.contains("/pub2")) || (myUser.privacy!!.contains("/pub3")) || (myUser.privacy!!.contains("/pub4"))) {
                                                                                if (doc.id !in searchIdList) {
                                                                                    searchList.add(myUser)
                                                                                    searchIdList.add(doc.id)
                                                                                }
                                                                            }

                                                                            if ((myUser.privacy!!.contains("/frds2")) || (myUser.privacy!!.contains("/frds3")) || (myUser.privacy!!.contains("/frds4"))) {
                                                                                if (doc.id in friendList && doc.id !in searchIdList) {
                                                                                    searchList.add(myUser)
                                                                                    searchIdList.add(doc.id)
                                                                                }
                                                                            }

                                                                            if ((myUser.privacy!!.contains("/fof2")) || (myUser.privacy!!.contains("/fof3")) || (myUser.privacy!!.contains("/fof4"))) {
                                                                                if (doc.id in fofList && doc.id !in searchIdList) {
                                                                                    searchList.add(myUser)
                                                                                    searchIdList.add(doc.id)
                                                                                }
                                                                            }

                                                                            if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                                                searchList.add(myUser)
                                                                                searchIdList.add(doc.id)
                                                                            }
                                                                        }
                                                                        search.clear()
                                                                        search.addAll(searchList)
                                                                        adapterSearch.notifyDataSetChanged()
                                                                        Log.i(TAG, "zz $searchList")
                                                                    }
                                                            }
                                                        }
                                                }
                                            }
                                    } else if (it.toString()[0].toString() == "/" && it.toString().contains(" ")) {
                                        searchList.clear()
                                        searchIdList.clear()
                                        var searchType = it.toString().split(' ')[0].drop(1)
                                        var searchTerm = it.toString().split(' ')[1]
                                        if (searchType == "user") {
                                            firestoreDb.collection("artifacts")
                                                .whereIn("type", mutableListOf(searchType))
                                                .whereGreaterThanOrEqualTo("username", searchTerm)
                                                .get()
                                                .addOnSuccessListener { querySnapshots ->
                                                    querySnapshots.forEach { doc ->
                                                        var myUser = doc.toObject(User::class.java)
                                                        Log.i(TAG, "Post ID: ${doc.id}")
                                                        Log.i(TAG, "Post: $myUser")
                                                        if ((myUser.privacy!!.contains("/pub2")) || (myUser.privacy!!.contains("/pub3")) || (myUser.privacy!!.contains("/pub4"))) {
                                                            if (doc.id !in searchIdList) {
                                                                searchList.add(myUser)
                                                                searchIdList.add(doc.id)
                                                            }
                                                        }

                                                        if ((myUser.privacy!!.contains("/frds2")) || (myUser.privacy!!.contains("/frds3")) || (myUser.privacy!!.contains("/frds4"))) {
                                                            if (doc.id in friendList && doc.id !in searchIdList) {
                                                                searchList.add(myUser)
                                                                searchIdList.add(doc.id)
                                                            }
                                                        }

                                                        if ((myUser.privacy!!.contains("/fof2")) || (myUser.privacy!!.contains("/fof3")) || (myUser.privacy!!.contains("/fof4"))) {
                                                            if (doc.id in fofList && doc.id !in searchIdList) {
                                                                searchList.add(myUser)
                                                                searchIdList.add(doc.id)
                                                            }
                                                        }

                                                        if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                            searchList.add(myUser)
                                                            searchIdList.add(doc.id)
                                                        }
                                                    }
                                                    search.clear()
                                                    search.addAll(searchList)
                                                    adapterSearch.notifyDataSetChanged()
                                                    Log.i(TAG, "zz $searchList")
                                                }
                                        } else if (searchTerm != "") {
                                            searchList.clear()
                                            firestoreDb.collection("artifacts")
                                                .whereIn("type", mutableListOf(searchType))
                                                .get()
                                                .addOnSuccessListener { querySnapshots ->
                                                    querySnapshots.forEach { doc ->
                                                        var myUser = doc.toObject(User::class.java)
                                                        var descList = doc.toObject((User::class.java)).description.split(' ')
                                                        Log.i(TAG, "Post ID: ${doc.id}")
                                                        Log.i(TAG, "Post: $myUser")
                                                        if ((myUser.privacy!!.contains("/pub2")) || (myUser.privacy!!.contains("/pub3")) || (myUser.privacy!!.contains("/pub4"))) {
                                                            if (doc.id !in searchIdList) {
                                                                for (desc in descList) {
                                                                    if ((desc.contains(searchTerm, ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                        searchList.add(myUser)
                                                                        searchIdList.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if ((myUser.privacy!!.contains("/frds2")) || (myUser.privacy!!.contains("/frds3")) || (myUser.privacy!!.contains("/frds4"))) {
                                                            if (doc.id in friendList && doc.id !in searchIdList) {
                                                                for (desc in descList) {
                                                                    if ((desc.contains(searchTerm, ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                        searchList.add(myUser)
                                                                        searchIdList.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if ((myUser.privacy!!.contains("/fof2")) || (myUser.privacy!!.contains("/fof3")) || (myUser.privacy!!.contains("/fof4"))) {
                                                            if (doc.id in fofList && doc.id !in searchIdList) {
                                                                for (desc in descList) {
                                                                    if ((desc.contains(searchTerm, ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                        searchList.add(myUser)
                                                                        searchIdList.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                            for (desc in descList) {
                                                                if ((desc.contains(searchTerm, ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                    searchList.add(myUser)
                                                                    searchIdList.add(doc.id)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    search.clear()
                                                    search.addAll(searchList)
                                                    adapterSearch.notifyDataSetChanged()
                                                    Log.i(TAG, "zz $searchList")
                                                }
                                        }
                                    } else {
                                        searchList.clear()
                                        searchIdList.clear()
                                        firestoreDb.collection("artifacts").whereIn("type", mutableListOf("user", "image", "video", "text", "audio"))
                                            .get()
                                            .addOnSuccessListener { querySnapshots ->
                                                querySnapshots.forEach { doc ->
                                                    var myUser = doc.toObject(User::class.java)
                                                    var descList = doc.toObject((User::class.java)).description.split(' ')
                                                    Log.i(TAG, "Post ID: ${doc.id}")
                                                    Log.i(TAG, "Post: $myUser")
                                                    if ((myUser.privacy!!.contains("/pub2")) || (myUser.privacy!!.contains("/pub3")) || (myUser.privacy!!.contains("/pub4"))) {
                                                        if (doc.id !in searchIdList) {
                                                            for (desc in descList) {
                                                                if ((desc.contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                    searchList.add(myUser)
                                                                    searchIdList.add(doc.id)
                                                                }
                                                            }
                                                            if ((myUser.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myUser)
                                                                searchIdList.add(doc.id)
                                                            }
                                                        }
                                                    }

                                                    if ((myUser.privacy!!.contains("/frds2")) || (myUser.privacy!!.contains("/frds3")) || (myUser.privacy!!.contains("/frds4"))) {
                                                        if (doc.id in friendList && doc.id !in searchIdList) {
                                                            for (desc in descList) {
                                                                if ((desc.contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                    searchList.add(myUser)
                                                                    searchIdList.add(doc.id)
                                                                }
                                                            }
                                                            if ((myUser.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myUser)
                                                                searchIdList.add(doc.id)
                                                            }
                                                        }
                                                    }

                                                    if ((myUser.privacy!!.contains("/fof2")) || (myUser.privacy!!.contains("/fof3")) || (myUser.privacy!!.contains("/fof4"))) {
                                                        if (doc.id in fofList && doc.id !in searchIdList) {
                                                            for (desc in descList) {
                                                                if ((desc.contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                    searchList.add(myUser)
                                                                    searchIdList.add(doc.id)
                                                                }
                                                            }
                                                            if ((myUser.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myUser)
                                                                searchIdList.add(doc.id)
                                                            }
                                                        }
                                                    }

                                                    if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                        for (desc in descList) {
                                                            if ((desc.contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myUser)
                                                                searchIdList.add(doc.id)
                                                            }
                                                        }
                                                        if ((myUser.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                            searchList.add(myUser)
                                                            searchIdList.add(doc.id)
                                                        }
                                                    }
                                                }
                                                search.clear()
                                                search.addAll(searchList)
                                                adapterSearch.notifyDataSetChanged()
                                                Log.i(TAG, "zz $searchList")
                                            }
                                    }
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