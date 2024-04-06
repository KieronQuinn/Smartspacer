package com.kieronquinn.app.smartspacer.ui.screens.expanded.container

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.DummyNavigation
import com.kieronquinn.app.smartspacer.components.navigation.ExpandedNavigation
import com.kieronquinn.app.smartspacer.databinding.FragmentContainerExpandedBinding
import com.kieronquinn.app.smartspacer.ui.activities.ExpandedActivity
import com.kieronquinn.app.smartspacer.ui.base.BaseContainerFragment
import org.koin.android.ext.android.inject

class ExpandedContainerFragment: BaseContainerFragment<FragmentContainerExpandedBinding>(FragmentContainerExpandedBinding::inflate) {

    override val appBar: AppBarLayout? = null
    override val bottomNavigation: BottomNavigationView? = null
    override val collapsingToolbar: CollapsingToolbarLayout? = null
    override val toolbar: Toolbar? = null
    override val handleInsets = false

    private val _navigation by inject<ExpandedNavigation>()

    private val isOverlay by lazy {
        ExpandedActivity.isOverlay(requireActivity() as ExpandedActivity)
    }

    override val navigation by lazy {
        if(isOverlay){
            DummyNavigation()
        }else{
            _navigation
        }
    }

    override val fragment by lazy {
        binding.navHostFragmentExpanded
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_expanded) as NavHostFragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background = ColorDrawable(Color.TRANSPARENT)
    }

}