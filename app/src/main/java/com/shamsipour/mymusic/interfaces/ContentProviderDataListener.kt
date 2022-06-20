package com.shamsipour.mymusic.interfaces

import android.database.Cursor

interface ContentProviderDataListener {
    fun onFinish(requestCode:Int,data:Cursor)
}