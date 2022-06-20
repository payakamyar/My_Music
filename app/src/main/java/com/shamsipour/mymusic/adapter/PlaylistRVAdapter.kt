package com.shamsipour.mymusic.adapter

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.enum.DataType
import com.shamsipour.mymusic.interfaces.SimpleCallback
import com.shamsipour.mymusic.model.data.PlaylistItem
import com.shamsipour.mymusic.view.activity.PlaylistItemsActivity
import com.shamsipour.mymusic.view.fragment.AllSongsFragment
import com.shamsipour.mymusic.view.fragment.DeleteBottomFragment
import com.squareup.picasso.Picasso


class PlaylistRVAdapter(private val context: Context, private val data: ArrayList<PlaylistItem>, private val callback:SimpleCallback
): RecyclerView.Adapter<PlaylistRVAdapter.ViewHolder>(){


    val fragmentManager:FragmentManager = (context as FragmentActivity).supportFragmentManager


    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var name: TextView = itemView.findViewById(R.id.tv1)
        var count: TextView = itemView.findViewById(R.id.tv2)
        var image: ImageView = itemView.findViewById(R.id.image)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val itemView: View = LayoutInflater.from(context).inflate(R.layout.item_recycler_view,parent,false)
        val viewHolder = ViewHolder(itemView)
        itemView.setOnClickListener{
            val bundle: Bundle = Bundle().apply {
                putString("id",this@PlaylistRVAdapter.data[viewHolder.adapterPosition].id.toString())
                putString("name",this@PlaylistRVAdapter.data[viewHolder.adapterPosition].name)
            }
            callback.callback(bundle)
        }
        itemView.setOnLongClickListener {
            DeleteBottomFragment.newInstance(data[viewHolder.adapterPosition].name,data[viewHolder.adapterPosition].id,null,
                DataType.PLAYLISTS).show(fragmentManager,"delete")
            true
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = data.get(position).name
        holder.count.text = (data.get(position).count).toString()
        val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, data.get(position).lastAlbumId.toLong())
        Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(holder.image)
    }

    override fun getItemCount(): Int {
        return data.size
    }



}


