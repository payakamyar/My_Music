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
import com.shamsipour.mymusic.util.ImageFinder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import org.w3c.dom.Text
import java.util.*
import kotlin.concurrent.fixedRateTimer

class PlaybackActivity : AppCompatActivity(),SeekBar.OnSeekBarChangeListener, View.OnClickListener {


    private lateinit var artwork:ImageView
    private lateinit var title:TextView
    private lateinit var artist:TextView
    private lateinit var seekBar: SeekBar
    private lateinit var current:TextView
    private lateinit var length:TextView
    private lateinit var timer: Timer
    private lateinit var repeat: ImageView
    private lateinit var shuffle: ImageView
    private lateinit var playback: ImageView
    private lateinit var reverse: ImageView
    private lateinit var forward: ImageView
    private var isPlaying:Boolean = false
    private var isServiceRunning:Boolean = false
    private var isRepeat:Boolean = false
    private var isShuffle:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
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
        artist = findViewById(R.id.artist)
        title = findViewById(R.id.title)
        artwork = findViewById(R.id.artwork)
        seekBar = findViewById(R.id.seekbar)
        seekBar.setOnSeekBarChangeListener(this)
        current = findViewById(R.id.current)
        length = findViewById(R.id.length)
        repeat = findViewById(R.id.repeat)
        repeat.setOnClickListener(this)
        shuffle = findViewById(R.id.shuffle)
        shuffle.setOnClickListener(this)
        playback = findViewById(R.id.play_pause)
        playback.setOnClickListener(this)
        reverse = findViewById(R.id.reverse)
        reverse.setOnClickListener(this)
        forward = findViewById(R.id.forward)
        forward.setOnClickListener(this)
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
                if (current.text.toString() == length.text.toString()){
                    this.cancel()
                    current.text = "0:00"
                }else{
                    seekBar.progress += 1
                    if(second < 10)
                        current.text = "$minute:0$second"
                    else
                        current.text = "$minute:$second"
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
            artist.text = bundle.getString("artist")
            title.text = bundle.getString("title")
            val albumId = bundle.getString("albumId")
            val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
            val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, albumId!!.toLong())
            Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(artwork)
            val currentMinute = _current / 1000 / 60
            val currentSecond = _current / 1000 % 60
            if(currentSecond<10)
                current.text = "$currentMinute:0$currentSecond"
            else
                current.text = "$currentMinute:$currentSecond"
            val minuteLength = _length / 1000 / 60
            val secondLength = _length / 1000 % 60
            if(secondLength < 10)
                length.text = "$minuteLength:0$secondLength"
            else
                length.text = "$minuteLength:$secondLength"
            seekBar.max = (_length / 1000)
            seekBar.progress = (_current / 1000)
            if(isShuffle)
                shuffle.backgroundTintList = AppCompatResources.getColorStateList(this,R.color.teal_200)
            else
                shuffle.backgroundTintList = AppCompatResources.getColorStateList(this,R.color.white)
            if(isRepeat)
                repeat.backgroundTintList = AppCompatResources.getColorStateList(this,R.color.teal_200)
            else
                repeat.backgroundTintList = AppCompatResources.getColorStateList(this,R.color.white)
            if(isPlaying){
                playback.setImageResource(R.drawable.ic_pause)
                timer = counter(currentMinute,currentSecond)
            }
        }
    }

    fun updateView(_title:String, _artist:String, _albumId:String ,_length:Int){

        if(!isServiceRunning)
            setButtons(true)

        val minuteLength = _length / 1000 / 60
        val secondLength = _length / 1000 % 60
        current.text = "0:00"
        if(secondLength < 10)
            length.text = "$minuteLength:0$secondLength"
        else
            length.text = "$minuteLength:$secondLength"
        title.text = _title
        artist.text = _artist
        val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, _albumId.toLong())
        Picasso.get().load(albumArtUri).placeholder(R.drawable.def).into(artwork)
        seekBar.progress = 0
        seekBar.max = _length / 1000
        if(isPlaying)
            timer = counter(seekBar.progress / 60,seekBar.progress % 60)
    }

    fun setButtons(enable:Boolean){

        isServiceRunning = enable
        isPlaying = enable

        if(!enable){
            repeat.isEnabled = false
            repeat.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            shuffle.isEnabled = false
            shuffle.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            playback.isEnabled = false
            reverse.isEnabled = false
            reverse.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            forward.isEnabled = false
            forward.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.gray)
            seekBar.isEnabled = false
            if(this::timer.isInitialized)
                timer.cancel()
            Picasso.get().load(R.drawable.def).into(artwork)
            title.text = ""
            artist.text = ""
        } else{
            repeat.isEnabled = true
            repeat.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            shuffle.isEnabled = true
            shuffle.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            playback.isEnabled = true
            reverse.isEnabled = true
            reverse.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            forward.isEnabled = true
            forward.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
            seekBar.isEnabled = true
        }
    }

    override fun onClick(p0: View?) {
        when(p0?.id) {
            R.id.play_pause -> sendBroadcast(Intent("com.mymusic.service.PLAYBACK"))
            R.id.reverse -> sendBroadcast(Intent("com.mymusic.service.REVERSE"))
            R.id.forward -> sendBroadcast(Intent("com.mymusic.service.FORWARD"))
            R.id.repeat -> {
                if (isRepeat)
                    repeat.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
                else
                    repeat.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.teal_200)

                sendBroadcast(Intent("com.mymusic.service.SET_REPEAT"))
                isRepeat = !isRepeat
            }
            R.id.shuffle -> {
             if (isShuffle)
                 shuffle.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.white)
             else
                 shuffle.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.teal_200)

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
            current.text = "$currentMinute:$currentSecond"
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
                    playback.setImageResource(R.drawable.ic_pause)
                    timer = counter(seekBar.progress / 60, seekBar.progress % 60)
                    isPlaying = true
                }
                "com.shamsipour.mymusic.PAUSE" -> {
                    playback.setImageResource(R.drawable.ic_play)
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

