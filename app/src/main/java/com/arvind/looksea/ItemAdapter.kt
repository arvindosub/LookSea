package com.arvind.looksea

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.arvind.looksea.models.Item
import com.bumptech.glide.Glide
import java.math.BigInteger
import java.security.MessageDigest

class ItemAdapter (val context: Context, private val items: List<Item>) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    private lateinit var iListener : onItemClickListener

    interface onItemClickListener {
        fun onItemClick(position : Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        iListener = listener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_item, viewGroup, false)
        return ViewHolder(view, iListener)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(itemView: View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: Item) {
            itemView.findViewById<TextView>(R.id.tvUsername).text = item.username as String
            itemView.findViewById<TextView>(R.id.tvDescription).text = item.description
            itemView.findViewById<TextView>(R.id.tvType).text = item.type
            if (item.fileUrl != "") {
                Glide.with(context).load(item.fileUrl).into(itemView.findViewById<ImageView>(R.id.ivImage))
            } else {
                Glide.with(context).load(getProfileImageUrl(item.username as String)).into(itemView.findViewById<ImageView>(R.id.ivImage))
            }
        }
        init {
            itemView.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
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