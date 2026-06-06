package com.kieronquinn.app.smartspacer.ui.screens.complications.requirements

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.databinding.FragmentRequirementsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add.ComplicationsRequirementsAddFragment
import com.kieronquinn.app.smartspacer.utils.extensions.applyMonet
import com.kieronquinn.app.smartspacer.utils.extensions.getSerializableCompat
import com.kieronquinn.app.smartspacer.utils.extensions.onSelected
import com.kieronquinn.app.smartspacer.utils.extensions.selectTab
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class ComplicationsRequirementsFragment: BoundFragment<FragmentRequirementsBinding>(FragmentRequirementsBinding::inflate), BackAvailable, LockCollapsed {

    private val args by navArgs<ComplicationsRequirementsFragmentArgs>()
    private val viewModel by viewModel<ComplicationsRequirementsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupTabs()
        setupViewPager()
        setupResult()
    }

    private fun setupMonet() {
        binding.requirementsTabs.applyMonet()
        val secondaryBackground = ColorStateList.valueOf(
            monet.getBackgroundColorSecondary(requireContext())
                ?: monet.getBackgroundColor(requireContext())
        )
        binding.requirementsTabsContainer.backgroundTintList = secondaryBackground
    }

    private fun setupTabs() = with(binding.requirementsTabs){
        selectTab(viewModel.getCurrentPage())
        whenResumed {
            onSelected().collect {
                viewModel.setCurrentPage(it)
                binding.requirementsViewpager.setCurrentItem(it, true)
            }
        }
    }

    private fun setupViewPager() = with(binding.requirementsViewpager) {
        isUserInputEnabled = false
        adapter = ComplicationsRequirementsViewPagerAdapter(
            args.complicationId, this@ComplicationsRequirementsFragment
        )
        setCurrentItem(viewModel.getCurrentPage(), false)
    }

    private fun setupResult() {
        setFragmentResultListener(
            ComplicationsRequirementsAddFragment.REQUEST_KEY_REQUIREMENTS_ADD
        ){ _, result ->
            val page = result.getSerializableCompat(
                ComplicationsRequirementsAddFragment.RESULT_KEY_PAGE_TYPE, PageType::class.java
            ) as PageType
            val complicationId = args.complicationId
            val id = result.getString(ComplicationsRequirementsAddFragment.RESULT_KEY_ID)
                ?: return@setFragmentResultListener
            val authority = result.getString(ComplicationsRequirementsAddFragment.RESULT_KEY_AUTHORITY)
                ?: return@setFragmentResultListener
            val packageName = result.getString(ComplicationsRequirementsAddFragment.RESULT_KEY_PACKAGE_NAME)
                ?: return@setFragmentResultListener
            viewModel.addRequirement(complicationId, page, authority, id, packageName)
        }
    }

}