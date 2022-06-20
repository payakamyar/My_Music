package com.shamsipour.mymusic.adapter

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.enum.DataType
import com.shamsipour.mymusic.model.data.AlbumItem
import com.shamsipour.mymusic.view.fragment.AllSongsFragment
import com.squareup.picasso.Picasso

class AlbumRVAdapter(private val context: Context, private val data: ArrayList<AlbumItem>
): RecyclerView.Adapter<AlbumRVAdapter.ViewHolder>(){


    val fragmentManager: FragmentManager = (context as FragmentActivity).supportFragmentManager


    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var name: TextView = itemView.findViewById(R.id.album_name)
        var image: ImageView = itemView.findViewById(R.id.image)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val itemView: View = LayoutInflater.from(context).inflate(R.layout.album_recycler_view,parent,false)
        val viewHolder = ViewHolder(itemView)
        itemView.setOnClickListener{
            val fragment: Fragment = AllSongsFragment.newInstance(
                DataType.ALBUMS,
                this.data.get(viewHolder.adapterPosition).albumId)
            fragmentManager.beginTransaction().replace(R.id.frameLayout,fragment)
                .addToBackStack("items").commit()
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, data.get(position).albumId.toLong())
        Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(holder.image)
        holder.name.text = data.get(position).album
    }


    override fun getItemCount(): Int {
        return data.size
    }


}