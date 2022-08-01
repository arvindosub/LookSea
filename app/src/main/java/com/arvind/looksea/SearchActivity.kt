package com.arvind.looksea

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.isVisible
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.looksea.databinding.ActivitySearchBinding
import com.arvind.looksea.models.Item
import com.arvind.looksea.models.Post
import com.google.firebase.firestore.FieldPath

private const val TAG = "SearchActivity"

class SearchActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivitySearchBinding
    private lateinit var search: MutableList<Item>
    private lateinit var adapterSearch: ItemAdapter
    private var searchList = mutableListOf<Item>()
    private var searchIdList = mutableListOf<String>()
    private var viewableIds = mutableListOf<String>()
    private var userId: String? = ""
    private var selUserId: String? = ""
    private var selUser: Item? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        search = mutableListOf()
        adapterSearch = ItemAdapter(this, search)
        binding.rvSearch.adapter = adapterSearch
        binding.rvSearch.layoutManager = LinearLayoutManager(this)
        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("artifacts")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
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

                                                                            if ((myPost.privacy!!.contains("/frds1")) || (myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))
                                                                            ) {
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
                        val item = searchList[position]
                        val itemId = searchIdList[position]
                        Log.i(TAG, "$item")

                        if (binding.etXpathCode.text.toString() != "" && binding.etSearch.text.toString() != "") {
                            if (searchIdList[position] in viewableIds) {
                                selUserId = searchIdList[position]
                                selUser = searchList[position]
                                binding.etXpathCode.text.append("'$selUserId'")
                                binding.etSearch.text.clear()
                                search.clear()
                                adapterSearch.notifyDataSetChanged()
                            } else {
                                Toast.makeText(this@SearchActivity, "You do not have permission to access this page!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (itemId in viewableIds) {
                                if (item.type == "user") {
                                    val intent = Intent(this@SearchActivity, ProfileActivity::class.java)
                                    intent.putExtra(EXTRA_USERNAME, item.username)
                                    startActivity(intent)
                                } else {
                                    val intent = Intent(this@SearchActivity, PostActivity::class.java)
                                    intent.putExtra(EXTRA_POSTTIME, item.creationTimeMs.toString())
                                    startActivity(intent)
                                }
                            } else {
                                Toast.makeText(this@SearchActivity, "You do not have permission to view this page!", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                })

                binding.btnSearch.setOnClickListener {
                    handleXPathSearch()
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

    private fun handleXPathSearch() {
        binding.btnSearch.isEnabled = false
        binding.etSearch.text.append("a")
        binding.etSearch.text.clear()
        var (cmdStr, subCmdStr) = getCommandString(binding.etXpathCode.text.toString())
        executeFirebaseCommand(cmdStr, subCmdStr)
        binding.btnSearch.isEnabled = true
    }

    private fun getCommandString (expression: String): Pair<String, String> {
        var items = expression.split('/')
        var myList = mutableListOf<MutableList<String>>()
        var startPt = mutableListOf<String>()
        startPt.add("user")
        startPt.add(userId.toString())
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
        Log.i(TAG, "first item: $startPt")
        Log.i(TAG, "input list: $myList")

        var cmdStr = "firestoreDb.collection("
        var subCmdStr = "firestoreDb.collection("
        if (myList.size == 1) {
            myList[0].forEach { subStep ->
                if (subStep.contains("@") && subStep.contains("=")) {
                    var tempSubStepList = subStep.drop(1).split("=")
                    cmdStr += "whereEqualTo('${tempSubStepList[0]}', '${subStep.substringAfter("'").substringBefore("'")}')."
                } else if (subStep.contains("contains")) {
                    var subField = subStep.substringAfter("@").substringBefore(",")
                    if (subField == "keywords") {
                        cmdStr += "whereArrayContains('$subField', '${subStep.substringAfter("'").substringBefore("'")}')."
                    } else {
                        cmdStr += "whereIn('$subField', mutableListOf('${subStep.substringAfter("'").substringBefore("'")}'))."
                    }
                } else {
                    if (subStep.contains("descendant")) {
                        var tempSubStepList = subStep.replace("::",":").split(":")
                        if (subStep.contains("user")) {
                            cmdStr += "'artifacts').whereEqualTo('type', '${tempSubStepList[1]}')."
                        } else {
                            cmdStr += "'links').document('${startPt[1].substringAfter("'").substringBefore("'")}').collection('${tempSubStepList[1]}')."
                        }
                    } else if (subStep.contains("child")) {
                        var tempSubStepList = subStep.replace("::",":").split(":")
                        cmdStr += "'links').document('${startPt[1].substringAfter("'").substringBefore("'")}').collection('${tempSubStepList[1]}')."
                    }
                }
            }
        } else if (myList.size == 2) {
            myList[0].forEach { subStep ->
                if (subStep.contains("::")) {
                    var tempSubStepList = subStep.replace("::",":").split(":")
                    if (tempSubStepList[0] != "descendant" && tempSubStepList[1] !in mutableListOf<String>("user", "post", "image", "video", "audio", "text")) {
                        cmdStr += "'links').document('${startPt[1].substringAfter("'").substringBefore("'")}').collection('${tempSubStepList[1]}')."
                    }
                } else if (subStep.contains("@") && subStep.contains("=")) {
                    cmdStr += "whereEqualTo('${subStep.substringAfter("@").substringBefore("=")}', '${subStep.substringAfter("'").substringBefore("'")}')."
                    //cmdStr += "document('${subStep.substringAfter("'").substringBefore("'")}')."
                    if (subStep.substringAfter("'").substringBefore("'").length == 28) {
                        startPt.add("'${subStep.substringAfter("'").substringBefore("'")}'")
                    }
                } else if (subStep.contains("contains")) {
                    var subField = subStep.substringAfter("@").substringBefore(",")
                    if (subField == "keywords") {
                        cmdStr += "whereArrayContains('$subField', '${subStep.substringAfter("'").substringBefore("'")}')."
                    } else {
                        cmdStr += "whereIn('$subField', mutableListOf('${subStep.substringAfter("'").substringBefore("'")}'))."
                    }
                }
            }
            myList[1].forEach { subStep ->
                if (subStep.contains("::")) {
                    var tempSubStepList = subStep.replace("::",":").split(":")
                    if (tempSubStepList[0] == "descendant") {
                        cmdStr == cmdStr
                        if (tempSubStepList[1] !in mutableListOf<String>("user", "post", "image", "video", "audio", "text")) {
                            subCmdStr += "'links').document('${startPt[2].substringAfter("'").substringBefore("'")}').collection('${tempSubStepList[1]}')."
                        }
                    }
                } else if (subStep.contains("@") && subStep.contains("=")) {
                    var tempSubStepList = subStep.drop(1).split("=")
                    if (tempSubStepList[0] == "id") {
                        cmdStr += "document('${tempSubStepList[1]}')."
                    } else {
                        if (tempSubStepList[0] == "user") {
                            cmdStr += "whereEqualTo('endOwner', '${tempSubStepList[1].substringAfter("'").substringBefore("'")}')."
                        } else {
                            //cmdStr += "whereEqualTo('${tempSubStepList[0]}', '${tempSubStepList[1].substringAfter("'").substringBefore("'")}')."
                            cmdStr = cmdStr
                            if (tempSubStepList[0] !in mutableListOf<String>("description", "user", "username", "type", "location", "privacy")) {
                                subCmdStr += "'links').document('resultId').collection('${tempSubStepList[1]}')."
                            } else {
                                subCmdStr += "'artifacts').whereEqualTo('${tempSubStepList[0]}', '${tempSubStepList[1].substringAfter("'").substringBefore("'")}')."
                            }
                        }
                    }
                } else if (subStep.contains("contains")) {
                    var subField = subStep.substringAfter("@").substringBefore(",")
                    if (subField == "keywords") {
                        cmdStr += "whereArrayContains('$subField', '${subStep.substringAfter("'").substringBefore("'")}')."
                    } else {
                        cmdStr += "whereIn('$subField', mutableListOf('${subStep.substringAfter("'").substringBefore("'")}'))."
                    }
                }
            }
        }
        cmdStr += "get()"
        subCmdStr += "get()"
        return Pair(cmdStr, subCmdStr)
    }

    private fun executeFirebaseCommand (cmdStr: String, subCmdStr: String) {
        var cmdList = cmdStr.split(".").drop(1).dropLast(1)
        var subCmdList = subCmdStr.split(".").drop(1).dropLast(1)
        Log.i(TAG, "cmd list: $cmdList")
        Log.i(TAG, "sub cmd list: $subCmdList")
        var colCount = 0
        var docCount = 0
        var subColCount = 0
        var subDocCount = 0
        cmdList.forEach { item ->
            if (item.contains("collection")) {
                colCount += 1
            } else if (item.contains("document")) {
                docCount += 1
            }
        }
        subCmdList.forEach { item ->
            if (item.contains("collection")) {
                subColCount += 1
            } else if (item.contains("document")) {
                subDocCount += 1
            }
        }
        Log.i(TAG, "colCount: $colCount")
        Log.i(TAG, "docCount: $docCount")
        Log.i(TAG, "subColCount: $subColCount")
        Log.i(TAG, "subDocCount: $subDocCount")
        var myObjList = mutableListOf<Item>()
        var myIdList = mutableListOf<String>()

        if (colCount == 1) {
            if (docCount == 0) {
                if (cmdList.size == 1) {
                    firestoreDb
                        .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                        .get()
                        .addOnSuccessListener { snapshots ->
                            snapshots.forEach { shot ->
                                myIdList.add(shot.id)
                            }
                            firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                .addOnSuccessListener { temps ->
                                    temps.forEach { temp ->
                                        myObjList.add(temp.toObject(Item::class.java))
                                    }
                                    Log.i(TAG, "$myIdList")
                                    Log.i(TAG, "$myObjList")
                                    search.clear()
                                    search.addAll(myObjList)
                                    searchList = myObjList
                                    searchIdList = myIdList
                                    adapterSearch.notifyDataSetChanged()
                                }
                        }
                } else if (cmdList.size == 2) {
                    if (cmdList[1].contains("whereIn")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[1].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[1].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
                            }
                    } else if (cmdList[1].contains("whereEqualTo")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .whereEqualTo("${cmdList[1].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[1].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
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
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
                            }
                    } else if (cmdList[1].contains("whereEqualTo") && cmdList[2].contains("whereIn")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .whereEqualTo("${cmdList[1].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[1].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[2].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[2].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
                            }
                    }
                }
            } else if (docCount == 1) {
                firestoreDb
                    .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                    .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                    .get()
                    .addOnSuccessListener { shot ->
                        firestoreDb.collection("artifacts").document("${shot.id}").get()
                            .addOnSuccessListener { temp ->
                                myObjList.add(temp.toObject(Item::class.java)!!)
                                myIdList.add(shot.id)
                                Log.i(TAG, "$myIdList")
                                Log.i(TAG, "$myObjList")
                                search.clear()
                                search.addAll(myObjList)
                                searchList = myObjList
                                searchIdList = myIdList
                                adapterSearch.notifyDataSetChanged()
                            }
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
                                myIdList.add(shot.id)
                            }
                            firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                .addOnSuccessListener { temps ->
                                    temps.forEach { temp ->
                                        myObjList.add(temp.toObject(Item::class.java))
                                    }
                                    if (subCmdStr.length > 28) {
                                        if (subCmdList.size == 2) {
                                            if (subCmdList[1].contains("whereEqualTo")) {
                                                firestoreDb
                                                    .collection("${subCmdList[0].substringAfter("'").substringBefore("'")}")
                                                    .whereEqualTo("${subCmdList[1].split(",")[0].substringAfter("'").substringBefore("'")}", "${subCmdList[1].split(",")[1].substringAfter("'").substringBefore("'")}")
                                                    .get()
                                                    .addOnSuccessListener { subSnapshots ->
                                                        var subIdList = mutableListOf<String>()
                                                        var subObjList = mutableListOf<Item>()
                                                        subSnapshots.forEach { subShot ->
                                                            if (subShot.id in myIdList) {
                                                                subIdList.add(subShot.id)
                                                            }
                                                        }
                                                        firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), subIdList).get()
                                                            .addOnSuccessListener { subtemps ->
                                                                subtemps.forEach { subtemp ->
                                                                    subObjList.add(subtemp.toObject(Item::class.java))
                                                                }
                                                                myIdList = subIdList
                                                                myObjList = subObjList
                                                                Log.i(TAG, "$myIdList")
                                                                Log.i(TAG, "$myObjList")
                                                                search.clear()
                                                                search.addAll(myObjList)
                                                                searchList = myObjList
                                                                searchIdList = myIdList
                                                                adapterSearch.notifyDataSetChanged()
                                                            }
                                                    }
                                            } else if (subCmdList[1].contains("whereIn")) {
                                                firestoreDb
                                                    .collection("${subCmdList[0].substringAfter("'").substringBefore("'")}")
                                                    .whereIn("${subCmdList[1].substringAfter("@").substringBefore(",")}", mutableListOf("${subCmdList[1].substringAfter("'").substringBefore("'")}"))
                                                    .get()
                                                    .addOnSuccessListener { subSnapshots ->
                                                        var subIdList = mutableListOf<String>()
                                                        var subObjList = mutableListOf<Item>()
                                                        subSnapshots.forEach { subShot ->
                                                            if (subShot.id in myIdList) {
                                                                subIdList.add(subShot.id)
                                                            }
                                                        }
                                                        firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), subIdList).get()
                                                            .addOnSuccessListener { subtemps ->
                                                                subtemps.forEach { subtemp ->
                                                                    subObjList.add(subtemp.toObject(Item::class.java))
                                                                }
                                                                myIdList = subIdList
                                                                myObjList = subObjList
                                                                Log.i(TAG, "$myIdList")
                                                                Log.i(TAG, "$myObjList")
                                                                search.clear()
                                                                search.addAll(myObjList)
                                                                searchList = myObjList
                                                                searchIdList = myIdList
                                                                adapterSearch.notifyDataSetChanged()
                                                            }
                                                    }
                                            }
                                        }
                                    } else {
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
                                }
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
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
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
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        if (subCmdStr.length > 28) {
                                            if (subCmdList.size == 3) {
                                                firestoreDb
                                                    .collection("${subCmdList[0].substringAfter("'").substringBefore("'")}")
                                                    .document("${subCmdList[1].substringAfter("'").substringBefore("'")}")
                                                    .collection("${subCmdList[2].substringAfter("'").substringBefore("'")}")
                                                    .get()
                                                    .addOnSuccessListener { subSnapshots ->
                                                        var subIdList = mutableListOf<String>()
                                                        var subObjList = mutableListOf<Item>()
                                                        subSnapshots.forEach { subShot ->
                                                            subIdList.add(subShot.id)
                                                        }
                                                        firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), subIdList).get()
                                                            .addOnSuccessListener { subtemps ->
                                                                subtemps.forEach { subtemp ->
                                                                    subObjList.add(subtemp.toObject(Item::class.java))
                                                                }
                                                                myIdList = subIdList
                                                                myObjList = subObjList
                                                                Log.i(TAG, "$myIdList")
                                                                Log.i(TAG, "$myObjList")
                                                                search.clear()
                                                                search.addAll(myObjList)
                                                                searchList = myObjList
                                                                searchIdList = myIdList
                                                                adapterSearch.notifyDataSetChanged()
                                                            }
                                                    }
                                            }
                                        } else {
                                            Log.i(TAG, "$myIdList")
                                            Log.i(TAG, "$myObjList")
                                            search.clear()
                                            search.addAll(myObjList)
                                            searchList = myObjList
                                            searchIdList = myIdList
                                            adapterSearch.notifyDataSetChanged()
                                        }
                                    }
                            }
                    } else if (cmdList[3].contains("whereArrayContains")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereArrayContains("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
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
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
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
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
                            }
                    } else if (cmdList[3].contains("whereEqualTo") && cmdList[4].contains("whereArrayContains")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereEqualTo("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .whereArrayContains("${cmdList[4].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[4].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
                            }
                    } else if (cmdList[3].contains("whereArrayContains") && cmdList[4].contains("whereEqualTo")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereArrayContains("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .whereEqualTo("${cmdList[4].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[4].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
                            }
                    } else if (cmdList[3].contains("whereEqualTo") && cmdList[4].contains("whereArrayContains")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .whereArrayContains("${cmdList[4].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[4].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
                            }
                    } else if (cmdList[4].contains("whereEqualTo") && cmdList[3].contains("whereArrayContains")) {
                        firestoreDb
                            .collection("${cmdList[0].substringAfter("'").substringBefore("'")}")
                            .document("${cmdList[1].substringAfter("'").substringBefore("'")}")
                            .collection("${cmdList[2].substringAfter("'").substringBefore("'")}")
                            .whereArrayContains("${cmdList[3].split(',')[0].substringAfter("'").substringBefore("'")}", "${cmdList[3].split(',')[1].substringAfter("'").substringBefore("'")}")
                            .whereIn("${cmdList[4].split(',')[0].substringAfter("'").substringBefore("'")}", mutableListOf("${cmdList[4].split(',')[1].substringAfter("'").substringBefore("'")}"))
                            .get()
                            .addOnSuccessListener { snapshots ->
                                snapshots.forEach { shot ->
                                    myIdList.add(shot.id)
                                }
                                firestoreDb.collection("artifacts").whereIn(FieldPath.documentId().toString(), myIdList).get()
                                    .addOnSuccessListener { temps ->
                                        temps.forEach { temp ->
                                            myObjList.add(temp.toObject(Item::class.java))
                                        }
                                        Log.i(TAG, "$myIdList")
                                        Log.i(TAG, "$myObjList")
                                        search.clear()
                                        search.addAll(myObjList)
                                        searchList = myObjList
                                        searchIdList = myIdList
                                        adapterSearch.notifyDataSetChanged()
                                    }
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
                        firestoreDb.collection("artifacts").document("${shot.id}").get()
                            .addOnSuccessListener { temp ->
                                myObjList.add(temp.toObject(Item::class.java)!!)
                                myIdList.add(shot.id)
                            }
                        Log.i(TAG, "$myIdList")
                        Log.i(TAG, "$myObjList")
                        search.clear()
                        search.addAll(myObjList)
                        searchList = myObjList
                        searchIdList = myIdList
                        adapterSearch.notifyDataSetChanged()
                    }
            }
        }
    }

}