package com.shamsipour.mymusic.view.activity

import android.content.Intent
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.adapter.SelectableRecyclerViewAdapter
import com.shamsipour.mymusic.interfaces.OnItemSelected
import com.shamsipour.mymusic.model.data.SongItem
import com.shamsipour.mymusic.viewmodel.DataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectSongsActivity : AppCompatActivity(),OnItemSelected {

    private  var playlistId:Long = -1
    private lateinit var backBtn:ImageView
    private lateinit var doneBtn:Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemList:ArrayList<SongItem>
    private lateinit var viewModel:DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_songs)
        playlistId = intent.getLongExtra("id",-1)
        initViews()
        setUpRecyclerView()
    }


    private fun initViews(){
        itemList = ArrayList()
        viewModel = ViewModelProvider(this).get(DataViewModel::class.java)
        backBtn = findViewById(R.id.back_btn)
        backBtn.setOnClickListener{
            finish()
        }
        doneBtn = findViewById(R.id.done_btn)
        doneBtn.setOnClickListener {
            if(playlistId > -1){
                viewModel.addToPlaylist(this,playlistId,itemList)
                finish()
            }
        }
        recyclerView = findViewById(R.id.recyclerView)
    }

    private fun setUpRecyclerView(){

        val textView: TextView = findViewById(R.id.message_text_view)
        val progressBar: ProgressBar = findViewById(R.id.progressbar)
        val projection = arrayOf(MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA)

        val cursor:Cursor? = viewModel.getAllSongs(this,projection,projection[0])
        if(cursor?.moveToFirst() == true){
            progressBar.visibility = View.VISIBLE
            textView.visibility = View.GONE

            GlobalScope.launch(Dispatchers.Default) {
                val dataArrayList = cursorToArrayList(cursor)
                withContext(Dispatchers.Main){
                    val selectableRecyclerViewAdapter = SelectableRecyclerViewAdapter(this@SelectSongsActivity,
                        dataArrayList,this@SelectSongsActivity)
                    recyclerView.apply {
                        adapter = selectableRecyclerViewAdapter
                        layoutManager = LinearLayoutManager(this@SelectSongsActivity,
                            LinearLayoutManager.VERTICAL,false)
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }else{
            textView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }

    }

    private suspend fun cursorToArrayList(data: Cursor):ArrayList<SongItem>{

        val arrayList:ArrayList<SongItem> = ArrayList()
        if(data.moveToFirst()){
            do{
                val albumId:String = data.getString(data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val song = SongItem(data.getString(data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
                    data.getString(data.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                    data.getString(data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                    data.getString(data.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                    albumId)
                arrayList.add(song)
            }while (data.moveToNext())
            data.close()
        }
        return arrayList
    }

    override fun onSelect(songItem: SongItem) {
        if(!doneBtn.isEnabled){
            doneBtn.isEnabled = true
            doneBtn.setTextColor(resources.getColor(R.color.white))
        }
        itemList.add(songItem)
    }

    override fun onRemove(songItem: SongItem) {
        itemList.remove(songItem)
        if(itemList.isEmpty()){
            doneBtn.isEnabled = false
            doneBtn.setTextColor(resources.getColor(R.color.gray))
        }
    }
}