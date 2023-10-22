package com.kieronquinn.app.smartspacer.ui.screens.complications.requirements

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType

class ComplicationsRequirementsViewPagerAdapter(
    private val requirementId: String,
    fragment: Fragment
): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = PageType.values().size

    override fun createFragment(position: Int): Fragment {
        return ComplicationsRequirementsPageFragment().apply {
            arguments = bundleOf(
                ComplicationsRequirementsPageFragment.KEY_REQUIREMENT_ID to requirementId,
                ComplicationsRequirementsPageFragment.KEY_PAGE_TYPE to PageType.values()[position]
            )
        }
    }

}