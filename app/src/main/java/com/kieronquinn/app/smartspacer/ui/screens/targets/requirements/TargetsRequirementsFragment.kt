package com.kieronquinn.app.smartspacer.ui.screens.targets.requirements

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRequirementsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType
import com.kieronquinn.app.smartspacer.ui.screens.targets.requirements.add.TargetsRequirementsAddFragment
import com.kieronquinn.app.smartspacer.utils.extensions.getSerializableCompat
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class TargetsRequirementsFragment: BoundFragment<FragmentRequirementsBinding>(FragmentRequirementsBinding::inflate), BackAvailable, LockCollapsed {

    private val args by navArgs<TargetsRequirementsFragmentArgs>()
    private val viewModel by viewModel<TargetsRequirementsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupResult()
    }

    private fun setupViewPager() {
        binding.requirementsViewpager.adapter = TargetsRequirementsViewPagerAdapter(
            args.targetId, this@TargetsRequirementsFragment
        )
        binding.requirementsViewpager.setCurrentItem(viewModel.getCurrentPage(), false)
        val labels = listOf(
            getString(R.string.requirements_tab_any),
            getString(R.string.requirements_tab_all)
        )
        TabLayoutMediator(binding.requirementsTabs, binding.requirementsViewpager) { tab, position ->
            tab.text = labels[position]
        }.attach()
        whenResumed {
            binding.requirementsViewpager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.setCurrentPage(position)
                }
            })
        }
    }

    private fun setupResult() {
        setFragmentResultListener(
            TargetsRequirementsAddFragment.REQUEST_KEY_REQUIREMENTS_ADD
        ){ _, result ->
            val page = result.getSerializableCompat(
                TargetsRequirementsAddFragment.RESULT_KEY_PAGE_TYPE, PageType::class.java
            ) as PageType
            val targetId = args.targetId
            val id = result.getString(TargetsRequirementsAddFragment.RESULT_KEY_ID)
                ?: return@setFragmentResultListener
            val authority = result.getString(TargetsRequirementsAddFragment.RESULT_KEY_AUTHORITY)
                ?: return@setFragmentResultListener
            val packageName = result.getString(TargetsRequirementsAddFragment.RESULT_KEY_PACKAGE_NAME)
                ?: return@setFragmentResultListener
            viewModel.addRequirement(targetId, page, authority, id, packageName)
        }
    }

}
