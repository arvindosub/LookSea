package com.arvind.looksea

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityAccessBinding
import com.arvind.looksea.models.Item
import com.arvind.looksea.models.Link
import com.google.firebase.firestore.FieldPath

private const val TAG = "AccessActivity"

class AccessActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var userId: String? = ""
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityAccessBinding
    private lateinit var search: MutableList<Item>
    private lateinit var adapterSearch: ItemAdapter
    private var searchList = mutableListOf<Item>()
    private var searchIdList = mutableListOf<String>()
    private var usernameList = mutableListOf<String>()
    private var viewableIds = mutableListOf<String>()
    private var selUserId: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        search = mutableListOf()
        adapterSearch = ItemAdapter(this, search)
        binding.rvSearch.adapter = adapterSearch
        binding.rvSearch.layoutManager = LinearLayoutManager(this)

        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("artifacts")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")

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
                                                                    .whereIn("type", mutableListOf("user"))
                                                                    .get()
                                                                    .addOnSuccessListener { friendSnapshots ->
                                                                        friendSnapshots.forEach { doc ->
                                                                            var myPost = doc.toObject(Item::class.java)
                                                                            Log.i(TAG, "Post ID: ${doc.id}")
                                                                            Log.i(TAG, "Post: $myPost")

                                                                            if ((myPost.privacy!!.contains("/pub1")) || (myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                                                                                if (doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    usernameList.add(myPost.username.toString())
                                                                                    if (!myPost.privacy!!.contains("/pub1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                                                                                if ((myPost.userId in friendList || doc.id in friendList) && doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    usernameList.add(myPost.username.toString())
                                                                                    if (!myPost.privacy!!.contains("/frds1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if ((myPost.privacy!!.contains("/fof1")) || (myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                                                                                if ((myPost.userId in fofList || doc.id in fofList) && doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    usernameList.add(myPost.username.toString())
                                                                                    if (!myPost.privacy!!.contains("/fof1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                                                searchList.add(myPost)
                                                                                searchIdList.add(doc.id)
                                                                                usernameList.add(myPost.username.toString())
                                                                                viewableIds.add(doc.id)
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
                                                                    .whereIn("type", mutableListOf("user"))
                                                                    .get()
                                                                    .addOnSuccessListener { docSnapshots ->
                                                                        docSnapshots.forEach { doc ->
                                                                            var myPost = doc.toObject(Item::class.java)
                                                                            Log.i(TAG, "Post ID: ${doc.id}")
                                                                            Log.i(TAG, "Post: $myPost")
                                                                            if ((myPost.privacy!!.contains("/pub1")) || (myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                                                                                if (doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    usernameList.add(myPost.username.toString())
                                                                                    if (!myPost.privacy!!.contains("/pub1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                                                                                if ((myPost.userId in friendList || doc.id in friendList) && doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    usernameList.add(myPost.username.toString())
                                                                                    if (!myPost.privacy!!.contains("/frds1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if ((myPost.privacy!!.contains("/fof1")) || (myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                                                                                if ((myPost.userId in fofList || doc.id in fofList) && doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    usernameList.add(myPost.username.toString())
                                                                                    if (!myPost.privacy!!.contains("/fof1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                                                searchList.add(myPost)
                                                                                searchIdList.add(doc.id)
                                                                                usernameList.add(myPost.username.toString())
                                                                                viewableIds.add(doc.id)
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
                                                        var myPost = doc.toObject(
                                                            Item::class.java)
                                                        Log.i(TAG, "Post ID: ${doc.id}")
                                                        Log.i(TAG, "Post: $myPost")
                                                        if ((myPost.privacy!!.contains("/pub1")) || (myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                                                            if (doc.id !in searchIdList) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                usernameList.add(myPost.username.toString())
                                                                if (!myPost.privacy!!.contains("/pub1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }

                                                        if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                                                            if ((myPost.userId in friendList || doc.id in friendList) && doc.id !in searchIdList) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                usernameList.add(myPost.username.toString())
                                                                if (!myPost.privacy!!.contains("/frds1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }

                                                        if ((myPost.privacy!!.contains("/fof1")) || (myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                                                            if ((myPost.userId in fofList || doc.id in fofList) && doc.id !in searchIdList) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                usernameList.add(myPost.username.toString())
                                                                if (!myPost.privacy!!.contains("/fof1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }

                                                        if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                            searchList.add(myPost)
                                                            searchIdList.add(doc.id)
                                                            usernameList.add(myPost.username.toString())
                                                            viewableIds.add(doc.id)
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
                                                .whereIn("type", mutableListOf("user"))
                                                .get()
                                                .addOnSuccessListener { querySnapshots ->
                                                    querySnapshots.forEach { doc ->
                                                        var myPost = doc.toObject(Item::class.java)
                                                        var descList = doc.toObject((Item::class.java)).description.split(' ')
                                                        Log.i(TAG, "Post ID: ${doc.id}")
                                                        Log.i(TAG, "Post: $myPost")
                                                        if ((myPost.privacy!!.contains("/pub1")) || (myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                                                            if (doc.id !in searchIdList) {
                                                                for (desc in descList) {
                                                                    if ((desc.contains(searchTerm, ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                        searchList.add(myPost)
                                                                        searchIdList.add(doc.id)
                                                                        usernameList.add(myPost.username.toString())
                                                                        if (!myPost.privacy!!.contains("/pub1")) {
                                                                            viewableIds.add(doc.id)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                                                            if ((myPost.userId in friendList || doc.id in friendList) && doc.id !in searchIdList) {
                                                                for (desc in descList) {
                                                                    if ((desc.contains(searchTerm, ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                        searchList.add(myPost)
                                                                        searchIdList.add(doc.id)
                                                                        usernameList.add(myPost.username.toString())
                                                                        if (!myPost.privacy!!.contains("/frds1")) {
                                                                            viewableIds.add(doc.id)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if ((myPost.privacy!!.contains("/fof1")) || (myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                                                            if ((myPost.userId in fofList || doc.id in fofList) && doc.id !in searchIdList) {
                                                                for (desc in descList) {
                                                                    if ((desc.contains(searchTerm, ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                        searchList.add(myPost)
                                                                        searchIdList.add(doc.id)
                                                                        usernameList.add(myPost.username.toString())
                                                                        if (!myPost.privacy!!.contains("/fof1")) {
                                                                            viewableIds.add(doc.id)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                            for (desc in descList) {
                                                                if ((desc.contains(searchTerm, ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                    searchList.add(myPost)
                                                                    searchIdList.add(doc.id)
                                                                    usernameList.add(myPost.username.toString())
                                                                    viewableIds.add(doc.id)
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
                                        firestoreDb.collection("artifacts").whereIn("type", mutableListOf("user"))
                                            .get()
                                            .addOnSuccessListener { querySnapshots ->
                                                querySnapshots.forEach { doc ->
                                                    var myPost = doc.toObject(Item::class.java)
                                                    var descList = doc.toObject((Item::class.java)).description.split(' ')
                                                    Log.i(TAG, "Post ID: ${doc.id}")
                                                    Log.i(TAG, "Post: $myPost")
                                                    if ((myPost.privacy!!.contains("/pub1")) || (myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                                                        if (doc.id !in searchIdList) {
                                                            for (desc in descList) {
                                                                if ((desc.contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                    searchList.add(myPost)
                                                                    searchIdList.add(doc.id)
                                                                    usernameList.add(myPost.username.toString())
                                                                    if (!myPost.privacy!!.contains("/pub1")) {
                                                                        viewableIds.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                            if ((myPost.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                usernameList.add(myPost.username.toString())
                                                                if (!myPost.privacy!!.contains("/pub1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                                                        if ((myPost.userId in friendList || doc.id in friendList) && doc.id !in searchIdList) {
                                                            for (desc in descList) {
                                                                if ((desc.contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                    searchList.add(myPost)
                                                                    searchIdList.add(doc.id)
                                                                    usernameList.add(myPost.username.toString())
                                                                    if (!myPost.privacy!!.contains("/frds1")) {
                                                                        viewableIds.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                            if ((myPost.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                usernameList.add(myPost.username.toString())
                                                                if (!myPost.privacy!!.contains("/frds1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if ((myPost.privacy!!.contains("/fof1")) || (myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                                                        if ((myPost.userId in fofList || doc.id in fofList) && doc.id !in searchIdList) {
                                                            for (desc in descList) {
                                                                if ((desc.contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                    searchList.add(myPost)
                                                                    searchIdList.add(doc.id)
                                                                    usernameList.add(myPost.username.toString())
                                                                    if (!myPost.privacy!!.contains("/fof1")) {
                                                                        viewableIds.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                            if ((myPost.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                usernameList.add(myPost.username.toString())
                                                                if (!myPost.privacy!!.contains("/fof1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                        for (desc in descList) {
                                                            if ((desc.contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                usernameList.add(myPost.username.toString())
                                                                viewableIds.add(doc.id)
                                                            }
                                                        }
                                                        if ((myPost.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                            searchList.add(myPost)
                                                            searchIdList.add(doc.id)
                                                            usernameList.add(myPost.username.toString())
                                                            viewableIds.add(doc.id)
                                                        }
                                                    }
                                                }
                                                search.clear()
                                                search.addAll(searchList)
                                                adapterSearch.notifyDataSetChanged()
                                                Log.i(TAG, "zz $searchList")
                                                Log.i(TAG, "zz $searchIdList")
                                            }
                                    }
                                }
                            }
                    }
                adapterSearch.setOnItemClickListener(object : ItemAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        if (searchIdList[position] in viewableIds) {
                            selUserId = searchIdList[position]
                            binding.tvSelectedUser.setText(usernameList[position])
                            Log.i(TAG, "${usernameList[position]}")
                        } else {
                            Toast.makeText(this@AccessActivity, "You do not have permission to access this page!", Toast.LENGTH_SHORT).show()
                        }
                    }
                })

                binding.btnSubmit.setOnClickListener {
                    handleSubmitButtonClick()
                }

                binding.btnSubmitXpath.setOnClickListener {
                    handleSubmitXpathButtonClick()
                }

            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }
    }

    private fun handleSubmitButtonClick() {
        binding.btnSubmit.isEnabled = false
        var artifactId = intent.getStringExtra(EXTRA_ARTIFACTID)
        if (artifactId == null) {
            artifactId = userId.toString()
        }
        Log.i(TAG, "$artifactId")
        var accessCode = binding.etAccessCode.text.toString()
        accessCode = accessCode.replace("\\s".toRegex(), "")
        var codeList = accessCode.split('/')
        Log.i(TAG, "$codeList")

        firestoreDb.collection("links").document(selUserId as String)
            .collection("ban").document(artifactId as String).delete()
        firestoreDb.collection("links").document(selUserId as String)
            .collection("read").document(artifactId as String).delete()
        firestoreDb.collection("links").document(selUserId as String)
            .collection("update").document(artifactId as String).delete()
        firestoreDb.collection("links").document(selUserId as String)
            .collection("delete").document(artifactId as String).delete()
        firestoreDb.collection("links").document(selUserId as String)
            .collection("configure").document(artifactId as String).delete()

        if ("ban" in codeList) {
            firestoreDb.collection("links").document(selUserId as String)
                .collection("ban").document(artifactId as String).set(Link("ban", "$userId"))
        }
        if ("read" in codeList) {
            firestoreDb.collection("links").document(selUserId as String)
                .collection("read").document(artifactId as String).set(Link("read", "$userId"))
        }
        if ("update" in codeList) {
            firestoreDb.collection("links").document(selUserId as String)
                .collection("read").document(artifactId as String).set(Link("read", "$userId"))
            firestoreDb.collection("links").document(selUserId as String)
                .collection("update").document(artifactId).set(Link("update", "$userId"))
        }
        if ("delete" in codeList) {
            firestoreDb.collection("links").document(selUserId as String)
                .collection("read").document(artifactId as String).set(Link("read", "$userId"))
            firestoreDb.collection("links").document(selUserId as String)
                .collection("delete").document(artifactId).set(Link("delete","$userId"))
        }
        if ("configure" in codeList) {
            firestoreDb.collection("links").document(selUserId as String)
                .collection("read").document(artifactId as String).set(Link("read", "$userId"))
            firestoreDb.collection("links").document(selUserId as String)
                .collection("configure").document(artifactId as String).set(Link("configure","$userId"))
        }

        binding.btnSubmit.isEnabled = true
        Toast.makeText(this, "Access Configured!", Toast.LENGTH_SHORT).show()
        finish()

    }

    private fun handleSubmitXpathButtonClick() {
        binding.btnSubmitXpath.isEnabled = false
        var artifactId = intent.getStringExtra(EXTRA_ARTIFACTID)
        if (artifactId == null) {
            artifactId = userId.toString()
        }
        Log.i(TAG, "artifactId: $artifactId")
        // var expression = binding.etXpathCode.text.toString()
        // ## searching
        // all users from japan --- //artifacts[@type='user'][contains(@description, 'japan')]
        // all friends of a certain user --- //links/child::document[@id=userId]/child::collection[@id='friend'][contains(@name, 'classmate')]
        // =========
        // [[[ type 1 queries -- more akin to xpath on xml ]]]
        // - //document[@id=myself]/sibling::document[contains(@description, 'japan')] --- all users from japan, using self as starting pt
        // - //collection[@id='artifacts']/child::document[contains(@description, 'japan')] --- all users from japan, using selected element (collection named 'artifacts') as starting pt
        // - //document[@id=myself][@owner=myfriend]/parent::collection[@id=friend] --- all friends of a particular friend, using self as starting pt
        // - //document[@id=auser]/child::collection[@id=friend] --- all friends of a certain user, using selected element (document with a user as id) as starting pt
        // challenges:
        //      requires knowledge of data storage structure to navigate between elements and query correctly. this takes away from the 'random search' feel
        //      not always possible to specify which collection to query unless we force the start pt to be a collection. ties back in with the above disadvantage.
        //
        // [[[ type 2 queries -- treat queries as a graph search with self node in the graph picked up to form a tree ]]]
        // - //user[@id=myself]/descendant::user[contains(@description, 'japan')] --- all users from japan, start point will always be specific artifact (DONE)
        // - //user[@id=myself]/child::friend[@id='4Lbyyznfw9YASlVXMhcG7fRKOZt2']/descendant::friend --- all friends of a particular friend (DONE)
        // - //user[@id='4Lbyyznfw9YASlVXMhcG7fRKOZt2']/descendant::friend --- all friends of a certain user (DONE)
        // - //user[@id='oxywVSc4DrOngH6VvJIxSSAYkeW2']/descendant::friend[@type='classmate'] --- all classmates of a certain user (DONE)
        // challenges:
        //      probably harder to decipher as no database storage-specific terms are used.
        //      hard to represent the relationship portion of ERM in the hierarchical form required for XML/XPath. might need to treat relationships as a 'node' between two entities.
        //
        // VERDICT: PROBABLY SHOULD TRANSFORM INTO TYPE 2. SUFFICIENT ABSTRACTION TO REMOVE ANYTHING SPECIFIC TO DATABASE STRUCTURE.
        // NEXT STEP IS TO PARSE TYPE-2 QUERIES INTO FIREBASE QUERIES.


        var expression = "//user[@id=myself]/descendant::user[contains(@description, 'japan')]"
        var cmdStr = getCommandString(expression)
        Log.i(TAG, "cmdStr: $cmdStr")
        executeFirebaseCommand(cmdStr)

        binding.btnSubmitXpath.isEnabled = true
        Toast.makeText(this, "Access Configured!", Toast.LENGTH_SHORT).show()
        //finish()

    }

    private fun getCommandString (expression: String): String {
        var items = expression.drop(2).split('/')
        var myList = mutableListOf<MutableList<String>>()
        var startPt = mutableListOf<String>()
        items.forEach { item ->
            var tempList = mutableListOf<String>()
            if (item.contains(']')) {
                var tempItem = item.replace("]","")
                var subItems = tempItem.split("[")
                subItems.forEach { subItem ->
                    tempList.add(subItem)
                }
                myList.add(tempList)
            } else {
                tempList.add(item)
                myList.add(tempList)
            }
        }
        myList[0].forEach { item ->
            if (item.contains('@')) {
                startPt.add(item.split('=')[1])
            } else {
                startPt.add(item)
            }
        }
        myList.removeAt(0)
        Log.i(TAG, "first item: $startPt")
        Log.i(TAG, "input list: $myList")

        var cmdStr = "firestoreDb.collection("
        if (myList.size == 1) {
            myList[0].forEach { subStep ->
                if (subStep.contains("@") && subStep.contains("=")) {
                    var tempSubStepList = subStep.drop(1).split("=")
                    if (tempSubStepList[0] == "type") {
                        cmdStr += "whereEqualTo('name', '${subStep.substringAfter("'").substringBefore("'")}')."
                    } else {
                        cmdStr += "whereEqualTo('${tempSubStepList[0]}', '${subStep.substringAfter("'").substringBefore("'")}')."
                    }
                } else if (subStep.contains("contains")) {
                    cmdStr += "whereIn('${subStep.substringAfter("@").substringBefore(",")}', mutableListOf('${subStep.substringAfter("'").substringBefore("'")}'))."
                } else {
                    if (subStep.contains("descendant")) {
                        var tempSubStepList = subStep.replace("::",":").split(":")
                        if (subStep.contains("user")) {
                            cmdStr += "'artifacts').whereEqualTo('type', '${tempSubStepList[1]}')."
                        } else {
                            cmdStr += "'links').document('${startPt[1].substringAfter("'").substringBefore("'")}').collection('${tempSubStepList[1]}')."
                        }
                    }
                }
            }
        } else if (myList.size == 2) {
            myList[0].forEach { subStep ->
                if (subStep.contains("::")) {
                    var tempSubStepList = subStep.replace("::",":").split(":")
                    if (tempSubStepList[0] != "descendant" && tempSubStepList[1] !in mutableListOf<String>("user", "post", "image", "video", "audio", "text")) {
                        cmdStr += "'links')."
                    }
                } else if (subStep.contains("@") && subStep.contains("=")) {
                    cmdStr += "document('${subStep.substringAfter("'").substringBefore("'")}')."
                }
            }
            myList[1].forEach { subStep ->
                if (subStep.contains("::")) {
                    var tempSubStepList = subStep.replace("::",":").split(":")
                    if (tempSubStepList[0] == "descendant") {
                        cmdStr += "collection('${tempSubStepList[1]}')."
                    }
                } else if (subStep.contains("@") && subStep.contains("=")) {
                    var tempSubStepList = subStep.drop(1).split("=")
                    if (tempSubStepList[0] == "id") {
                        cmdStr += "document('${tempSubStepList[1]}')."
                    } else {
                        cmdStr += "whereEqualTo('${tempSubStepList[0]}', '${tempSubStepList[1].substringAfter("'").substringBefore("'")}')."
                    }
                } else if (subStep.contains("contains") && subStep.contains("=")) {
                    cmdStr += "whereIn('${subStep.substringAfter("@").substringBefore(",")}', mutableListOf('${subStep.substringAfter("'").substringBefore("'")}'))."
                }
            }
        }
        cmdStr += "get()"
        return cmdStr
    }

    private fun executeFirebaseCommand (cmdStr: String) {
        var cmdList = cmdStr.split(".").drop(1).dropLast(1)
        Log.i(TAG, "cmd list: $cmdList")
        var colCount = 0
        var docCount = 0
        cmdList.forEach { item ->
            if (item.contains("collection")) {
                colCount += 1
            } else if (item.contains("document")) {
                docCount += 1
            }
        }
        Log.i(TAG, "colCount: $colCount")
        Log.i(TAG, "docCount: $docCount")
        var myObjList = mutableListOf<Any>()
        var myIdList = mutableListOf<String>()

        if (colCount == 1) {
            if (docCount == 0) {
                if (cmdList.size == 1) {
                    firestoreDb
                        .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                        .get()
                        .addOnSuccessListener { snapshots ->
                            snapshots.forEach { shot ->
                                myObjList.add(shot.getData())
                                myIdList.add(shot.id)
                            }
                            Log.i(TAG, "$myIdList")
                            Log.i(TAG, "$myObjList")
                        }
                } else if (cmdList.size == 2) {
                    if (cmdList[1].contains("whereIn")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[1].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[1].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myObjList.add(shot.getData())
                                    myIdList.add(shot.id)
                                }
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                            }
                    } else if (cmdList[1].contains("whereEqualTo")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .whereEqualTo("${cmdList[1].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[1].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myObjList.add(shot.getData())
                                    myIdList.add(shot.id)
                                }
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                            }
                    }
                } else if (cmdList.size == 3) {
                    if (cmdList[1].contains("whereIn") && cmdList[2].contains("whereEqualTo")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[1].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[1].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .whereEqualTo("${cmdList[2].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[2].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myObjList.add(shot.getData())
                                    myIdList.add(shot.id)
                                }
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                            }
                    } else if (cmdList[1].contains("whereEqualTo") && cmdList[2].contains("whereIn")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .whereEqualTo("${cmdList[1].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[1].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[2].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[2].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myObjList.add(shot.getData())
                                    myIdList.add(shot.id)
                                }
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                            }
                    }
                }
            } else if (docCount == 1) {
                firestoreDb
                    .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                    .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                    .get()
                    .addOnSuccessListener { shot ->
                        myObjList.add(shot.getData()!!)
                        myIdList.add(shot.id)
                        Log.i(TAG, "$myIdList")
                        Log.i(TAG, "$myObjList")
                    }
            }
        } else if (colCount == 2) {
            if (docCount == 1) {
                if (cmdList.size == 3) {
                    firestoreDb
                        .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                        .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                        .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                        .get()
                        .addOnSuccessListener { snapshots ->
                            snapshots.forEach { shot ->
                                myObjList.add(shot.getData())
                                myIdList.add(shot.id)
                            }
                            Log.i(TAG, "$myIdList")
                            Log.i(TAG, "$myObjList")
                        }
                } else if (cmdList.size == 4) {
                    if (cmdList[3].contains("whereIn")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myObjList.add(shot.getData())
                                    myIdList.add(shot.id)
                                }
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                            }
                    } else if (cmdList[3].contains("whereEqualTo")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereEqualTo("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myObjList.add(shot.getData())
                                    myIdList.add(shot.id)
                                }
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                            }
                    }
                } else if (cmdList.size == 5) {
                    if (cmdList[3].contains("whereIn") && cmdList[4].contains("whereEqualTo")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .whereEqualTo("${cmdList[4].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[4].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myObjList.add(shot.getData())
                                    myIdList.add(shot.id)
                                }
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                            }
                    } else if (cmdList[3].contains("whereEqualTo") && cmdList[4].contains("whereIn")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereEqualTo("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[4].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[4].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myObjList.add(shot.getData())
                                    myIdList.add(shot.id)
                                }
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                            }
                    }
                }
            } else if (docCount == 2) {
                firestoreDb
                    .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                    .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                    .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                    .document("${cmdList[3].substringAfter("'").substringBefore("'")}")
                    .get()
                    .addOnSuccessListener { shot ->
                        myObjList.add(shot.getData()!!)
                        myIdList.add(shot.id)
                        Log.i(TAG, "$myIdList")
                        Log.i(TAG, "$myObjList")
                    }
            }
        }
    }

}