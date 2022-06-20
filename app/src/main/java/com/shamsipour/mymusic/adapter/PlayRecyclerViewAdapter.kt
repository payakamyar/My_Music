package com.shamsipour.mymusic.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.model.data.SongItem
import com.shamsipour.mymusic.service.PlaybackService
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import android.content.ContentUris
import android.net.Uri
import com.shamsipour.mymusic.enum.DataType
import com.shamsipour.mymusic.interfaces.OnPlaylistItemLongClick


class PlayRecyclerViewAdapter(private val context:Context, private val data: ArrayList<SongItem>, private val dataType: DataType,
                              private val listener:OnPlaylistItemLongClick
                                    ):RecyclerView.Adapter<PlayRecyclerViewAdapter.ViewHolder>(){


    inner class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        var title:TextView = itemView.findViewById(R.id.tv1)
        var artist:TextView = itemView.findViewById(R.id.tv2)
        var image:ImageView = itemView.findViewById(R.id.image)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val itemView:View = LayoutInflater.from(context).inflate(R.layout.item_recycler_view,parent,false)
        val viewHolder = ViewHolder(itemView)
        itemView.setOnClickListener{
            var intent = Intent(context, PlaybackService::class.java).apply {
                putExtra("selected",this@PlayRecyclerViewAdapter.data.get(viewHolder.adapterPosition))
                putParcelableArrayListExtra("playlist",this@PlayRecyclerViewAdapter.data)
            }
            context.startService(intent)
            intent = Intent().apply {
                putExtra("title",this@PlayRecyclerViewAdapter.data.get(viewHolder.adapterPosition).title)
                putExtra("artist",this@PlayRecyclerViewAdapter.data.get(viewHolder.adapterPosition).artist)
                putExtra("albumId",this@PlayRecyclerViewAdapter.data.get(viewHolder.adapterPosition).albumId)
                putExtra("location",this@PlayRecyclerViewAdapter.data.get(viewHolder.adapterPosition).location)
                }
                intent.action = "com.shamsipour.mymusic.SWITCH_SONG"
                context.sendBroadcast(intent)
        }
        itemView.setOnLongClickListener {
            if(dataType == DataType.PLAYLISTS){
                listener.onLongClick(data[viewHolder.adapterPosition])
            }
            true
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = data.get(position).title
        holder.artist.text = data.get(position).artist
        val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, data.get(position).albumId.toLong())
        Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(holder.image)
    }

    override fun getItemCount(): Int {
        return data.size
    }



}