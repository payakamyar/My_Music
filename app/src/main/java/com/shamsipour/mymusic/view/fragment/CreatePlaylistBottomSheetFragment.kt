package com.shamsipour.mymusic.view.fragment

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shamsipour.mymusic.R
import com.shamsipour.mymusic.view.activity.MainActivity
import com.shamsipour.mymusic.viewmodel.DataViewModel


class CreatePlaylistBottomSheetFragment : BottomSheetDialogFragment() {


    private lateinit var fragmentView:View
    private lateinit var inputText:EditText
    private lateinit var errorTextView: TextView
    private lateinit var createPlaylistBtn:Button
    private lateinit var cancelBtn:Button
    private lateinit var viewModel: DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView =  inflater.inflate(R.layout.fragment_create_playlist_bottom_sheet, container, false)
        initViews()
        viewModel = (requireActivity() as MainActivity).loadSongsViewModel
        return fragmentView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sheet = super.onCreateDialog(savedInstanceState)
        if(sheet is BottomSheetDialog){
            sheet.behavior.skipCollapsed = true
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return sheet
    }

    fun initViews(){
        inputText = fragmentView.findViewById(R.id.editText)
        errorTextView = fragmentView.findViewById(R.id.error_text_view)
        createPlaylistBtn = fragmentView.findViewById(R.id.create_playlist_btn)
        cancelBtn = fragmentView.findViewById(R.id.cancel_btn)

        inputText.addTextChangedListener{
            if (errorTextView.visibility == View.VISIBLE){
                errorTextView.visibility = View.GONE
                inputText.setHintTextColor(ContextCompat.getColor(requireContext(),R.color.teal_200))
                inputText.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.teal_200))
            }
        }
        createPlaylistBtn.setOnClickListener {
            if(inputText.text.isEmpty()){
                errorTextView.text = "The name cannot be blank"
                errorTextView.visibility = View.VISIBLE
                inputText.setHintTextColor(Color.parseColor("#FF0000"))
                inputText.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF0000"))
            }
            else{
                viewModel.createPlaylist(requireContext(),inputText.text.toString())
                dismiss()
            }
        }
        cancelBtn.setOnClickListener {
            dismiss()
        }
    }


}