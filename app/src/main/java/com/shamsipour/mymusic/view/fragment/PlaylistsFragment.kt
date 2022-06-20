package com.shamsipour.mymusic.view.fragment

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.adapter.PlaylistRVAdapter
import com.shamsipour.mymusic.databinding.FragmentPlaylistsBinding
import com.shamsipour.mymusic.interfaces.SimpleCallback
import com.shamsipour.mymusic.model.data.PlaylistItem
import com.shamsipour.mymusic.model.data.SongItem
import com.shamsipour.mymusic.view.activity.MainActivity
import com.shamsipour.mymusic.view.activity.PlaylistItemsActivity
import com.shamsipour.mymusic.viewmodel.DataViewModel
import kotlinx.coroutines.*


class PlaylistsFragment : Fragment(), SimpleCallback {

    private lateinit var fragmentBinding: FragmentPlaylistsBinding
    private lateinit var playlistRVAdapter:PlaylistRVAdapter
    private var arrayList: ArrayList<PlaylistItem>? = null
    private lateinit var loadSongsViewModel:DataViewModel
    private var contentObserver:ContentObserver? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
            fragmentBinding = FragmentPlaylistsBinding.inflate(layoutInflater)
            initViews()
            setUpRecyclerView()
            contentObserver = object : ContentObserver(null){
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    setUpRecyclerView()
                }
            }
            requireContext().contentResolver.registerContentObserver(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,true,
                contentObserver!!)
        return fragmentBinding.root
    }


    private fun initViews(){
        fragmentBinding.addPlaylistBtn.setOnClickListener {
            val fragment = CreatePlaylistBottomSheetFragment()
            fragment.show(requireActivity().supportFragmentManager,"createPlaylist")
        }
        loadSongsViewModel = (activity as MainActivity).loadSongsViewModel
    }

    private fun setUpRecyclerView(){

        lifecycleScope.launch(Dispatchers.Main) {
            fragmentBinding.messageTextView2.visibility = View.VISIBLE

            withContext(Dispatchers.IO){
                getData()
            }

            arrayList?.let {
                fragmentBinding.progressbar2.visibility = View.VISIBLE
                fragmentBinding.messageTextView2.visibility = View.GONE


                playlistRVAdapter = PlaylistRVAdapter(requireContext(),it,this@PlaylistsFragment)
                fragmentBinding.recyclerView2.apply {
                    adapter = playlistRVAdapter
                    layoutManager = LinearLayoutManager(activity,
                        LinearLayoutManager.VERTICAL,false)
                }
                fragmentBinding.progressbar2.visibility = View.GONE
            }?: kotlin.run {

                playlistRVAdapter = PlaylistRVAdapter(requireContext(), ArrayList(),this@PlaylistsFragment)
                fragmentBinding.recyclerView2.apply {
                    adapter = playlistRVAdapter
                    layoutManager = LinearLayoutManager(activity,
                        LinearLayoutManager.VERTICAL,false)
                }
            }
        }


    }

    private fun getData(){

        val projection = arrayOf(
            MediaStore.Audio.Playlists.NAME, MediaStore.Audio.Playlists.Members._ID)
        arrayList = loadSongsViewModel.getPlaylists(requireContext(),
            projection, projection[0])

    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().contentResolver.unregisterContentObserver(contentObserver!!)
    }

    override fun callback(bundle: Bundle?) {
        val intent = Intent(context, PlaylistItemsActivity::class.java).apply {
            putExtra("id",bundle!!.getString("id"))
            putExtra("name",bundle.getString("name"))
        }
        val include: View = requireActivity().findViewById(R.id.bottom_playback2)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),
            (requireActivity() as MainActivity).activityBinding.bottomPlayback2.root,"bottom_playback")
        startActivity(intent, options.toBundle())
    }


}


