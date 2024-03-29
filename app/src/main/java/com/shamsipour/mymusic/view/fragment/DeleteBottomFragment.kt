package com.shamsipour.mymusic.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.enum.DataType
import com.shamsipour.mymusic.view.activity.PlaylistItemsActivity
import com.shamsipour.mymusic.viewmodel.DataViewModel


private const val ARG_PARAM1 = "name"
private const val ARG_PARAM2 = "playlistId"
private const val ARG_PARAM3 = "songId"
private const val ARG_PARAM4 = "type"


class DeleteBottomFragment : BottomSheetDialogFragment() {

    private var param1: String? = null
    private var param2: Long? = null
    private var param3: String? = null
    private var param4: DataType? = null
    private lateinit var fragmentView:View
    private lateinit var nameTextView: TextView
    private lateinit var positiveBtn: Button
    private lateinit var cancelBtn: Button
    private lateinit var viewModel: DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getLong(ARG_PARAM2)
            param3 = it.getString(ARG_PARAM3)
            param4 = it.getSerializable(ARG_PARAM4) as DataType
        }
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        fragmentView = inflater.inflate(R.layout.fragment_delete_bottom, container, false)
        initViews()
        viewModel = (requireActivity() as PlaylistItemsActivity).loadSongsViewModel

        return fragmentView
    }

    fun initViews(){
        nameTextView = fragmentView.findViewById(R.id.item_name)
        nameTextView.text = param1
        positiveBtn = fragmentView.findViewById(R.id.ok_button)
        cancelBtn = fragmentView.findViewById(R.id.cancel_button)

        positiveBtn.setOnClickListener {
            when(param4){
                DataType.PLAYLISTS -> viewModel.deletePlaylist(requireContext(),param2!!.toLong())
                DataType.SONGS -> viewModel.removeFromPlaylist(requireContext(),param2!!.toLong(),param3!!)
                else -> Toast.makeText(requireContext(),"Invalid Type",Toast.LENGTH_SHORT).show()
            }

            dismiss()
        }
        cancelBtn.setOnClickListener {
            dismiss()
        }
    }

    companion object {


        @JvmStatic fun newInstance(name: String, playlistId: Long, songId:String?, type: DataType) =
                DeleteBottomFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, name)
                        putLong(ARG_PARAM2, playlistId)
                        putString(ARG_PARAM3,songId)
                        putSerializable(ARG_PARAM4,type)
                    }
                }
    }
}