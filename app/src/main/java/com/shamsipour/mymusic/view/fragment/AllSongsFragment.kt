package com.shamsipour.mymusic.view.fragment

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.adapter.PlayRecyclerViewAdapter
import com.shamsipour.mymusic.enum.DataType
import com.shamsipour.mymusic.interfaces.OnPlaylistItemLongClick
import com.shamsipour.mymusic.model.data.SongItem
import com.shamsipour.mymusic.util.ImageFinder
import com.shamsipour.mymusic.view.activity.MainActivity
import com.shamsipour.mymusic.view.activity.SelectSongsActivity
import com.shamsipour.mymusic.viewmodel.DataViewModel
import kotlinx.coroutines.*

private const val ARG_PARAM1 = "type"
private const val ARG_PARAM2 = "id"

class AllSongsFragment : Fragment(), OnPlaylistItemLongClick {

    private var param1: DataType? = null
    private var param2: String? = null
    private lateinit var textView:TextView
    private lateinit var progressBar:ProgressBar
    private lateinit var recyclerView:RecyclerView
    private lateinit var fragmentView:View
    private lateinit var imageFinder: ImageFinder
    private lateinit var dataArrayList:ArrayList<SongItem>
    private lateinit var loadSongsViewModel:DataViewModel
    private var contentObserver:ContentObserver? = null

    companion object {
        @JvmStatic
        fun newInstance(param1: DataType, param2: String?) =
            AllSongsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PARAM1, param1)
                    param2?.let {
                        putString(ARG_PARAM2, it)
                    }
                }
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        loadSongsViewModel = (requireActivity() as MainActivity).loadSongsViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getSerializable(ARG_PARAM1) as DataType?
            param2 = it.getString(ARG_PARAM2)
        }

        contentObserver = object : ContentObserver(null){
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                setUpRecyclerView()
            }
        }

        if(param1 == DataType.PLAYLISTS)
            requireContext().contentResolver.registerContentObserver(MediaStore.Audio.Playlists.Members.getContentUri("external",
            param2!!.toLong()),true,contentObserver!!)
        else
            requireContext().contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,contentObserver!!)
    }


    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_all_songs, container, false)
        imageFinder = ImageFinder()
        initViews()
        setUpRecyclerView()
        return fragmentView
    }

    private fun initViews(){
        textView = fragmentView.findViewById(R.id.message_text_view)
        progressBar = fragmentView.findViewById(R.id.progressbar)
        recyclerView = fragmentView.findViewById(R.id.recyclerView)
    }

    private fun setUpRecyclerView(){


        val projection = arrayOf(MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA)

        var cursor:Cursor? = null
        when(param1){
            DataType.SONGS -> cursor = loadSongsViewModel.getAllSongs(requireContext(), projection, projection[0])
            DataType.ALBUMS -> cursor =  loadSongsViewModel.getItems(requireContext(),projection,null,DataType.ALBUMS,
                                                                                                                param2!!)
        }

        if(cursor?.moveToFirst() == true){

            GlobalScope.launch(Dispatchers.Default) {
                dataArrayList = cursorToArrayList(cursor)
                withContext(Dispatchers.Main){
                    progressBar.visibility = View.VISIBLE
                    textView.visibility = View.GONE
                    val allSongsRecyclerViewAdapter = PlayRecyclerViewAdapter(requireContext(),dataArrayList,param1!!,
                        this@AllSongsFragment)
                    recyclerView.apply {
                        adapter = allSongsRecyclerViewAdapter
                        layoutManager = LinearLayoutManager(activity,LinearLayoutManager.VERTICAL,false)
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }else{
            textView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            val allSongsRecyclerViewAdapter = PlayRecyclerViewAdapter(requireContext(),
                ArrayList(),param1!!,this@AllSongsFragment)
            recyclerView.apply {
                adapter = allSongsRecyclerViewAdapter
                layoutManager = LinearLayoutManager(activity,LinearLayoutManager.VERTICAL,false)
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
         DeleteBottomFragment.newInstance("${songItem.title}\n${songItem.artist}",param2!!.toLong(),songItem.id,DataType.SONGS)
             .show(requireActivity().supportFragmentManager,"delete")
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().contentResolver.unregisterContentObserver(contentObserver!!)
    }

}