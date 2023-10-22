package com.kieronquinn.app.smartspacer.ui.screens.targets.requirements

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRequirementsPageBinding
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.RequirementHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationMarginShort
import com.kieronquinn.app.smartspacer.utils.extensions.getSerializableCompat
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.setClassLoaderToPackage
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class TargetsRequirementsPageFragment: BoundFragment<FragmentRequirementsPageBinding>(FragmentRequirementsPageBinding::inflate) {

    companion object {
        const val KEY_TARGET_ID = "target_id"
        const val KEY_PAGE_TYPE = "page_type"
    }

    private val targetId
        get() = requireArguments().getString(KEY_TARGET_ID)!!

    private val pageType
        get() = requireArguments().getSerializableCompat(KEY_PAGE_TYPE, PageType::class.java) as PageType

    private val viewModel by viewModel<TargetsRequirementsPageViewModel>()
    private var cachedRequirement: RequirementHolder? = null

    private val configureResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val requirement = cachedRequirement ?: return@registerForActivityResult
        viewModel.notifyRequirementChange(requirement)
        cachedRequirement = null
    }

    private val adapter by lazy {
        TargetsRequirementsPageAdapter(
            binding.requirementsPageRecyclerView,
            emptyList(),
            ::onConfigureClicked,
            viewModel::onDeleteClicked,
            viewModel::invertRequirement
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setup(targetId, pageType)
        setupMonet()
        setupRecyclerView()
        setupState()
        setupEmpty()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun setupMonet() {
        binding.requirementsPageLoading.loadingProgress.applyMonet()
    }

    private fun setupRecyclerView() = with(binding.requirementsPageRecyclerView) {
        isNestedScrollingEnabled = false
        layoutManager = LinearLayoutManager(context)
        adapter = this@TargetsRequirementsPageFragment.adapter
        val fabMargin = resources.getDimension(R.dimen.fab_margin)
        applyBottomNavigationInset(fabMargin)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun setupEmpty() = with(binding.requirementsEmptyLabel) {
        val fabMargin = resources.getDimension(R.dimen.fab_margin)
        applyBottomNavigationMarginShort(fabMargin)
    }

    private fun setupFab() = with(binding.requirementsFabAdd) {
        applyBottomNavigationMarginShort(resources.getDimension(R.dimen.margin_16))
        backgroundTintList = ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        whenResumed {
            onClicked().collect {
                viewModel.onAddClicked()
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.requirementsPageLoading.root.isVisible = true
                binding.requirementsPageRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.requirementsPageLoading.root.isVisible = false
                binding.requirementsPageRecyclerView.isVisible = state.items.isNotEmpty()
                binding.requirementsEmpty.isVisible = state.items.isEmpty()
                adapter.items = state.items
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun onConfigureClicked(requirement: RequirementHolder) {
        cachedRequirement = requirement
        val intent = requirement.configurationIntent?.apply {
            setClassLoaderToPackage(requireContext(), requirement.requirement.packageName)
            putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, requirement.requirement.id)
            putExtra(SmartspacerConstants.EXTRA_AUTHORITY, requirement.requirement.authority)
        } ?: return
        try {
            configureResult.launch(intent)
        }catch (e: Exception) {
            Toast.makeText(
                requireContext(), R.string.requirement_edit_external_error, Toast.LENGTH_LONG
            ).show()
        }
    }

}