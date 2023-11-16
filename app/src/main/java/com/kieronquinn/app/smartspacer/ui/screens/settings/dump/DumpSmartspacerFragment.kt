package com.kieronquinn.app.smartspacer.ui.screens.settings.dump

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentDumpBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationMarginShort
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.filterNotNull
import org.koin.androidx.viewmodel.ext.android.viewModel

class DumpSmartspacerFragment: BoundFragment<FragmentDumpBinding>(FragmentDumpBinding::inflate), BackAvailable, LockCollapsed {

    private val viewModel by viewModel<DumpSmartspacerViewModel>()

    private val writeToFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) {
        viewModel.onWriteToFileSelected(requireContext(), it ?: return@registerForActivityResult)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.dumpScrollable.isNestedScrollingEnabled = false
        setupContent()
        setupSuccessToast()
        setupWriteToFile()
        setupInsets()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun setupContent() = with(binding.dumpContent) {
        text = viewModel.content.value
        whenResumed {
            viewModel.content.collect {
                text = it
            }
        }
    }

    private fun setupSuccessToast() = whenResumed {
        viewModel.successToastBus.filterNotNull().collect {
            Toast.makeText(
                requireContext(), R.string.settings_dump_complete, Toast.LENGTH_LONG
            ).show()
            viewModel.consumeToast()
        }
    }

    private fun setupWriteToFile() = with(binding.dumpWrite) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onWriteToFileClicked(writeToFileLauncher)
            }
        }
    }

    private fun setupInsets() = with(binding.root) {
        applyBottomNavigationMarginShort(resources.getDimension(R.dimen.margin_16))
    }

}