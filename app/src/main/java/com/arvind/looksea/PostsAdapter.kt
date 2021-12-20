package com.arvind.looksea

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.arvind.looksea.models.Post
import com.bumptech.glide.Glide
import java.math.BigInteger
import java.security.MessageDigest

class PostsAdapter (val context: Context, private val posts: List<Post>) : RecyclerView.Adapter<PostsAdapter.ViewHolder>() {

    private var mediaControls: MediaController? = null
    private var myVideo: VideoView? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_post, viewGroup, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(post: Post) {
            val username = post.user?.username as String
            itemView.findViewById<TextView>(R.id.tvUsername).text = post.user?.username
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
            Glide.with(context).load(getProfileImageUrl(username)).into(itemView.findViewById<ImageView>(R.id.ivProfileImage))
            itemView.findViewById<TextView>(R.id.tvRelativeTime).text = DateUtils.getRelativeTimeSpanString(post.creationTimeMs)
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