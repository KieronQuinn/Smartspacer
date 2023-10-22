package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.complete

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRestoreCompleteBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor

class RestoreCompleteFragment: BoundFragment<FragmentRestoreCompleteBinding>(FragmentRestoreCompleteBinding::inflate), BackAvailable, HideBottomNavigation {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClose()
    }

    private fun setupClose() = with(binding.restoreCompleteClose) {
        val accent = monet.getAccentColor(requireContext())
        setTextColor(accent)
        overrideRippleColor(accent)
        whenResumed {
            onClicked().collect {
                findNavController().popBackStack(R.id.backupRestoreFragment, false)
            }
        }
    }

}