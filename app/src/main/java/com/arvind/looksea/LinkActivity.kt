package com.arvind.looksea

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivityLinkBinding
import com.arvind.looksea.models.Item
import com.arvind.looksea.models.Link
import com.google.firebase.firestore.FieldPath

private const val TAG = "LinkActivity"

class LinkActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var userId: String? = ""
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivityLinkBinding
    private lateinit var search: MutableList<Item>
    private lateinit var adapterSearch: ItemAdapter
    private var searchList = mutableListOf<Item>()
    private var searchIdList = mutableListOf<String>()
    private var idList = mutableListOf<String>()
    private var viewableIds = mutableListOf<String>()
    private var chosenId: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLinkBinding.inflate(layoutInflater)
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
                                                                                    if (!myPost.privacy!!.contains("/pub1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                                                                                if ((myPost.userId in friendList || doc.id in friendList) && doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    if (!myPost.privacy!!.contains("/frds1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if ((myPost.privacy!!.contains("/fof1")) || (myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                                                                                if ((myPost.userId in fofList || doc.id in fofList) && doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    if (!myPost.privacy!!.contains("/fof1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                                                searchList.add(myPost)
                                                                                searchIdList.add(doc.id)
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
                                                                                    if (!myPost.privacy!!.contains("/pub1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                                                                                if ((myPost.userId in friendList || doc.id in friendList) && doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    if (!myPost.privacy!!.contains("/frds1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if ((myPost.privacy!!.contains("/fof1")) || (myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                                                                                if ((myPost.userId in fofList || doc.id in fofList) && doc.id !in searchIdList) {
                                                                                    searchList.add(myPost)
                                                                                    searchIdList.add(doc.id)
                                                                                    if (!myPost.privacy!!.contains("/fof1")) {
                                                                                        viewableIds.add(doc.id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                                                searchList.add(myPost)
                                                                                searchIdList.add(doc.id)
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
                                                                if (!myPost.privacy!!.contains("/pub1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }

                                                        if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                                                            if ((myPost.userId in friendList || doc.id in friendList) && doc.id !in searchIdList) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                if (!myPost.privacy!!.contains("/frds1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }

                                                        if ((myPost.privacy!!.contains("/fof1")) || (myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                                                            if ((myPost.userId in fofList || doc.id in fofList) && doc.id !in searchIdList) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
                                                                if (!myPost.privacy!!.contains("/fof1")) {
                                                                    viewableIds.add(doc.id)
                                                                }
                                                            }
                                                        }

                                                        if (doc.id in granularReadList && doc.id !in searchIdList) {
                                                            searchList.add(myPost)
                                                            searchIdList.add(doc.id)
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
                                                .whereIn("type", mutableListOf(searchType))
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
                                        firestoreDb.collection("artifacts").whereIn("type", mutableListOf("user", "image", "video", "text", "audio"))
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
                                                                    if (!myPost.privacy!!.contains("/pub1")) {
                                                                        viewableIds.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                            if ((myPost.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
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
                                                                    if (!myPost.privacy!!.contains("/frds1")) {
                                                                        viewableIds.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                            if ((myPost.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
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
                                                                    if (!myPost.privacy!!.contains("/fof1")) {
                                                                        viewableIds.add(doc.id)
                                                                    }
                                                                }
                                                            }
                                                            if ((myPost.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                                searchList.add(myPost)
                                                                searchIdList.add(doc.id)
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
                                                                viewableIds.add(doc.id)
                                                            }
                                                        }
                                                        if ((myPost.username.toString().contains(it.toString(), ignoreCase = true)) && (doc.id !in searchIdList)) {
                                                            searchList.add(myPost)
                                                            searchIdList.add(doc.id)
                                                            viewableIds.add(doc.id)
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
                adapterSearch.setOnItemClickListener(object : ItemAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        if (searchIdList[position] in viewableIds) {
                            chosenId = searchIdList[position]
                            var myId = searchList[position].username.toString()
                            binding.etLinkedItem.setText(myId)
                            Log.i(TAG, "$myId")
                        } else {
                            Toast.makeText(this@LinkActivity, "You do not have permission to link to this page!", Toast.LENGTH_SHORT).show()
                        }
                    }
                })

                binding.btnSubmit.setOnClickListener {
                    handleSubmitButtonClick()
                }

            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }
    }

    private fun handleSubmitButtonClick() {
        binding.btnSubmit.isEnabled = false
        var artifactId = intent.getStringExtra(EXTRA_ARTIFACTID)
        var searchId = chosenId.toString()
        var linkName = binding.etLinkName.text.toString()
        var linkCollName = "linked"
        if (searchId.length == 28 && artifactId!!.length == 28) {
            linkCollName = "friend"
        } else if (searchId.length == 28 || artifactId!!.length == 28) {
            linkCollName = "liked"
        }
        if (binding.etLinkName.text.isBlank() || binding.etLinkedItem.text.isBlank()) {
            Toast.makeText(this, "Using default link names.", Toast.LENGTH_SHORT).show()
            linkName = "default"
            Log.i(TAG, "${searchId.length}, ${artifactId!!.length}")
            if (searchId.length == 28 && artifactId!!.length == 28) {
                linkName = "friend"
            } else if (searchId.length == 28 || artifactId!!.length == 28) {
                linkName = "liked"
            }
        }
        var linkVal = Link(
            linkName,
            "$userId"
        )

        Log.i(TAG, "artifact id: $artifactId")
        Log.i(TAG, "search id: $searchId")

        firestoreDb.collection("links").document(artifactId as String)
            .collection("$linkCollName").document(searchId).set(linkVal)
            .addOnCompleteListener {
                artifactId = searchId
                searchId = intent.getStringExtra(EXTRA_ARTIFACTID).toString()
                var linkVal = Link(
                    linkName,
                    "$userId"
                )
                firestoreDb.collection("links").document(artifactId as String)
                    .collection("$linkCollName").document(searchId).set(linkVal)
                    .addOnCompleteListener { linkCreationTask ->
                        if (artifactId!!.length == 28 && searchId.length != 28) {
                            firestoreDb.collection("artifacts").document(searchId).get()
                                .addOnSuccessListener { postSnapshot ->
                                    var post = postSnapshot.toObject(Post::class.java)
                                    firestoreDb.collection("artifacts").document(searchId).update("likes", post!!.likes+1)
                                }
                        } else if (searchId.length == 28 && artifactId!!.length != 28) {
                            firestoreDb.collection("artifacts").document(artifactId!!).get()
                                .addOnSuccessListener { postSnapshot ->
                                    var post = postSnapshot.toObject(Post::class.java)
                                    firestoreDb.collection("artifacts").document(artifactId!!).update("likes", post!!.likes+1)
                                }
                        }
                        binding.btnSubmit.isEnabled = true
                        if (!linkCreationTask.isSuccessful) {
                            Log.e(TAG, "Exception during Firebase operations", linkCreationTask.exception)
                            Toast.makeText(this, "Failed to link post...", Toast.LENGTH_SHORT).show()
                        }
                        Toast.makeText(this, "Link created!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
    }

}