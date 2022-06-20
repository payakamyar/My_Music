package com.shamsipour.mymusic.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.model.data.SongItem
import com.squareup.picasso.Picasso
import android.content.ContentUris
import android.net.Uri
import android.widget.CheckBox
import com.shamsipour.mymusic.interfaces.OnItemSelected


class SelectableRecyclerViewAdapter(private val context:Context, private val data: ArrayList<SongItem>, private val listener:OnItemSelected
                                    ):RecyclerView.Adapter<SelectableRecyclerViewAdapter.ViewHolder>(){


    inner class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        var title:TextView = itemView.findViewById(R.id.tv1)
        var artist:TextView = itemView.findViewById(R.id.tv2)
        var image:ImageView = itemView.findViewById(R.id.image)
        var checkBox:CheckBox = itemView.findViewById(R.id.checkbox)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val itemView:View = LayoutInflater.from(context).inflate(R.layout.selectable_rv_items,parent,false)
        val viewHolder = ViewHolder(itemView)
        itemView.setOnClickListener{
            viewHolder.checkBox.toggle()
            if(viewHolder.checkBox.isChecked)
                listener.onSelect(data[viewHolder.adapterPosition])
            else
                listener.onRemove(data[viewHolder.adapterPosition])
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = data[position].title
        holder.artist.text = data[position].artist
        val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, data[position].albumId.toLong())
        Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(holder.image)
    }

    override fun getItemCount(): Int {
        return data.size
    }



}