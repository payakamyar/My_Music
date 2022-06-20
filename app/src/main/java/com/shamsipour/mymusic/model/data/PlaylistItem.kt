package com.shamsipour.mymusic.model.data

import android.os.Parcel
import android.os.Parcelable

data class PlaylistItem(val id:Long, val name:String, val count:Int, val lastAlbumId:String):
    Parcelable {


    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!
    ) {
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0!!.writeLong(id)
        p0.writeString(name)
        p0.writeInt(count)
        p0.writeString(lastAlbumId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SongItem> {
        override fun createFromParcel(parcel: Parcel): SongItem {
            return SongItem(parcel)
        }

        override fun newArray(size: Int): Array<SongItem?> {
            return arrayOfNulls(size)
        }
    }

}
