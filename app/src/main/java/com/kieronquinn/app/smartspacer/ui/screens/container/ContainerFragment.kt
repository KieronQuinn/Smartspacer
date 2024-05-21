package com.kieronquinn.app.smartspacer.ui.screens.container

import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.databinding.FragmentContainerBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseContainerFragment
import com.kieronquinn.app.smartspacer.ui.base.CanShowSnackbar
import com.kieronquinn.app.smartspacer.utils.extensions.getTopFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onSwipeDismissed
import com.kieronquinn.app.smartspacer.utils.extensions.setTypeface
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.applyMonet
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ContainerFragment: BaseContainerFragment<FragmentContainerBinding>(FragmentContainerBinding::inflate) {

    override val bottomNavigation by lazy {
        binding.containerBottomNavigation
    }

    override val fragment by lazy {
        binding.navHostFragment
    }

    override val collapsingToolbar by lazy {
        binding.containerCollapsingToolbar
    }

    override val appBar by lazy {
        binding.containerAppBar
    }

    override val toolbar by lazy {
        binding.containerToolbar
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    private val updateSnackbar by lazy {
        Snackbar.make(
            binding.root, getString(R.string.snackbar_update), Snackbar.LENGTH_INDEFINITE
        ).apply {
            setTypeface(googleSansTextMedium)
            anchorView = binding.containerBottomNavigation
            isAnchorViewLayoutListenerEnabled = true
            view.setBackgroundResource(R.drawable.background_snackbar)
            setAction(R.string.snackbar_update_button){
                viewModel.onUpdateClicked()
            }
            onSwipeDismissed {
                viewModel.onUpdateDismissed()
                navHostFragment.getTopFragment()?.updateSnackbarState(false)
            }
            applyMonet()
        }
    }

    override val navigation by inject<ContainerNavigation>()
    override val rootDestinationId = R.id.nav_graph_main

    private val viewModel by viewModel<ContainerViewModel>()

    private val badgeBackgroundColour by lazy {
        monet.getAccentColor(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUpdateBadge()
        setupUpdateSnackbar()
        setupPluginRepository()
    }

    override fun onResume() {
        super.onResume()
        if(!Settings.canDrawOverlays(requireContext())) {
            viewModel.showDisplayOverOtherAppsDialogIfNeeded()
        }
    }

    private fun setupPluginRepository() {
        handlePluginRepository(viewModel.pluginRepositoryEnabled.value)
        whenResumed {
            viewModel.pluginRepositoryEnabled.collect {
                handlePluginRepository(it)
            }
        }
    }

    private fun handlePluginRepository(enabled: Boolean) = with(binding.containerBottomNavigation){
        menu.findItem(R.id.nav_graph_plugin_repository).isVisible = enabled
    }

    private fun setupUpdateBadge() = whenResumed {
        viewModel.pluginUpdateCount.collect {
            handleUpdateBadge(it)
        }
    }

    private fun handleUpdateBadge(count: Int) = with(binding.containerBottomNavigation) {
        if(count != 0){
            getOrCreateBadge(R.id.nav_graph_plugin_repository).apply {
                backgroundColor = badgeBackgroundColour
            }.number = count
        }else{
            removeBadge(R.id.nav_graph_plugin_repository)
        }
    }

    private fun setupUpdateSnackbar() {
        handleUpdateSnackbar(viewModel.showUpdateSnackbar.value)
        whenResumed {
            viewModel.showUpdateSnackbar.collect {
                handleUpdateSnackbar(it)
            }
        }
    }

    override fun onTopFragmentChanged(topFragment: Fragment, currentDestination: NavDestination) {
        super.onTopFragmentChanged(topFragment, currentDestination)
        viewModel.setCanShowSnackbar(topFragment is CanShowSnackbar)
        topFragment.updateSnackbarState(updateSnackbar.isShown)
    }

    private fun handleUpdateSnackbar(show: Boolean) {
        if(show) {
            //Set state, allow animation time and then show
            navHostFragment.getTopFragment()?.updateSnackbarState(true)
            updateSnackbar.show()
        }else{
            //Dismiss, allow animation time and then update state
            updateSnackbar.dismiss()
            navHostFragment.getTopFragment()?.updateSnackbarState(false)
        }
    }

    private fun Fragment.updateSnackbarState(isVisible: Boolean) {
        (this as? CanShowSnackbar)?.setSnackbarVisible(isVisible)
    }

}