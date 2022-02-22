package com.arvind.looksea

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.arvind.looksea.models.Post
import com.bumptech.glide.Glide

internal class FileAdapter(val context: Context, private val files: List<Post>) : BaseAdapter() {

    private var layoutInflater: LayoutInflater? = null
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
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
        imageView = convertView!!.findViewById(R.id.imageView)
        //textView = convertView.findViewById(R.id.textView)
        Glide.with(context).load(files[position].fileUrl).into(imageView)
        //textView.text = files[position].description
        return convertView
    }
}