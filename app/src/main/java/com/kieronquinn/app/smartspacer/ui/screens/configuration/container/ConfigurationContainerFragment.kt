package com.kieronquinn.app.smartspacer.ui.screens.configuration.container

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationBinding
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.base.BaseContainerFragment
import org.koin.android.ext.android.inject

class ConfigurationContainerFragment: BaseContainerFragment<FragmentConfigurationBinding>(FragmentConfigurationBinding::inflate) {

    override val navigation by inject<ConfigurationNavigation>()
    override val bottomNavigation: BottomNavigationView? = null

    override val appBar by lazy {
        binding.configurationContainerAppBar
    }

    override val toolbar by lazy {
        binding.configurationContainerToolbar
    }

    override val collapsingToolbar by lazy {
        binding.configurationContainerCollapsingToolbar
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_configuration) as NavHostFragment
    }

    override val fragment by lazy {
        binding.navHostFragmentConfiguration
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navGraph = ConfigurationActivity.getNavGraph(requireActivity() as ConfigurationActivity)
            ?: throw RuntimeException("No Nav Graph specified for ConfigurationActivity")
        val navController = navHostFragment.navController
        navController.graph = navController.navInflater.inflate(navGraph.graph)
    }

}