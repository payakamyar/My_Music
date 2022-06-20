package com.shamsipour.mymusic.interfaces

import com.shamsipour.mymusic.model.data.SongItem

interface OnPlaylistItemLongClick{
    fun onLongClick(songItem: SongItem)
}