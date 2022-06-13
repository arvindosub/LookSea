package com.arvind.looksea

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.arvind.looksea.models.Link
import com.arvind.looksea.models.User
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigInteger
import java.security.MessageDigest

class CommentAdapter(val context: PostActivity, private val comments: List<Link>, private val signedInUserID: String) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private lateinit var cListener : onCommentClickListener
    private lateinit var uListener : onUserClickListener
    private lateinit var dListener : onDeleteClickListener
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

    interface onDeleteClickListener {
        fun onDeleteClick(position : Int)
    }

    fun setOnDeleteClickListener(deleteListener: onDeleteClickListener) {
        dListener = deleteListener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_comment, viewGroup, false)
        return ViewHolder(view, cListener, uListener, dListener)
    }

    override fun getItemCount() = comments.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    inner class ViewHolder(itemView: View, commentListener: onCommentClickListener, userListener: onUserClickListener, deleteListener: onDeleteClickListener) : RecyclerView.ViewHolder(itemView) {
        fun bind(comment: Link) {
            itemView.findViewById<TextView>(R.id.tvComment).text = comment.name
            if (comment.owner == signedInUserID) {
                itemView.findViewById<ImageButton>(R.id.btnDelete).isVisible = true
            }

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

            itemView.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
                deleteListener.onDeleteClick(bindingAdapterPosition)
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