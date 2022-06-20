package com.shamsipour.mymusic.service

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.model.data.SongItem
import com.shamsipour.mymusic.util.ImageFinder
import com.shamsipour.mymusic.view.activity.MainActivity
import com.shamsipour.mymusic.view.activity.PlaybackActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class PlaybackService: Service(),MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener{

    private lateinit var broadcastReceiver:PlayBroadcast
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationContent:RemoteViews
    private var current: SongItem? = null
    private var position:Int = -1
    private lateinit var imageFinder: ImageFinder
    private lateinit var playlist:ArrayList<SongItem>
    private lateinit var ogPlaylist:ArrayList<SongItem>
    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private var isShuffle = false
    private var isRepeat = false
    private var isNotificationCreated:Boolean = false
    var isPlaying:Boolean = false
    lateinit var sharedPreferences:SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("states", MODE_PRIVATE)
        isRepeat = sharedPreferences.getBoolean("isRepeat",false)
        isShuffle = sharedPreferences.getBoolean("isShuffle",false)

        notificationManagerCompat = NotificationManagerCompat.from(this)
        if(Build.VERSION.SDK_INT > 25)
            createNotificationChannel()
        val filter:IntentFilter = IntentFilter().apply {
            addAction("com.mymusic.service.PLAYBACK")
            addAction("com.mymusic.service.REVERSE")
            addAction("com.mymusic.service.FORWARD")
            addAction("com.mymusic.service.STOP")
            addAction("com.mymusic.service.SEEK_TO")
            addAction("com.mymusic.service.SET_REPEAT")
            addAction("com.mymusic.service.SET_SHUFFLE")
            addAction("com.mymusic.service.GET_INFO")

        }
        imageFinder = ImageFinder()
        broadcastReceiver = PlayBroadcast()
        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener(this@PlaybackService)
            setOnCompletionListener(this@PlaybackService)
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build()
            )
        }
        registerReceiver(broadcastReceiver,filter)
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val selected:SongItem = intent?.getParcelableExtra("selected")!!
        ogPlaylist = intent.getParcelableArrayListExtra("playlist")!!
        playlist = ArrayList()
        playlist.addAll(ogPlaylist)

        if(isShuffle)
            playlist.shuffle()

        position = playlist.indexOf(selected)

        startSong(selected,false)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.reset()
        mediaPlayer.release()
        unregisterReceiver(broadcastReceiver)
        val intent = Intent().also { it.action = "com.shamsipour.mymusic.STOP" }
        sendBroadcast(intent)
    }

    private fun albumIdToBitmap(albumId:String):Bitmap{
        var bitmap: Bitmap
        val job = GlobalScope.async {
            imageFinder.fetchCompressedArtwork(applicationContext,albumId,100,100,ImageFinder.DEFAULT_OPTIONS)
        }
        runBlocking {
            bitmap = job.await()
        }
        return bitmap
    }

    private fun foregroundNotification(title:String,artist:String,artwork:Bitmap,isPlaying:Boolean):NotificationCompat.Builder{

         notificationContent = RemoteViews(packageName,R.layout.notification_playback).apply {
             setTextViewText(R.id.title,title)
             setTextViewText(R.id.artist,artist)
             setImageViewBitmap(R.id.imageview,artwork)
             setImageViewResource(R.id.playback,
                            if(isPlaying)R.drawable.ic_pause else R.drawable.ic_play)
         }
        setClickPendingIntentNotification(notificationContent)
        val taskStack = TaskStackBuilder.create(this).apply {
            addParentStack(PlaybackActivity::class.java)
            addNextIntent(Intent(this@PlaybackService,PlaybackActivity::class.java))
        }
        val pendingIntent = taskStack.getPendingIntent(20,PendingIntent.FLAG_UPDATE_CURRENT)
         return NotificationCompat.Builder(this,"131").apply {
             setSmallIcon(R.mipmap.ic_launcher)
             setContent(notificationContent)
             priority = NotificationCompat.PRIORITY_LOW
             setContentIntent(pendingIntent)
             setOngoing(true)
        }
    }

    private fun setClickPendingIntentNotification(view:RemoteViews){

        val intent = Intent().also {
                                            it.action = "com.mymusic.service.PLAYBACK" }
        var pending = PendingIntent.getBroadcast(applicationContext,3000,intent,0)
        view.setOnClickPendingIntent(R.id.playback,pending)
        intent.action = "com.mymusic.service.REVERSE"
        pending = PendingIntent.getBroadcast(this,3000,intent,0)
        view.setOnClickPendingIntent(R.id.reverse,pending)
        intent.action = "com.mymusic.service.FORWARD"
        pending = PendingIntent.getBroadcast(this,3000,intent,0)
        view.setOnClickPendingIntent(R.id.forward,pending)
        intent.action = "com.mymusic.service.STOP"
        pending = PendingIntent.getBroadcast(this,3000,intent,0)
        view.setOnClickPendingIntent(R.id.close,pending)

    }

    @RequiresApi(26)
    private fun createNotificationChannel(){
        val channel = NotificationChannel("131","Playback",NotificationManager.IMPORTANCE_LOW)
        notificationManagerCompat.createNotificationChannel(channel)
    }

    private fun startSong(songItem: SongItem,forwardOrReverse:Boolean){
        current?.let {
            if(it == songItem){
                if(forwardOrReverse){
                    mediaPlayer.apply {
                        reset()
                        setDataSource(this@PlaybackService,Uri.parse(songItem.location))
                        mediaPlayer.prepareAsync()
                    }
                } else{
                    if(mediaPlayer.isPlaying) pauseSong() else playSong()
                }
            } else{
                mediaPlayer.apply {
                    reset()
                    setDataSource(this@PlaybackService,Uri.parse(songItem.location))
                    current = songItem
                    mediaPlayer.prepareAsync()
                }
            }
        } ?: run {
            isPlaying = true
            mediaPlayer.setDataSource(this,Uri.parse(songItem.location))
            current = songItem
            mediaPlayer.prepareAsync()
        }
    }

    override fun onPrepared(p0: MediaPlayer?) {
        p0?.let {
            if(isPlaying)
                playSong()
            else{
                notificationManagerCompat.notify(131,
                    foregroundNotification(current!!.title,current!!.artist,albumIdToBitmap(current!!.albumId),
                        false).build())
            }

            val intent = Intent().also {
                it.action = "com.shamsipour.mymusic.SWITCH_SONG"
                it.putExtra("title",current!!.title)
                it.putExtra("artist",current!!.artist)
                it.putExtra("albumId",current!!.albumId)
                it.putExtra("length",mediaPlayer.duration)
            }
            sendBroadcast(intent)
        }
    }

    override fun onCompletion(p0: MediaPlayer?) {
        if(isRepeat){
            startSong(current!!,true)
        } else{
            forwardSong()
        }
    }

    private fun playSong(){
        isPlaying = true
        if(!isNotificationCreated){
            startForeground(131,foregroundNotification(current!!.title,current!!.artist,
                albumIdToBitmap(current!!.albumId),true).build())
            isNotificationCreated = true
        } else{
            notificationManagerCompat.notify(131,
                foregroundNotification(current!!.title,current!!.artist,albumIdToBitmap(current!!.albumId),
                    true).build())
        }
        mediaPlayer.start()
        val intent = Intent().also { it.action = "com.shamsipour.mymusic.PLAY" }
        sendBroadcast(intent)
    }

    private fun pauseSong(){
        isPlaying = false
        notificationManagerCompat.notify(131, foregroundNotification(current!!.title,current!!.artist,
            albumIdToBitmap(current!!.albumId),false).build())
        mediaPlayer.pause()
        val intent = Intent().also { it.action = "com.shamsipour.mymusic.PAUSE" }
        sendBroadcast(intent)
    }

    private fun stopService(){
        stopSelf()
        val intent = Intent().also { it.action = "com.shamsipour.mymusic.STOP" }
        sendBroadcast(intent)
    }

    private fun forwardSong(){
        if(position + 1 >= playlist.size)
            position = 0
        else
            ++position

        val selected = playlist.get(position)
        startSong(selected,true)
    }
    private fun reverseSong(){
        if(position - 1 < 0)
            position = playlist.size - 1
        else
            --position

        val selected = playlist.get(position)
        startSong(selected,true)
    }

    fun sendInfo(){
        val length:Int = mediaPlayer.duration
        val current:Int = mediaPlayer.currentPosition
        val bundle = Bundle()
        bundle.apply {
            putString("title",this@PlaybackService.current!!.title)
            putString("artist",this@PlaybackService.current!!.artist)
            putString("albumId",this@PlaybackService.current!!.albumId)
            putInt("length",length)
            putInt("current",current)
            putBoolean("shuffle",isShuffle)
            putBoolean("repeat",isRepeat)
            putBoolean("playing",isPlaying)
        }
        val intent = Intent().also {
            it.action = "com.shamsipour.mymusic.GET_INFO"
            it.putExtra("data",bundle)
        }
        sendBroadcast(intent)
    }

    private inner class PlayBroadcast:BroadcastReceiver(){

        override fun onReceive(p0: Context?, p1: Intent?){
            when(p1?.action){
                "com.mymusic.service.PLAYBACK" -> {
                    if(!mediaPlayer.isPlaying)
                        playSong()
                    else
                        pauseSong()
                }
                "com.mymusic.service.REVERSE" -> reverseSong()
                "com.mymusic.service.FORWARD" -> forwardSong()
                "com.mymusic.service.STOP" -> stopService()
                "com.mymusic.service.SET_REPEAT" -> {
                    isRepeat = !isRepeat
                    sharedPreferences.edit().putBoolean("isRepeat",isRepeat).apply()
                }
                "com.mymusic.service.SET_SHUFFLE" -> {
                    isShuffle = !isShuffle
                    sharedPreferences.edit().putBoolean("isShuffle",isShuffle).apply()
                    if(isShuffle)
                        playlist.shuffle()
                    else{
                        playlist.removeAll(playlist)
                        playlist.addAll(ogPlaylist)
                    }
                    position = playlist.indexOf(current!!)
                }
                "com.mymusic.service.GET_INFO" -> sendInfo()
                "com.mymusic.service.SEEK_TO" -> mediaPlayer.seekTo(p1.getIntExtra("seekValue",0))
                else -> Log.e("Invalid","Wrong command")
            }
        }

    }

}