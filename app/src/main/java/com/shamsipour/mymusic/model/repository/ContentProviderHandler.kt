package com.shamsipour.mymusic.model.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.shamsipour.mymusic.model.data.SongItem

class ContentProviderHandler(
    private val context: Context
) {

    //TODO: Add livedata to content provider

    suspend fun fetchAllSongs(projections: Array<String>?, orderBy: String?):Cursor?{

        val uri:Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        return context.contentResolver.query(uri,projections,null,null,orderBy)

    }

    suspend fun fetchAllPlaylists(projections: Array<String>?, orderBy: String?):Cursor?{

        val uri:Uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
        return context.contentResolver.query(uri,projections,null,null,orderBy)

    }

    suspend fun fetchAllAlbums(projections: Array<String>?, orderBy: String?):Cursor?{

        val uri:Uri = MediaStore.Audio.Albums.getContentUri("external")
        return context.contentResolver.query(uri,projections,null,null,orderBy)
    }

    suspend fun getPlaylistInfo(id:Long, projections: Array<String>?):HashMap<String,Any>{
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external",id)
        val cursor:Cursor? = context.contentResolver.query(uri,projections,null,null,
                                                    projections?.get(0))
        var item = HashMap<String, Any>()
        item.put("albumId","-1")
        item.put("count",0)
        cursor?.let {
            if(it.moveToFirst()){
                val albumId:String = it.getString(cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM_ID))
                val count:Int = it.count
                item.put("albumId",albumId)
                item.put("count",count)
            }
        }
        return item
    }

    suspend fun getPlaylistItems(id:Long, projections: Array<String>?):Cursor?{
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external",id)
        return context.contentResolver.query(uri,projections,null,null,
            projections?.get(0))
    }


    suspend fun getAlbumItems(id:String, projections: Array<String>?):Cursor?{
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArg = arrayOf(id)
        return context.contentResolver.query(uri,projections,selection,selectionArg,
            projections?.get(0))
    }

    suspend fun addPlaylist(name:String){
        val playlistItem = ContentValues()
        playlistItem.apply {
            put(MediaStore.Audio.Playlists.NAME,name)
            put(MediaStore.Audio.Playlists.DATE_ADDED,System.currentTimeMillis())
        }
        context.contentResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,playlistItem)
    }

    suspend fun removePlaylist(id:Long){
        context.contentResolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
        "${MediaStore.Audio.Playlists._ID} = ?", arrayOf(id.toString()))
    }

    suspend fun addSongsToPlaylist(id:Long, items: ArrayList<SongItem>){
        for(i in 0 until items.size) {
            val members = ContentValues()
            members.apply {
                put(MediaStore.Audio.Playlists.Members.AUDIO_ID, items[i].id)
                put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, 0)
            }
            context.contentResolver.insert(
                MediaStore.Audio.Playlists.Members.getContentUri(
                    "external",
                    id
                ), members
            )
        }
    }

    suspend fun removeSongsFromPlaylist(playlistId:Long, songId:String){
        context.contentResolver.delete(MediaStore.Audio.Playlists.Members.getContentUri("external",playlistId),
            "${MediaStore.Audio.Playlists.Members._ID} = ?", arrayOf(songId))
    }

}