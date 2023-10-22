package com.kieronquinn.app.smartspacer.ui.screens.safemode

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.kieronquinn.app.smartspacer.databinding.FragmentSafeModeBinding
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet

class SafeModeFragment: BoundFragment<FragmentSafeModeBinding>(FragmentSafeModeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val background = monet.getBackgroundColor(requireContext())
        val toolbar = monet.getBackgroundColorSecondary(requireContext()) ?: background
        requireActivity().window.statusBarColor = toolbar
        requireActivity().window.navigationBarColor = background
        view.setBackgroundColor(background)
        binding.safeModeToolbar.setBackgroundColor(toolbar)
        binding.safeModeRelaunch.applyMonet()
        whenResumed {
            binding.safeModeRelaunch.onClicked().collect {
                startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
            }
        }
    }

}