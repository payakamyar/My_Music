package com.shamsipour.mymusic.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.adapter.PagerAdapter
import com.shamsipour.mymusic.databinding.ActivityMainBinding
import com.shamsipour.mymusic.enum.DataType
import com.shamsipour.mymusic.interfaces.SongSwitchListener
import com.shamsipour.mymusic.util.ImageFinder
import com.shamsipour.mymusic.view.fragment.AlbumFragment
import com.shamsipour.mymusic.view.fragment.AllSongsFragment
import com.shamsipour.mymusic.view.fragment.PlaylistsFragment
import com.shamsipour.mymusic.viewmodel.DataViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity(),SongSwitchListener, View.OnClickListener, TabLayout.OnTabSelectedListener {

    lateinit var activityBinding: ActivityMainBinding
    private var isServiceRunning = false
    lateinit var imageFinder: ImageFinder
    lateinit var viewPager:ViewPager2
    lateinit var tabLayout: TabLayout
    lateinit var loadSongsViewModel:DataViewModel
    lateinit var miniPlayer:LinearLayout
    lateinit var smallTitle:TextView
    lateinit var smallArtist:TextView
    lateinit var smallArtwork:ImageView
    lateinit var smallReverse:Button
    lateinit var smallPlayback:Button
    lateinit var smallForward:Button
    private lateinit var receiver:PlayBroadcast
    lateinit var _albumId: String

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.isNotEmpty()){
            if(requestCode == 11){
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    initialize()
                else if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                    Snackbar.make(findViewById(R.id.constraint_layout),
                        "The app requires you to grant permissions.",Snackbar.LENGTH_LONG).show()
                    requestPermissions(permissions,11)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityBinding.root)

        if(Build.VERSION.SDK_INT > 22){
            val permissions:Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if(checkPermissionsInit(permissions))
                initialize()
        }else
            initialize()

    }

    override fun onStart() {
        super.onStart()
        sendBroadcast(Intent("com.mymusic.service.GET_INFO"))
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }


    private fun initialize(){
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
        val include: View = findViewById(R.id.bottom_playback2)
        miniPlayer = findViewById(R.id.linearlayout)
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
        viewPagerAndTabLayoutInit()



    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissionsInit(permissions:Array<String>):Boolean{
        for(i in permissions.indices){
            if(checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, 11)
                return false
            }else
                Log.i("Granted", permissions[i])
        }
        return true
    }

    private fun viewPagerAndTabLayoutInit(){
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
        val array:ArrayList<Fragment> = ArrayList()
        array.apply {
            add(AlbumFragment())
            add(AllSongsFragment.newInstance(DataType.SONGS,null))
            add(PlaylistsFragment())
        }

        val adapter = PagerAdapter(this,array)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout,viewPager){ tab,position ->
            when(position){
                0 ->
                    tab.text = "Albums"
                1 ->
                    tab.text = "All"
                2->
                    tab.text = "Playlists"
            }}.attach()

        for (t in 0 until tabLayout.tabCount){
            val tab: TabLayout.Tab = tabLayout.getTabAt(t)!!
            val tabView: View? = if(tabLayout.getTabAt(t) != tabLayout.getTabAt(tabLayout.selectedTabPosition))
                LayoutInflater.from(this).inflate(R.layout.tab_unselected,null)
            else
                LayoutInflater.from(this).inflate(R.layout.tab_selected,null)

            tabView!!.findViewById<TextView>(R.id.tab_title).text = tab.text
            tab.customView = tabView
        }
        tabLayout.addOnTabSelectedListener(this)
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
            val bitmap:Bitmap

            val job = lifecycleScope.async{
                imageFinder.fetchCompressedArtwork(this@MainActivity,"def",50,
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

    override fun onChange(title: String, artist: String, albumId: String) {
        if(!isServiceRunning){
            isServiceRunning = true
            setButtons(true)
        }
        smallTitle.text = title
        smallArtist.text = artist
        _albumId = albumId
        val image = lifecycleScope.async { imageFinder.fetchCompressedArtwork(this@MainActivity,
            albumId,50,50,ImageFinder.DEFAULT_OPTIONS)}
        runBlocking {
            smallArtwork.setImageBitmap(image.await())
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



    private inner class PlayBroadcast(private val listener:SongSwitchListener): BroadcastReceiver(){

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
                else -> {Log.e("Invalid","Wrong command")}
            }
        }

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
    override fun onTabSelected(tab: TabLayout.Tab?) {
        tab!!.customView = null
        val tabView: View? = LayoutInflater.from(this).inflate(R.layout.tab_selected,null)
        tabView!!.findViewById<TextView>(R.id.tab_title).text = tab.text
        tab.customView = tabView
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        tab!!.customView = null
        val tabView: View? = LayoutInflater.from(this).inflate(R.layout.tab_unselected,null)
        tabView!!.findViewById<TextView>(R.id.tab_title).text = tab.text
        tab.customView = tabView
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {

    }

}