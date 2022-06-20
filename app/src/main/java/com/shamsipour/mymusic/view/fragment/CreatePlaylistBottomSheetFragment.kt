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
import com.shamsipour.mymusic.databinding.FragmentCreatePlaylistBottomSheetBinding
import com.shamsipour.mymusic.view.activity.MainActivity
import com.shamsipour.mymusic.viewmodel.DataViewModel


class CreatePlaylistBottomSheetFragment : BottomSheetDialogFragment() {


    private lateinit var binding: FragmentCreatePlaylistBottomSheetBinding
    private lateinit var viewModel: DataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreatePlaylistBottomSheetBinding.inflate(layoutInflater)
        initViews()
        viewModel = (requireActivity() as MainActivity).loadSongsViewModel
        return binding.root
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

        binding.editText.addTextChangedListener{
            if (binding.errorTextView.visibility == View.VISIBLE){
                binding.errorTextView.visibility = View.GONE
                binding.editText.setHintTextColor(ContextCompat.getColor(requireContext(),R.color.teal_200))
                binding.editText.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.teal_200))
            }
        }
        binding.createPlaylistBtn.setOnClickListener {
            if(binding.editText.text.isEmpty()){
                binding.errorTextView.text = "The name cannot be blank"
                binding.errorTextView.visibility = View.VISIBLE
                binding.editText.setHintTextColor(Color.parseColor("#FF0000"))
                binding.editText.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF0000"))
            }
            else{
                viewModel.createPlaylist(requireContext(),binding.editText.text.toString())
                dismiss()
            }
        }
        binding.cancelBtn.setOnClickListener {
            dismiss()
        }
    }


}