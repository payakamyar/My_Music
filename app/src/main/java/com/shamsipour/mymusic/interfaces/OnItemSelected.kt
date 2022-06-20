package com.shamsipour.mymusic.interfaces

import com.shamsipour.mymusic.model.data.SongItem

interface OnItemSelected {
    fun onSelect(songItem: SongItem)
    fun onRemove(songItem: SongItem)
}