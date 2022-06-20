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
import com.shamsipour.mymusic.databinding.FragmentAlbumBinding
import com.shamsipour.mymusic.model.data.AlbumItem
import com.shamsipour.mymusic.model.data.PlaylistItem
import com.shamsipour.mymusic.util.ImageFinder
import com.shamsipour.mymusic.view.activity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumFragment : Fragment() {

    private lateinit var binding: FragmentAlbumBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAlbumBinding.inflate(layoutInflater)
        setUpRecyclerView()
        return binding.root
    }
    private fun setUpRecyclerView() {

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,MediaStore.Audio.Albums.ALBUM
        )
        val loadSongsViewModel = (activity as MainActivity).loadSongsViewModel
        val arrayList: ArrayList<AlbumItem>? = loadSongsViewModel.getAlbums(
            requireContext(),
            projection, projection[0]
        )

        binding.messageTextView2.visibility = View.VISIBLE

        arrayList?.let {
            binding.progressbar2.visibility = View.VISIBLE
            binding.messageTextView2.visibility = View.GONE
                    val albumRVAdapter = AlbumRVAdapter(
                        requireContext(), it
                    )
            binding.recyclerView.apply {
                        adapter = albumRVAdapter
                        layoutManager = GridLayoutManager(
                            activity,2
                        )
                    }
            binding.progressbar2.visibility = View.GONE
                }
            }
        }
