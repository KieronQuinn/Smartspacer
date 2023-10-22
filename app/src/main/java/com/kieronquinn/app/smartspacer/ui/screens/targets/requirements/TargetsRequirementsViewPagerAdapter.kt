package com.kieronquinn.app.smartspacer.ui.screens.targets.requirements

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType

class TargetsRequirementsViewPagerAdapter(
    private val targetId: String,
    fragment: Fragment
): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = PageType.values().size

    override fun createFragment(position: Int): Fragment {
        return TargetsRequirementsPageFragment().apply {
            arguments = bundleOf(
                TargetsRequirementsPageFragment.KEY_TARGET_ID to targetId,
                TargetsRequirementsPageFragment.KEY_PAGE_TYPE to PageType.values()[position]
            )
        }
    }

}