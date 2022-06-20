package com.shamsipour.mymusic.view.activity

import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.adapter.PlayRecyclerViewAdapter
import com.shamsipour.mymusic.databinding.ActivityPlaylistItemsBinding
import com.shamsipour.mymusic.enum.DataType
import com.shamsipour.mymusic.interfaces.OnPlaylistItemLongClick
import com.shamsipour.mymusic.interfaces.SongSwitchListener
import com.shamsipour.mymusic.model.data.SongItem
import com.shamsipour.mymusic.util.ImageFinder
import com.shamsipour.mymusic.view.fragment.DeleteBottomFragment
import com.shamsipour.mymusic.viewmodel.DataViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*

class PlaylistItemsActivity : AppCompatActivity(), OnPlaylistItemLongClick,SongSwitchListener, View.OnClickListener {

    lateinit var binding: ActivityPlaylistItemsBinding
    lateinit var loadSongsViewModel:DataViewModel
    private lateinit var dataArrayList:ArrayList<SongItem>
    private var isServiceRunning = false
    lateinit var imageFinder: ImageFinder
    private lateinit var receiver: PlaylistItemsActivity.PlayBroadcast
    lateinit var _albumId: String
    private lateinit var contentObserver: ContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contentObserver = object : ContentObserver(null){
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                setUpRecyclerView()
            }
        }
        contentResolver.registerContentObserver(MediaStore.Audio.Playlists.Members.getContentUri("external",
            intent.getStringExtra("id")!!.toLong()),true,contentObserver)
        initViews()
        setUpRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        sendBroadcast(Intent("com.mymusic.service.GET_INFO"))
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(contentObserver)
        unregisterReceiver(receiver)
    }

    private fun initViews(){
        loadSongsViewModel = ViewModelProvider(this).get(DataViewModel::class.java)
        val playlistNameTextView:TextView = findViewById(R.id.playlist_name)
        playlistNameTextView.text = intent.getStringExtra("name")
        val backBtn: View = findViewById(R.id.back_btn)
        backBtn.setOnClickListener {
            finish()
        }
        binding.addToPlaylistBtn.setOnClickListener {
            val intent = Intent(baseContext,SelectSongsActivity::class.java)
            intent.putExtra("id",getIntent().getStringExtra("id")!!.toLong())
            startActivity(intent)
        }
        _albumId = ""
        loadSongsViewModel = ViewModelProvider(this).get(DataViewModel::class.java)
        val filter: IntentFilter = IntentFilter().apply {
            addAction("com.shamsipour.mymusic.PLAY")
            addAction("com.shamsipour.mymusic.PAUSE")
            addAction("com.shamsipour.mymusic.STOP")
            addAction("com.shamsipour.mymusic.SWITCH_SONG")
            addAction("com.shamsipour.mymusic.GET_INFO")
        }
        receiver = PlayBroadcast(this)
        registerReceiver(receiver,filter)

        binding.bottomPlayback.linearlayout.setOnClickListener(this)
        binding.bottomPlayback.smallReverse.setOnClickListener(this)
        binding.bottomPlayback.smallPlayback.setOnClickListener(this)
        binding.bottomPlayback.smallForward.setOnClickListener(this)
        imageFinder = ImageFinder()
        setButtons(isServiceRunning)
    }

    fun setButtons(enable:Boolean){
        if(!enable){
            binding.bottomPlayback.smallPlayback.isEnabled = false

            binding.bottomPlayback.smallPlayback.background =
                AppCompatResources.getDrawable(this,R.drawable.ic_play)

            binding.bottomPlayback.smallPlayback.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.gray)

            binding.bottomPlayback.smallForward.isEnabled = false

            binding.bottomPlayback.smallForward.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.gray)

            binding.bottomPlayback.smallReverse.isEnabled = false

            binding.bottomPlayback.smallReverse.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.gray)

            val bitmap: Bitmap

            val job = lifecycleScope.async{
                imageFinder.fetchCompressedArtwork(this@PlaylistItemsActivity,"def",50,
                    50, ImageFinder.DEFAULT_OPTIONS)
            }
            runBlocking { bitmap =  job.await() }
            binding.bottomPlayback.smallArtwork.setImageBitmap(bitmap)
            binding.bottomPlayback.smallTitle.text = ""
            binding.bottomPlayback.smallArtist.text = ""
        } else{
            binding.bottomPlayback.smallPlayback.isEnabled = true

            binding.bottomPlayback.smallPlayback.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.white)

            binding.bottomPlayback.smallForward.isEnabled = true

            binding.bottomPlayback.smallForward.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.white)

            binding.bottomPlayback.smallReverse.isEnabled = true

            binding.bottomPlayback.smallReverse.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.white)
        }
    }

    private fun getInfo(bundle: Bundle?){
        if(!isServiceRunning){
            isServiceRunning = true
            setButtons(true)
        }
        bundle?.let {
            val isPlaying = bundle.getBoolean("playing")
            if(isPlaying)
                binding.bottomPlayback.smallPlayback.background =
                    AppCompatResources.getDrawable(this,R.drawable.ic_pause)
            else
                binding.bottomPlayback.smallPlayback.background =
                    AppCompatResources.getDrawable(this,R.drawable.ic_play)

            binding.bottomPlayback.smallArtist.text = bundle.getString("artist")
            binding.bottomPlayback.smallTitle.text = bundle.getString("title")
            val albumId = bundle.getString("albumId")
            val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
            val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, albumId!!.toLong())
            Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(binding.bottomPlayback.smallArtwork)
        }
    }

    private fun setUpRecyclerView(){

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA)

        val cursor = loadSongsViewModel.getItems(applicationContext,projection,null, DataType.PLAYLISTS,
            intent.getStringExtra("id")!!)

        if(cursor?.moveToFirst() == true){

            GlobalScope.launch(Dispatchers.Default) {
                dataArrayList = cursorToArrayList(cursor)
                withContext(Dispatchers.Main){
                    binding.progressbar.visibility = View.VISIBLE
                    binding.messageTextView.visibility = View.GONE
                    val allSongsRecyclerViewAdapter = PlayRecyclerViewAdapter(baseContext,dataArrayList,DataType.PLAYLISTS,
                        this@PlaylistItemsActivity)
                    binding.recyclerView.apply {
                        adapter = allSongsRecyclerViewAdapter
                        layoutManager = LinearLayoutManager(this@PlaylistItemsActivity,
                            LinearLayoutManager.VERTICAL,false)
                    }
                    binding.progressbar.visibility = View.GONE
                }
            }
        }else{
            binding.messageTextView.visibility = View.VISIBLE
            binding.progressbar.visibility = View.GONE
            val allSongsRecyclerViewAdapter = PlayRecyclerViewAdapter(this,
                ArrayList(),DataType.PLAYLISTS,this)
            binding.recyclerView.apply {
                adapter = allSongsRecyclerViewAdapter
                layoutManager = LinearLayoutManager(this@PlaylistItemsActivity, LinearLayoutManager.VERTICAL,false)
            }
        }
    }

    private suspend fun cursorToArrayList(data:Cursor):ArrayList<SongItem>{

        val arrayList:ArrayList<SongItem> = ArrayList()
        if(data.moveToFirst()){
            do{
                var albumId:String = data.getString(data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
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

    override fun onLongClick(songItem: SongItem) {
        DeleteBottomFragment.newInstance("${songItem.title}\n${songItem.artist}",intent.getStringExtra("id")!!.toLong()
                                        ,songItem.id,DataType.SONGS)
                                        .show(this.supportFragmentManager,"delete")
        Log.i("id", intent.getStringExtra("id")!!)
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.small_reverse -> {
                val sendIntent = Intent()
                sendIntent.action = "com.mymusic.service.REVERSE"
                sendBroadcast(sendIntent)
            }
            R.id.small_playback ->{
                val sendIntent = Intent()
                sendIntent.action = "com.mymusic.service.PLAYBACK"
                sendBroadcast(sendIntent)
            }
            R.id.small_forward ->{
                val sendIntent = Intent()
                sendIntent.action = "com.mymusic.service.FORWARD"
                sendBroadcast(sendIntent)
            }
            binding.bottomPlayback.linearlayout.id -> {
                val intent = Intent(this,PlaybackActivity::class.java)
                startActivity(intent)
            }
        }
    }


    override fun onChange(title: String, artist: String, albumId: String) {
        if(!isServiceRunning){
            isServiceRunning = true
            setButtons(true)
        }
        binding.bottomPlayback.smallTitle.text = title
        binding.bottomPlayback.smallArtist.text = artist
        _albumId = albumId
        val image = lifecycleScope.async { imageFinder.fetchCompressedArtwork(this@PlaylistItemsActivity,
            albumId,50,50,ImageFinder.DEFAULT_OPTIONS)}
        runBlocking {
            binding.bottomPlayback.smallArtwork.setImageBitmap(image.await())
        }
    }

    private inner class PlayBroadcast(private val listener: SongSwitchListener): BroadcastReceiver(){

        override fun onReceive(p0: Context?, p1: Intent?){
            when(p1?.action){
                "com.shamsipour.mymusic.PLAY" -> {

                    binding.bottomPlayback.smallPlayback.background =
                        AppCompatResources.getDrawable(applicationContext,R.drawable.ic_pause)

                }
                "com.shamsipour.mymusic.PAUSE" -> {

                    binding.bottomPlayback.smallPlayback.background =
                        AppCompatResources.getDrawable(applicationContext,R.drawable.ic_play)

                }
                "com.shamsipour.mymusic.SWITCH_SONG" -> {
                    p1.let {
                        listener.onChange(it.getStringExtra("title")!!,p1.getStringExtra("artist")!!,
                            p1.getStringExtra("albumId")!!)
                    }
                }
                "com.shamsipour.mymusic.STOP" -> {
                    isServiceRunning = false
                    setButtons(false)
                }
                "com.shamsipour.mymusic.GET_INFO" -> getInfo(p1.getBundleExtra("data"))
                else -> {
                    Log.e("Invalid","Wrong command")}
            }
        }

    }
}