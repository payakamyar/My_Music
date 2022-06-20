package com.shamsipour.mymusic.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.shamsipour.mymusic.R
import java.io.BufferedInputStream

class ImageFinder {

    companion object{
        val DEFAULT_OPTIONS = BitmapFactory.Options().apply {
            inJustDecodeBounds = true; inSampleSize=5; inJustDecodeBounds = false
        }
    }

    private val artworkUri = Uri.parse("content://media/external/audio/albumart")

    /*
    * converts bitmap into compressed image
    */

    suspend fun fetchCompressedArtwork(context: Context, albumId:String, w:Int, h:Int,
                                       options:BitmapFactory.Options): Bitmap {

        var bitmap: Bitmap
        try {
            val uri: Uri = Uri.withAppendedPath(artworkUri,albumId)
            val inputStream = BufferedInputStream(context.contentResolver?.openInputStream(uri))
            bitmap = BitmapFactory.decodeStream(inputStream,null,options)!!
            bitmap = Bitmap.createScaledBitmap(bitmap!!,w,h,false)
        }catch (e: Exception){
            e.printStackTrace()
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.def,options)!!
            bitmap = Bitmap.createScaledBitmap(bitmap,w,h,false)
        }
        return bitmap
    }

    /*
    * gets the original artwork
    */

    suspend fun fetchArtwork(context: Context, albumId:String):Bitmap{
        var bitmap: Bitmap
        try {
            val uri: Uri = Uri.withAppendedPath(artworkUri,albumId)
            val inputStream = BufferedInputStream(context.contentResolver?.openInputStream(uri))
            bitmap = BitmapFactory.decodeStream(inputStream,null,null)!!
        }catch (e: Exception){
            e.printStackTrace()
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.def,null)!!
        }
        return bitmap
    }

}