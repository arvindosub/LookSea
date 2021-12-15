package com.arvind.looksea

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arvind.looksea.models.Post
import com.bumptech.glide.Glide
import java.math.BigInteger
import java.security.MessageDigest

class PostsAdapter (val context: Context, private val posts: List<Post>) : RecyclerView.Adapter<PostsAdapter.ViewHolder>() {

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
            Glide.with(context).load(post.imageUrl).into(itemView.findViewById<ImageView>(R.id.ivPost))
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