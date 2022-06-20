package com.shamsipour.mymusic.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shamsipour.mymusic.enum.DataType
import com.shamsipour.mymusic.model.data.AlbumItem
import com.shamsipour.mymusic.model.data.PlaylistItem
import com.shamsipour.mymusic.model.data.SongItem
import com.shamsipour.mymusic.model.repository.ContentProviderHandler
import kotlinx.coroutines.*
import java.lang.Exception

class DataViewModel: ViewModel() {

    companion object{
        @SuppressLint("StaticFieldLeak")
        private var contentProviderHandler:ContentProviderHandler? = null

        fun getContentProviderHandlerInstance(context: Context):ContentProviderHandler{
            return (contentProviderHandler?: synchronized(this){
                val inst = ContentProviderHandler(context)
                contentProviderHandler = inst
                contentProviderHandler
            }) as ContentProviderHandler
    }
    }

    fun getAllSongs(context: Context, projections:Array<String>?, orderBy:String?):Cursor?{
        val cursor:Cursor?
        val job = viewModelScope.async {
                  getContentProviderHandlerInstance(context).fetchAllSongs(projections,orderBy)
            }
        runBlocking {
            cursor = job.await()
        }
        return cursor
        }

    fun getItems(context: Context, projections: Array<String>?, orderBy: String?, dataType: DataType, id:String):Cursor?{
        val cursor:Cursor?
        val job = viewModelScope.async {
            when(dataType){
                DataType.PLAYLISTS -> getContentProviderHandlerInstance(context).getPlaylistItems(id.toLong(),projections)
                DataType.ALBUMS -> getContentProviderHandlerInstance(context).getAlbumItems(id,projections)
                else -> throw Exception("Unknown Type")
            }

        }
        runBlocking {
            cursor = job.await()
        }
        return cursor
    }

    fun getPlaylists(context: Context, projections:Array<String>?, orderBy:String?):ArrayList<PlaylistItem>?{
        var playlistItems:ArrayList<PlaylistItem>? = null
        val job = viewModelScope.launch {
            val cursor:Cursor? = getContentProviderHandlerInstance(context).fetchAllPlaylists(projections, orderBy)
            cursor?.let {
                if(cursor.moveToFirst()){
                    playlistItems = ArrayList()
                    do {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID))
                        val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME))
                        val map = getContentProviderHandlerInstance(context).getPlaylistInfo(id,null)
                        val count:Int = map.get("count") as Int
                        val lastAlbumId:String = map.get("albumId") as String
                        playlistItems!!.add(PlaylistItem(id,name,count,lastAlbumId))
                    }while (cursor.moveToNext())
                }
            }
        }
        runBlocking {
            job.join()
        }
        return playlistItems
    }

    fun getAlbums(context: Context, projections:Array<String>?, orderBy:String?):ArrayList<AlbumItem>?{
        var albumItems:ArrayList<AlbumItem>? = null
        val job = viewModelScope.launch {
            val cursor:Cursor? = getContentProviderHandlerInstance(context).fetchAllAlbums(projections, orderBy)
            cursor?.let {
                if(cursor.moveToFirst()){
                    albumItems = ArrayList()
                    do {
                        val id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
                        val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM))
                        albumItems!!.add(AlbumItem(id,name))
                    }while (cursor.moveToNext())
                }
            }
        }
        runBlocking {
            job.join()
        }
        return albumItems
    }

    fun createPlaylist(context: Context, name:String){
        viewModelScope.launch {
            getContentProviderHandlerInstance(context).addPlaylist(name)
        }
    }

    fun deletePlaylist(context: Context, id:Long){
        viewModelScope.launch {
            getContentProviderHandlerInstance(context).removePlaylist(id)
        }
    }

    fun addToPlaylist(context: Context, id:Long, items:ArrayList<SongItem>){
        viewModelScope.launch {
            getContentProviderHandlerInstance(context).addSongsToPlaylist(id,items)
        }
    }

    fun removeFromPlaylist(context: Context, playlistId:Long, songId:String){
        viewModelScope.launch {
            getContentProviderHandlerInstance(context).removeSongsFromPlaylist(playlistId,songId)
        }
    }

    }


