package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRestoreRequirementsBinding
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.setClassLoaderToPackage
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import kotlinx.coroutines.flow.drop
import org.koin.androidx.viewmodel.ext.android.viewModel

class RestoreRequirementsFragment: BoundFragment<FragmentRestoreRequirementsBinding>(
    FragmentRestoreRequirementsBinding::inflate
), BackAvailable, LockCollapsed, HideBottomNavigation {

    private val adapter by lazy {
        RestoreRequirementsAdapter(
            binding.restoreRequirementsRecyclerview, emptyList(), viewModel::onRequirementClicked
        )
    }

    private val viewModel by viewModel<RestoreRequirementsViewModel>()
    private val navArgs by navArgs<RestoreRequirementsFragmentArgs>()

    private val configurationResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onConfigureResult(requireContext(), it.resultCode == Activity.RESULT_OK)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupControls()
        setupMonet()
        setupState()
        setupAddState()
        viewModel.setupWithConfig(navArgs.config)
    }

    private fun setupMonet() {
        binding.restoreRequirementsLoading.loadingProgress.applyMonet()
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.restoreRequirementsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.restoreRequirementsControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.restoreRequirementsControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.restoreRequirementsControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

    private fun setupRecyclerView() = with(binding.restoreRequirementsRecyclerview) {
        isNestedScrollingEnabled = false
        adapter = this@RestoreRequirementsFragment.adapter
        layoutManager = LinearLayoutManager(context)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.restoreRequirementsLoading.root.isVisible = true
                binding.restoreRequirementsRecyclerview.isVisible = false
                binding.restoreRequirementsControls.isVisible = false
            }
            is State.Loaded -> {
                binding.restoreRequirementsLoading.root.isVisible = false
                binding.restoreRequirementsControls.isVisible = true
                binding.restoreRequirementsRecyclerview.isVisible = state.items.isNotEmpty()
                binding.restoreRequirementsEmpty.isVisible = state.items.isEmpty()
                adapter.items = state.items
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupAddState() = whenCreated {
        viewModel.addState.drop(1).collect {
            handleAddState(it)
        }
    }

    private fun handleAddState(addState: AddState) {
        when(addState){
            is AddState.Configure -> {
                val setupIntent = addState.requirement.setupIntent?.apply {
                    setClassLoaderToPackage(requireContext(), addState.requirement.packageName)
                    putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, addState.requirement.id)
                }
                try {
                    configurationResult.launch(setupIntent)
                }catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        R.string.requirements_add_failed_to_launch_setup,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            is AddState.Idle -> {
                //No-op
            }
        }
    }

}