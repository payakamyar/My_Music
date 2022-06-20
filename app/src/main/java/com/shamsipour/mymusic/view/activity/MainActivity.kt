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

    lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false
    lateinit var imageFinder: ImageFinder
    lateinit var loadSongsViewModel:DataViewModel
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.bottomPlayback2.linearlayout.setOnClickListener(this)
        binding.bottomPlayback2.smallReverse.setOnClickListener(this)
        binding.bottomPlayback2.smallPlayback.setOnClickListener(this)
        binding.bottomPlayback2.smallForward.setOnClickListener(this)
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
        val array:ArrayList<Fragment> = ArrayList()
        array.apply {
            add(AlbumFragment())
            add(AllSongsFragment.newInstance(DataType.SONGS,null))
            add(PlaylistsFragment())
        }

        val adapter = PagerAdapter(this,array)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout,binding.viewPager){ tab,position ->
            when(position){
                0 ->
                    tab.text = "Albums"
                1 ->
                    tab.text = "All"
                2->
                    tab.text = "Playlists"
            }}.attach()

        for (t in 0 until binding.tabLayout.tabCount){
            val tab: TabLayout.Tab = binding.tabLayout.getTabAt(t)!!
            val tabView: View? = if(binding.tabLayout.getTabAt(t) !=
                                    binding.tabLayout.getTabAt(binding.tabLayout.selectedTabPosition))
                LayoutInflater.from(this).inflate(R.layout.tab_unselected,null)
            else
                LayoutInflater.from(this).inflate(R.layout.tab_selected,null)

            tabView!!.findViewById<TextView>(R.id.tab_title).text = tab.text
            tab.customView = tabView
        }
        binding.tabLayout.addOnTabSelectedListener(this)
    }

    fun setButtons(enable:Boolean){
        if(!enable){

            binding.bottomPlayback2.smallPlayback.isEnabled = false

            binding.bottomPlayback2.smallPlayback.background =
                AppCompatResources.getDrawable(this,R.drawable.ic_play)

            binding.bottomPlayback2.smallPlayback.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.gray)

            binding.bottomPlayback2.smallForward.isEnabled = false

            binding.bottomPlayback2.smallForward.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.gray)

            binding.bottomPlayback2.smallReverse.isEnabled = false

            binding.bottomPlayback2.smallReverse.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.gray)

            val bitmap:Bitmap

            val job = lifecycleScope.async{
                imageFinder.fetchCompressedArtwork(this@MainActivity,"def",50,
                    50, ImageFinder.DEFAULT_OPTIONS)
            }

            runBlocking { bitmap =  job.await() }
            binding.bottomPlayback2.smallArtwork.setImageBitmap(bitmap)
            binding.bottomPlayback2.smallTitle.text = ""
            binding.bottomPlayback2.smallArtist.text = ""
        } else{
            binding.bottomPlayback2.smallPlayback.isEnabled = true

            binding.bottomPlayback2.smallPlayback.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.white)

            binding.bottomPlayback2.smallForward.isEnabled = true

            binding.bottomPlayback2.smallForward.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.white)

            binding.bottomPlayback2.smallReverse.isEnabled = true

            binding.bottomPlayback2.smallReverse.backgroundTintList =
                AppCompatResources.getColorStateList(this, R.color.white)
        }
    }

    override fun onChange(title: String, artist: String, albumId: String) {
        if(!isServiceRunning){
            isServiceRunning = true
            setButtons(true)
        }

        binding.bottomPlayback2.smallTitle.text = title
        binding.bottomPlayback2.smallArtist.text = artist
        _albumId = albumId
        val image = lifecycleScope.async { imageFinder.fetchCompressedArtwork(this@MainActivity,
            albumId,50,50,ImageFinder.DEFAULT_OPTIONS)}
        runBlocking {
            binding.bottomPlayback2.smallArtwork.setImageBitmap(image.await())
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
                binding.bottomPlayback2.smallPlayback.background =
                    AppCompatResources.getDrawable(this,R.drawable.ic_pause)
            else
                binding.bottomPlayback2.smallPlayback.background =
                    AppCompatResources.getDrawable(this,R.drawable.ic_play)

            binding.bottomPlayback2.smallArtist.text = bundle.getString("artist")
            binding.bottomPlayback2.smallTitle.text = bundle.getString("title")
            val albumId = bundle.getString("albumId")
            val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
            val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, albumId!!.toLong())
            Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(binding.bottomPlayback2.smallArtwork)
        }
    }



    private inner class PlayBroadcast(private val listener:SongSwitchListener): BroadcastReceiver(){

        override fun onReceive(p0: Context?, p1: Intent?){
            when(p1?.action){
                "com.shamsipour.mymusic.PLAY" -> {

                    binding.bottomPlayback2.smallPlayback.background =
                        AppCompatResources.getDrawable(applicationContext,R.drawable.ic_pause)

                }
                "com.shamsipour.mymusic.PAUSE" -> {

                    binding.bottomPlayback2.smallPlayback.background =
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
            binding.bottomPlayback2.linearlayout.id -> {
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