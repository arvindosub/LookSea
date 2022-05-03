package com.arvind.looksea

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.arvind.looksea.models.Link
import com.arvind.looksea.models.User
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigInteger
import java.security.MessageDigest

class CommentAdapter(val context: PostActivity, private val comments: List<Link>) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private lateinit var cListener : onCommentClickListener
    private lateinit var uListener : onUserClickListener
    private lateinit var firestoreDb: FirebaseFirestore

    interface onCommentClickListener {
        fun onCommentClick(position : Int)
    }

    fun setOnCommentClickListener(commentListener: onCommentClickListener) {
        cListener = commentListener
    }

    interface onUserClickListener {
        fun onUserClick(position : Int)
    }

    fun setOnUserClickListener(userListener: onUserClickListener) {
        uListener = userListener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_comment, viewGroup, false)
        return ViewHolder(view, cListener, uListener)
    }

    override fun getItemCount() = comments.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    inner class ViewHolder(itemView: View, commentListener: onCommentClickListener, userListener: onUserClickListener) : RecyclerView.ViewHolder(itemView) {
        fun bind(comment: Link) {
            itemView.findViewById<TextView>(R.id.tvComment).text = comment.name

            firestoreDb = FirebaseFirestore.getInstance()
            firestoreDb.collection("artifacts")
                .document(comment.owner)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    var user = userSnapshot.toObject((User::class.java))
                    itemView.findViewById<TextView>(R.id.tvUsername).text = user!!.username
                    if (user!!.file_url != "") {
                        Glide.with(context).load(user!!.file_url).into(itemView.findViewById<ImageView>(R.id.ivProfileImage))
                    } else {
                        Glide.with(context).load(getProfileImageUrl(user!!.username)).into(itemView.findViewById<ImageView>(R.id.ivProfileImage))
                    }
                }

        }
        init {
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