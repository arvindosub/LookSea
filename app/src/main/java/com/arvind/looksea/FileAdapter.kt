package com.arvind.looksea

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.arvind.looksea.models.Post
import com.bumptech.glide.Glide

internal class FileAdapter(val context: Context, private val files: List<Post>) : BaseAdapter() {

    private var layoutInflater: LayoutInflater? = null
    private lateinit var fileView: ImageView
    override fun getCount(): Int {
        return files.size
    }
    override fun getItem(position: Int): Any? {
        return null
    }
    override fun getItemId(position: Int): Long {
        return 0
    }
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View? {
        var convertView = convertView
        if (layoutInflater == null) {
            layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }
        if (convertView == null) {
            convertView = layoutInflater!!.inflate(R.layout.item_file, null)
        }
        fileView = convertView!!.findViewById(R.id.imageView)
        if (files[position].type == "audio") {
            Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/looksea-43f7d.appspot.com/o/icons%2Faudio_icon.jpg?alt=media&token=378a0754-ac98-4d2a-8e3f-e3480685f1c7").into(fileView)
        } else if (files[position].type == "text") {
            Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/looksea-43f7d.appspot.com/o/icons%2Ftext_icon.png?alt=media&token=6255e353-2374-4cc8-ad59-45f44c350816").into(fileView)
        } else {
            Glide.with(context).load(files[position].fileUrl).into(fileView)
        }
        return convertView
    }
}