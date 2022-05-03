package com.arvind.looksea

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigInteger
import java.security.MessageDigest

class PostAdapter (val context: Context, private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    //var mediaControls: MediaController? = null
    //private var myVideo: VideoView? = null

    private lateinit var pListener : onItemClickListener
    private lateinit var lListener : onLikeClickListener
    private lateinit var uListener : onUserClickListener
    private lateinit var firestoreDb: FirebaseFirestore

    interface onItemClickListener {
        fun onItemClick(position : Int)
    }

    fun setOnItemClickListener(postListener: onItemClickListener) {
        pListener = postListener
    }

    interface onLikeClickListener {
        fun onLikeClick(position : Int)
    }

    fun setOnLikeClickListener(likeListener: onLikeClickListener) {
        lListener = likeListener
    }

    interface onUserClickListener {
        fun onUserClick(position : Int)
    }

    fun setOnUserClickListener(userListener: onUserClickListener) {
        uListener = userListener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_post, viewGroup, false)
        return ViewHolder(view, pListener, lListener, uListener)
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    inner class ViewHolder(itemView: View, postListener: onItemClickListener, likeListener: onLikeClickListener, userListener: onUserClickListener) : RecyclerView.ViewHolder(itemView) {
        private var toggleLike : ImageButton = itemView.findViewById<ImageButton>(R.id.btnLike)
        fun bind(post: Post) {
            val username = post.username as String
            itemView.findViewById<TextView>(R.id.tvUsername).text = post.username
            itemView.findViewById<TextView>(R.id.tvDescription).text = post.description

            if (post.type == "video") {
                // rendering the video on the feed consumes a lot of bandwidth.
                // removing posts using videos for now.
                /*
                itemView.findViewById<View>(R.id.vvPost).isVisible = true
                itemView.findViewById<View>(R.id.ivPost).isVisible = false
                myVideo = itemView.findViewById<View>(R.id.vvPost) as VideoView?
                if (mediaControls == null) {
                    mediaControls = MediaController(context)
                    mediaControls?.setAnchorView(myVideo)
                }
                myVideo?.setMediaController(mediaControls)
                myVideo?.setVideoURI(Uri.parse(post.fileUrl))
                myVideo?.requestFocus()
                */
                itemView.findViewById<View>(R.id.vvPost).isVisible = false
                itemView.findViewById<View>(R.id.ivPost).isVisible = true
                Glide.with(context).load(post.fileUrl).into(itemView.findViewById<ImageView>(R.id.ivPost))
                /*
                itemView.findViewById<ImageView>(R.id.ivPost).setImageResource(R.drawable.ic_video)
                */

            } else if (post.type == "image") {
                itemView.findViewById<View>(R.id.vvPost).isVisible = false
                itemView.findViewById<View>(R.id.ivPost).isVisible = true
                Glide.with(context).load(post.fileUrl).into(itemView.findViewById<ImageView>(R.id.ivPost))
            }

            itemView.findViewById<TextView>(R.id.tvRelativeTime).text = DateUtils.getRelativeTimeSpanString(post.creationTimeMs)
            itemView.findViewById<TextView>(R.id.tvLikes).text = post.likes.toString()

            var userId = ""
            firestoreDb = FirebaseFirestore.getInstance()
            firestoreDb.collection("artifacts")
                .whereEqualTo("type", "user")
                .whereEqualTo("username", post.username)
                .get()
                .addOnSuccessListener { snapshots ->
                    snapshots.forEach { doc ->
                        userId = doc.id
                    }
                    firestoreDb.collection("artifacts")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            var user = userSnapshot.toObject((User::class.java))
                            if (user?.file_url.toString() == "") {
                                Glide.with(context).load(getProfileImageUrl(username)).into(itemView.findViewById<ImageView>(R.id.ivProfileImage))
                            } else {
                                Glide.with(context).load(user?.file_url.toString()).into(itemView.findViewById<ImageView>(R.id.ivProfileImage))
                            }
                        }
                }
        }
        init {
            itemView.setOnClickListener {
                postListener.onItemClick(bindingAdapterPosition)
            }

            itemView.findViewById<ImageButton>(R.id.btnLike).setOnClickListener {
                likeListener.onLikeClick(bindingAdapterPosition)
            }

            itemView.findViewById<ImageView>(R.id.ivProfileImage).setOnClickListener {
                userListener.onUserClick(bindingAdapterPosition)
            }

            itemView.findViewById<TextView>(R.id.tvUsername).setOnClickListener {
                userListener.onUserClick(bindingAdapterPosition)
            }
        }

        private fun getProfileImageUrl(username: String): String {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(username.toByteArray())
            val bigInt = BigInteger(hash)
            val hex = bigInt.abs().toString(16)
            return "https://www.gravatar.com/avatar/$hex?d=identicon"
        }
    }
}