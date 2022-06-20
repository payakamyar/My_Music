package com.shamsipour.mymusic.view.activity

import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.databinding.ActivityPlaybackBinding
import com.shamsipour.mymusic.util.ImageFinder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import org.w3c.dom.Text
import java.util.*
import kotlin.concurrent.fixedRateTimer

class PlaybackActivity : AppCompatActivity(),SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    lateinit var binding: ActivityPlaybackBinding
    private lateinit var timer: Timer
    private var isPlaying:Boolean = false
    private var isServiceRunning:Boolean = false
    private var isRepeat:Boolean = false
    private var isShuffle:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaybackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
        val filter:IntentFilter = IntentFilter().apply {
            addAction("com.shamsipour.mymusic.SWITCH_SONG")
            addAction("com.shamsipour.mymusic.PLAY")
            addAction("com.shamsipour.mymusic.PAUSE")
            addAction("com.shamsipour.mymusic.GET_INFO")
            addAction("com.shamsipour.mymusic.STOP")
        }
        val receiver = PlaybackBroadcast()
        registerReceiver(receiver,filter)
    }


    private fun initialize(){
        binding.seekbar.setOnSeekBarChangeListener(this)
        binding.repeat.setOnClickListener(this)
        binding.shuffle.setOnClickListener(this)
        binding.playPause.setOnClickListener(this)
        binding.reverse.setOnClickListener(this)
        binding.forward.setOnClickListener(this)
        sendBroadcast(Intent("com.mymusic.service.GET_INFO"))
        setButtons(isServiceRunning)
    }


    private fun counter(m:Int,s:Int):Timer{
        if(this::timer.isInitialized){
            timer.cancel()
        }
        var second = s
        var minute = m
        return fixedRateTimer("timer",false,1000L,1000) {
            ++second
            if(second > 59){
                second = 0
                ++minute
            }
            lifecycleScope.launch(Dispatchers.Main){
                if (binding.current.text.toString() == binding.length.text.toString()){
                    this.cancel()
                    binding.current.text = "0:00"
                }else{
                    binding.seekbar.progress += 1
                    if(second < 10)
                        binding.current.text = "$minute:0$second"
                    else
                        binding.current.text = "$minute:$second"
                }
            }
        }
    }

    fun getInfo(bundle: Bundle?){
        bundle?.let {
            if(!isServiceRunning)
                setButtons(true)

            isPlaying = bundle.getBoolean("playing")
            isRepeat = bundle.getBoolean("repeat")
            isShuffle = bundle.getBoolean("shuffle")
            val _current = bundle.getInt("current")
            val _length = bundle.getInt("length")
            binding.artist.text = bundle.getString("artist")
            binding.title.text = bundle.getString("title")
            val albumId = bundle.getString("albumId")
            val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
            val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, albumId!!.toLong())
            Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(binding.artwork)
            val currentMinute = _current / 1000 / 60
            val currentSecond = _current / 1000 % 60
            if(currentSecond<10)
                binding.current.text = "$currentMinute:0$currentSecond"
            else
                binding.current.text = "$currentMinute:$currentSecond"
            val minuteLength = _length / 1000 / 60
            val secondLength = _length / 1000 % 60
            if(secondLength < 10)
                binding.length.text = "$minuteLength:0$secondLength"
            else
                binding.length.text = "$minuteLength:$secondLength"
            binding.seekbar.max = (_length / 1000)
            binding.seekbar.progress = (_current / 1000)
            if(isShuffle)
                binding.shuffle.backgroundTintList = AppCompatResources.getColorStateList(this,R.color.teal_200)
            else
                binding.shuffle.backgroundTintList = AppCompatResources.getColorStateList(this,R.color.white)
            if(isRepeat)
                binding.repeat.backgroundTintList = AppCompatResources.getColorStateList(this,R.color.teal_200)
            else
                binding.repeat.backgroundTintList = AppCompatResources.getColorStateList(this,R.color.white)
            if(isPlaying){
                binding.playPause.setImageResource(R.drawable.ic_pause)
                timer = counter(currentMinute,currentSecond)
            }
        }
    }

    fun updateView(_title:String, _artist:String, _albumId:String ,_length:Int){

        if(!isServiceRunning)
            setButtons(true)

        val minuteLength = _length / 1000 / 60
        val secondLength = _length / 1000 % 60
        binding.current.text = "0:00"
        if(secondLength < 10)
            binding.length.text = "$minuteLength:0$secondLength"
        else
            binding.length.text = "$minuteLength:$secondLength"
        binding.title.text = _title
        binding.artist.text = _artist
        val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, _albumId.toLong())
        Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(binding.artwork)
        binding.seekbar.progress = 0
        binding.seekbar.max = _length / 1000
        if(isPlaying)
            timer = counter(binding.seekbar.progress / 60,binding.seekbar.progress % 60)
    }

    fun setButtons(enable:Boolean){

        isServiceRunning = enable
        isPlaying = enable

        if(!enable){
            binding.repeat.isEnabled = false
            binding.repeat.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            binding.shuffle.isEnabled = false
            binding.shuffle.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            binding.playPause.isEnabled = false
            binding.reverse.isEnabled = false
            binding.reverse.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            binding.forward.isEnabled = false
            binding.forward.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            binding.seekbar.isEnabled = false
            if(this::timer.isInitialized)
                timer.cancel()
            Picasso.get().load(R.drawable.def).into(binding.artwork)
            binding.title.text = ""
            binding.artist.text = ""
        } else{
            binding.repeat.isEnabled = true
            binding.repeat.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            binding.shuffle.isEnabled = true
            binding.shuffle.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            binding.playPause.isEnabled = true
            binding.reverse.isEnabled = true
            binding.reverse.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            binding.forward.isEnabled = true
            binding.forward.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            binding.seekbar.isEnabled = true
        }
    }

    override fun onClick(p0: View?) {
        when(p0?.id) {
            R.id.play_pause -> sendBroadcast(Intent("com.mymusic.service.PLAYBACK"))
            R.id.reverse -> sendBroadcast(Intent("com.mymusic.service.REVERSE"))
            R.id.forward -> sendBroadcast(Intent("com.mymusic.service.FORWARD"))
            R.id.repeat -> {
                if (isRepeat)
                    binding.repeat.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
                else
                    binding.repeat.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.teal_200)

                sendBroadcast(Intent("com.mymusic.service.SET_REPEAT"))
                isRepeat = !isRepeat
            }
            R.id.shuffle -> {
             if (isShuffle)
                 binding.shuffle.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
             else
                 binding.shuffle.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.teal_200)

                sendBroadcast(Intent("com.mymusic.service.SET_SHUFFLE"))
                isShuffle = !isShuffle
            }
        }
    }
    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
        timer.cancel()
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        p0?.let {
            val currentMinute = p0.progress / 60
            val currentSecond = p0.progress % 60
            binding.current.text = "$currentMinute:$currentSecond"
            if(isPlaying)
                timer = counter(currentMinute,currentSecond)
            sendBroadcast(
                Intent("com.mymusic.service.SEEK_TO").also {
                    it.putExtra("seekValue",p0.progress * 1000)
                }
            )
        }
    }

    private inner class PlaybackBroadcast:BroadcastReceiver(){

        override fun onReceive(p0: Context?, p1: Intent?) {
            when(p1?.action){
                "com.shamsipour.mymusic.SWITCH_SONG" -> {
                    val title = p1.getStringExtra("title")
                    val artist = p1.getStringExtra("artist")
                    val albumId = p1.getStringExtra("albumId")
                    val length = p1.getIntExtra("length",0)
                    updateView(title!!,artist!!,albumId!!,length)
                }
                "com.shamsipour.mymusic.PLAY" -> {
                    binding.playPause.setImageResource(R.drawable.ic_pause)
                    timer = counter(binding.seekbar.progress / 60, binding.seekbar.progress % 60)
                    isPlaying = true
                }
                "com.shamsipour.mymusic.PAUSE" -> {
                    binding.playPause.setImageResource(R.drawable.ic_play)
                    timer.cancel()
                    isPlaying = false
                }
                "com.shamsipour.mymusic.GET_INFO" -> {
                    val data = p1.getBundleExtra("data")
                    getInfo(data)
                }
                "com.shamsipour.mymusic.STOP" -> {
                    setButtons(false)
                }
            }
        }

    }

}

