package com.shamsipour.mymusic.view.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.adapter.AlbumRVAdapter
import com.shamsipour.mymusic.adapter.PlaylistRVAdapter
import com.shamsipour.mymusic.model.data.AlbumItem
import com.shamsipour.mymusic.model.data.PlaylistItem
import com.shamsipour.mymusic.util.ImageFinder
import com.shamsipour.mymusic.view.activity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumFragment : Fragment() {

    private lateinit var fragmentView:View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = inflater.inflate(R.layout.fragment_album, container, false)
        setUpRecyclerView()
        return fragmentView
    }
    private fun setUpRecyclerView() {

        val textView: TextView = fragmentView.findViewById(R.id.message_text_view2)
        val progressBar: ProgressBar = fragmentView.findViewById(R.id.progressbar2)
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,MediaStore.Audio.Albums.ALBUM
        )
        val loadSongsViewModel = (activity as MainActivity).loadSongsViewModel
        val arrayList: ArrayList<AlbumItem>? = loadSongsViewModel.getAlbums(
            requireContext(),
            projection, projection[0]
        )

        textView.visibility = View.VISIBLE

        arrayList?.let {
            progressBar.visibility = View.VISIBLE
            textView.visibility = View.GONE
                    val albumRVAdapter = AlbumRVAdapter(
                        requireContext(), it
                    )
                    val recyclerView: RecyclerView = fragmentView.findViewById(R.id.recyclerView)
                    recyclerView.apply {
                        adapter = albumRVAdapter
                        layoutManager = GridLayoutManager(
                            activity,2
                        )
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }
