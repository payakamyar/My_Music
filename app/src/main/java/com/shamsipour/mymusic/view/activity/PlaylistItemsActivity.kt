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

    private lateinit var addToPlaylistBtn: Button
    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    lateinit var loadSongsViewModel:DataViewModel
    private lateinit var dataArrayList:ArrayList<SongItem>
    private var isServiceRunning = false
    lateinit var imageFinder: ImageFinder
    lateinit var miniPlayer: LinearLayout
    lateinit var smallTitle:TextView
    lateinit var smallArtist:TextView
    lateinit var smallArtwork: ImageView
    lateinit var smallReverse:Button
    lateinit var smallPlayback:Button
    lateinit var smallForward:Button
    private lateinit var receiver: PlaylistItemsActivity.PlayBroadcast
    lateinit var _albumId: String
    private lateinit var contentObserver: ContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_items)
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
        textView = findViewById(R.id.message_text_view)
        progressBar = findViewById(R.id.progressbar)
        val backBtn: View = findViewById(R.id.back_btn)
        backBtn.setOnClickListener {
            finish()
        }
        recyclerView = findViewById(R.id.recyclerView)
        addToPlaylistBtn = findViewById(R.id.add_to_playlist_btn)
        addToPlaylistBtn.setOnClickListener {
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

        val include = findViewById<View>(R.id.bottom_playback)
        miniPlayer = include.findViewById(R.id.linearlayout)
        miniPlayer.setOnClickListener(this)
        smallTitle = include.findViewById(R.id.small_title)
        smallArtist = include.findViewById(R.id.small_artist)
        smallArtwork = include.findViewById(R.id.small_artwork)
        smallReverse = include.findViewById(R.id.small_reverse)
        smallReverse.setOnClickListener(this)
        smallPlayback = include.findViewById(R.id.small_playback)
        smallPlayback.setOnClickListener(this)
        smallForward = include.findViewById(R.id.small_forward)
        smallForward.setOnClickListener(this)
        imageFinder = ImageFinder()
        setButtons(isServiceRunning)
    }

    fun setButtons(enable:Boolean){
        if(!enable){
            smallPlayback.isEnabled = false
            smallPlayback.background = AppCompatResources.getDrawable(this,R.drawable.ic_play)
            smallPlayback.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            smallForward.isEnabled = false
            smallForward.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            smallReverse.isEnabled = false
            smallReverse.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            val bitmap: Bitmap

            val job = lifecycleScope.async{
                imageFinder.fetchCompressedArtwork(this@PlaylistItemsActivity,"def",50,
                    50, ImageFinder.DEFAULT_OPTIONS)
            }
            runBlocking { bitmap =  job.await() }
            smallArtwork.setImageBitmap(bitmap)
            smallTitle.text = ""
            smallArtist.text = ""
        } else{
            smallPlayback.isEnabled = true
            smallPlayback.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            smallForward.isEnabled = true
            smallForward.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            smallReverse.isEnabled = true
            smallReverse.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
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
                smallPlayback.background = AppCompatResources.getDrawable(this,R.drawable.ic_pause)
            else
                smallPlayback.background = AppCompatResources.getDrawable(this,R.drawable.ic_play)

            smallArtist.text = bundle.getString("artist")
            smallTitle.text = bundle.getString("title")
            val albumId = bundle.getString("albumId")
            val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
            val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, albumId!!.toLong())
            Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(smallArtwork)
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
                    progressBar.visibility = View.VISIBLE
                    textView.visibility = View.GONE
                    val allSongsRecyclerViewAdapter = PlayRecyclerViewAdapter(baseContext,dataArrayList,DataType.PLAYLISTS,
                        this@PlaylistItemsActivity)
                    recyclerView.apply {
                        adapter = allSongsRecyclerViewAdapter
                        layoutManager = LinearLayoutManager(this@PlaylistItemsActivity,
                            LinearLayoutManager.VERTICAL,false)
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }else{
            textView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            val allSongsRecyclerViewAdapter = PlayRecyclerViewAdapter(this,
                ArrayList(),DataType.PLAYLISTS,this)
            recyclerView.apply {
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
            R.id.linearlayout -> {
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
        smallTitle.text = title
        smallArtist.text = artist
        _albumId = albumId
        val image = lifecycleScope.async { imageFinder.fetchCompressedArtwork(this@PlaylistItemsActivity,
            albumId,50,50,ImageFinder.DEFAULT_OPTIONS)}
        runBlocking {
            smallArtwork.setImageBitmap(image.await())
        }
    }

    private inner class PlayBroadcast(private val listener: SongSwitchListener): BroadcastReceiver(){

        override fun onReceive(p0: Context?, p1: Intent?){
            when(p1?.action){
                "com.shamsipour.mymusic.PLAY" -> {
                    smallPlayback.background = AppCompatResources.getDrawable(applicationContext,R.drawable.ic_pause)
                }
                "com.shamsipour.mymusic.PAUSE" -> {
                    smallPlayback.background = AppCompatResources.getDrawable(applicationContext,R.drawable.ic_play)
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