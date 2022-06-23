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
import com.arvind.looksea.models.Link
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
    private lateinit var imageIds: MutableList<String>
    private lateinit var adapterImages: FileAdapter
    private lateinit var imageGridView: GridView
    private fun loadImages(friendlist: MutableList<String> = ArrayList(), foflist: MutableList<String> = ArrayList(), granularreadlist: MutableList<String> = ArrayList()) {
        images = mutableListOf()
        imageIds = mutableListOf()
        imageGridView = binding.gvImages
        adapterImages = FileAdapter(this, images)
        imageGridView.adapter = adapterImages

        firestoreDb.collection("artifacts").whereEqualTo("user", currUserId)
            .whereEqualTo("type", "image")
            .get()
            .addOnSuccessListener { myImages ->
                var imgList = mutableListOf<Post>()
                var imgIdList = mutableListOf<String>()
                myImages.forEach { doc ->
                    var myPost = doc.toObject(Post::class.java)
                    Log.i(TAG, "Post ID: ${doc.id}")
                    Log.i(TAG, "Post: $myPost")
                    if ((myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                        if (doc.id !in imgIdList) {
                            imgList.add(myPost)
                            imgIdList.add(doc.id)
                        }
                    }

                    if ((myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                        if (myPost.userId in friendlist && doc.id !in imgIdList) {
                            imgList.add(myPost)
                            imgIdList.add(doc.id)
                        }
                    }

                    if ((myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                        if (myPost.userId in foflist && doc.id !in imgIdList) {
                            imgList.add(myPost)
                            imgIdList.add(doc.id)
                        }
                    }

                    if (doc.id in granularreadlist && doc.id !in imgIdList) {
                        imgList.add(myPost)
                        imgIdList.add(doc.id)
                    }
                }
                images.clear()
                images.addAll(imgList)
                imageIds.clear()
                imageIds.addAll(imgIdList)
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
    private lateinit var videoIds: MutableList<String>
    private lateinit var adapterVideos: FileAdapter
    private lateinit var videoGridView: GridView
    private fun loadVideos(friendlist: MutableList<String> = ArrayList(), foflist: MutableList<String> = ArrayList(), granularreadlist: MutableList<String> = ArrayList()) {
        videos = mutableListOf()
        videoIds = mutableListOf()
        videoGridView = binding.gvVideos
        adapterVideos = FileAdapter(this, videos)
        videoGridView.adapter = adapterVideos

        firestoreDb.collection("artifacts").whereEqualTo("user", currUserId)
            .whereEqualTo("type", "video")
            .get()
            .addOnSuccessListener { myImages ->
                var vidList = mutableListOf<Post>()
                var vidIdList = mutableListOf<String>()
                myImages.forEach { doc ->
                    var myPost = doc.toObject(Post::class.java)
                    Log.i(TAG, "Post ID: ${doc.id}")
                    Log.i(TAG, "Post: $myPost")
                    if ((myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                        if (doc.id !in vidIdList) {
                            vidList.add(myPost)
                            vidIdList.add(doc.id)
                        }
                    }

                    if ((myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                        if (myPost.userId in friendlist && doc.id !in vidIdList) {
                            vidList.add(myPost)
                            vidIdList.add(doc.id)
                        }
                    }

                    if ((myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                        if (myPost.userId in foflist && doc.id !in vidIdList) {
                            vidList.add(myPost)
                            vidIdList.add(doc.id)
                        }
                    }

                    if (doc.id in granularreadlist && doc.id !in vidIdList) {
                        vidList.add(myPost)
                        vidIdList.add(doc.id)
                    }
                }
                videos.clear()
                videos.addAll(vidList)
                videoIds.clear()
                videoIds.addAll(vidIdList)
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
    private lateinit var audioIds: MutableList<String>
    private lateinit var adapterAudio: FileAdapter
    private lateinit var audioGridView: GridView
    private fun loadAudio(friendlist: MutableList<String> = ArrayList(), foflist: MutableList<String> = ArrayList(), granularreadlist: MutableList<String> = ArrayList()) {
        audio = mutableListOf()
        audioIds = mutableListOf()
        audioGridView = binding.gvAudio
        adapterAudio = FileAdapter(this, audio)
        audioGridView.adapter = adapterAudio

        firestoreDb.collection("artifacts").whereEqualTo("user", currUserId)
            .whereEqualTo("type", "audio")
            .get()
            .addOnSuccessListener { myImages ->
                var audList = mutableListOf<Post>()
                var audIdList = mutableListOf<String>()
                myImages.forEach { doc ->
                    var myPost = doc.toObject(Post::class.java)
                    Log.i(TAG, "Post ID: ${doc.id}")
                    Log.i(TAG, "Post: $myPost")
                    if ((myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                        if (doc.id !in audIdList) {
                            audList.add(myPost)
                            audIdList.add(doc.id)
                        }
                    }

                    if ((myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                        if (myPost.userId in friendlist && doc.id !in audIdList) {
                            audList.add(myPost)
                            audIdList.add(doc.id)
                        }
                    }

                    if ((myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                        if (myPost.userId in foflist && doc.id !in audIdList) {
                            audList.add(myPost)
                            audIdList.add(doc.id)
                        }
                    }

                    if (doc.id in granularreadlist && doc.id !in audIdList) {
                        audList.add(myPost)
                        audIdList.add(doc.id)
                    }
                }
                audio.clear()
                audio.addAll(audList)
                audioIds.clear()
                audioIds.addAll(audIdList)
                adapterAudio.notifyDataSetChanged()

                audioGridView.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        val intent = Intent(this@ProfileActivity, PostActivity::class.java)
                        intent.putExtra(EXTRA_POSTTIME, audio[position].creationTimeMs.toString())
                        startActivity(intent)
                    }
            }
    }

    private lateinit var text: MutableList<Post>
    private lateinit var textIds: MutableList<String>
    private lateinit var adapterText: FileAdapter
    private lateinit var textGridView: GridView
    private fun loadText(friendlist: MutableList<String> = ArrayList(), foflist: MutableList<String> = ArrayList(), granularreadlist: MutableList<String> = ArrayList()) {
        text = mutableListOf()
        textIds = mutableListOf()
        textGridView = binding.gvText
        adapterText = FileAdapter(this, text)
        textGridView.adapter = adapterText

        firestoreDb.collection("artifacts").whereEqualTo("user", currUserId)
            .whereEqualTo("type", "text")
            .get()
            .addOnSuccessListener { myImages ->
                var texList = mutableListOf<Post>()
                var texIdList = mutableListOf<String>()
                myImages.forEach { doc ->
                    var myPost = doc.toObject(Post::class.java)
                    Log.i(TAG, "Post ID: ${doc.id}")
                    Log.i(TAG, "Post: $myPost")
                    if ((myPost.privacy!!.contains("/pub2")) || (myPost.privacy!!.contains("/pub3")) || (myPost.privacy!!.contains("/pub4"))) {
                        if (doc.id !in texIdList) {
                            texList.add(myPost)
                            texIdList.add(doc.id)
                        }
                    }

                    if ((myPost.privacy!!.contains("/frds2")) || (myPost.privacy!!.contains("/frds3")) || (myPost.privacy!!.contains("/frds4"))) {
                        if (myPost.userId in friendlist && doc.id !in texIdList) {
                            texList.add(myPost)
                            texIdList.add(doc.id)
                        }
                    }

                    if ((myPost.privacy!!.contains("/fof2")) || (myPost.privacy!!.contains("/fof3")) || (myPost.privacy!!.contains("/fof4"))) {
                        if (myPost.userId in foflist && doc.id !in texIdList) {
                            texList.add(myPost)
                            texIdList.add(doc.id)
                        }
                    }

                    if (doc.id in granularreadlist && doc.id !in texIdList) {
                        texList.add(myPost)
                        texIdList.add(doc.id)
                    }
                }
                text.clear()
                text.addAll(texList)
                textIds.clear()
                textIds.addAll(texIdList)
                adapterText.notifyDataSetChanged()

                textGridView.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        val intent = Intent(this@ProfileActivity, PostActivity::class.java)
                        intent.putExtra(EXTRA_POSTTIME, text[position].creationTimeMs.toString())
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
        firestoreDb.collection("artifacts")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed-In User: $signedInUser")
                firestoreDb.collection("artifacts")
                    .whereEqualTo("type", "user")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        userSnapshot.forEach { doc ->
                            currUser = doc.toObject((User::class.java))
                            currUserId = doc.id
                        }
                        binding.profileDescription.text = currUser?.description
                        if (currUser?.file_url != "") {
                            Glide.with(applicationContext).load(currUser?.file_url).into(binding.profilePicture)
                        } else {
                            Glide.with(applicationContext).load("https://firebasestorage.googleapis.com/v0/b/looksea-43f7d.appspot.com/o/profilepics%2Fdefault_icon.png?alt=media&token=7e6d6755-726d-4f02-ae75-f74cda6dd748").into(binding.profilePicture)
                        }
                        Log.i(TAG, "Current User: $currUser")

                        firestoreDb.collection("links").document(userId as String)
                            .collection("sentrequest").document(currUserId as String)
                            .get()
                            .addOnSuccessListener { sentResult ->
                                var contact = false
                                Log.i(TAG, "SENT: ${sentResult.data}")
                                if (sentResult.data != null) {
                                    contact = true
                                }

                                firestoreDb.collection("links").document(userId as String)
                                    .collection("receivedrequest").document(currUserId as String)
                                    .get()
                                    .addOnSuccessListener { receivedResult ->
                                        Log.i(TAG, "RECEIVED: ${receivedResult.data}")
                                        if (receivedResult.data != null) {
                                            contact = true
                                            requestReceiver = true
                                        }

                                        firestoreDb.collection("links").document(userId as String)
                                            .collection("friend").document(currUserId as String)
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
                                                        val createIntent =
                                                            Intent(this, CreateActivity::class.java)
                                                        startActivity(createIntent)
                                                    }
                                                    val particularsIntent = Intent(
                                                        this,
                                                        ParticularsActivity::class.java
                                                    )
                                                    binding.profilePicture.setOnClickListener {
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

                                                                Log.i(
                                                                    TAG,
                                                                    "Friends List: $friendList"
                                                                )
                                                                Log.i(TAG, "FoF List: $fofList")
                                                                Log.i(
                                                                    TAG,
                                                                    "Granular Read List: $granularReadList"
                                                                )

                                                                loadImages(friendList, fofList, granularReadList)
                                                                loadVideos(friendList, fofList, granularReadList)
                                                                loadText(friendList, fofList, granularReadList)
                                                                loadAudio(friendList, fofList, granularReadList)
                                                                binding.fabLink.setOnClickListener {
                                                                    handleLinkButtonClick()
                                                                }
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }

    }

    private fun handleLinkButtonClick() {
        val id = currUserId
        val intent = Intent(this, LinkActivity::class.java)
        intent.putExtra(EXTRA_ARTIFACTID, id)
        startActivity(intent)
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

    private fun sendFriendRequest() {
        val curr = currUser
        val sign = signedInUser
        if (curr != null) {
            firestoreDb.collection("links").document(userId as String)
                .collection("sentrequest").document(currUserId as String).set(curr)
        }
        if (sign != null) {
            firestoreDb.collection("links").document(currUserId as String)
                .collection("receivedrequest").document(userId as String).set(sign)
        }

        Toast.makeText(this, "Friend Request Sent...", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(getIntent())
    }

    private fun rejectFriendRequest() {
        if (requestReceiver == true) {
            firestoreDb.collection("links").document(userId as String)
                .collection("receivedrequest").document(currUserId as String).delete()

            firestoreDb.collection("links").document(currUserId as String)
                .collection("sentrequest").document(userId as String).delete()

            Toast.makeText(this, "Friend Request Rejected...", Toast.LENGTH_SHORT).show()

        } else {
            firestoreDb.collection("links").document(userId as String)
                .collection("sentrequest").document(currUserId as String).delete()

            firestoreDb.collection("links").document(currUserId as String)
                .collection("receivedrequest").document(userId as String).delete()

            Toast.makeText(this, "Friend Request Cancelled...", Toast.LENGTH_SHORT).show()

        }
        finish()
        val intent = Intent(this, SocialActivity::class.java)
        startActivity(intent)
    }

    private fun acceptFriendRequest() {
        val curr = currUser
        val sign = signedInUser
        //firestoreDb.collection("friendlists").document(userId as String).collection("myfriends").document(currUserId as String).set(curr!!)
        //firestoreDb.collection("friendlists").document(currUserId as String).collection("myfriends").document(userId as String).set(sign!!)

        firestoreDb.collection("links").document(userId as String)
            .collection("friend").document(currUserId as String).set(Link("friend","$userId"))
        firestoreDb.collection("links").document(currUserId as String)
            .collection("friend").document(userId as String).set(Link("friend","$currUserId"))

        firestoreDb.collection("links").document(userId as String)
            .collection("receivedrequest").document(currUserId as String).delete()

        firestoreDb.collection("links").document(currUserId as String)
            .collection("sentrequest").document(userId as String).delete()

        Toast.makeText(this, "Friend Request Accepted...", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(getIntent())
    }

    private fun removeFriend() {
        //firestoreDb.collection("friendlists").document(userId as String).collection("myfriends").document(currUserId as String).delete()
        //firestoreDb.collection("friendlists").document(currUserId as String).collection("myfriends").document(userId as String).delete()

        firestoreDb.collection("links").document(userId as String)
            .collection("friend").document(currUserId as String).delete()
        firestoreDb.collection("links").document(currUserId as String)
            .collection("friend").document(userId as String).delete()

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