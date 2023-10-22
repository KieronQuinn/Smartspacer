package com.kieronquinn.app.smartspacer.ui.screens.setup.container

import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupContainerBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseContainerFragment
import org.koin.android.ext.android.inject

class SetupContainerFragment: BaseContainerFragment<FragmentSetupContainerBinding>(FragmentSetupContainerBinding::inflate) {

    override val fragment by lazy {
        binding.navHostFragmentSetup
    }

    override val collapsingToolbar by lazy {
        binding.setupContainerCollapsingToolbar
    }

    override val appBar by lazy {
        binding.setupContainerAppBar
    }

    override val toolbar by lazy {
        binding.setupContainerToolbar
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_setup) as NavHostFragment
    }

    override val bottomNavigation: BottomNavigationView? = null

    override val navigation by inject<SetupNavigation>()

}