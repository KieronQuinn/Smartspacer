package com.kieronquinn.app.smartspacer.ui.screens.container

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate.BlurMode
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.databinding.FragmentContainerBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseContainerFragment
import com.kieronquinn.app.smartspacer.ui.base.CanShowSnackbar
import com.kieronquinn.app.smartspacer.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.smartspacer.utils.extensions.getTopFragment
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onNavDestinationSelected
import com.kieronquinn.app.smartspacer.utils.extensions.onSwipeDismissed
import com.kieronquinn.app.smartspacer.utils.extensions.setTypeface
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.app.smartspacer.utils.extensions.withAlpha
import com.kieronquinn.monetcompat.extensions.applyMonet
import com.kieronquinn.monetcompat.extensions.toArgb
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ContainerFragment: BaseContainerFragment<FragmentContainerBinding>(FragmentContainerBinding::inflate) {

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

    private val blur by lazy {
        BlurDelegate.get(
            BlurMode.View(
                binding.containerBottomNavigationBlurView,
                binding.containerBottomNavigationBlurTarget,
                requireView().background
            ),
            lifecycleScope
        )
    }

    private val bottomNavigationBackgroundColour by lazy {
        if(requireContext().isDarkMode){
            monet.getMonetColors().neutral2[800]?.toArgb()
        }else{
            monet.getMonetColors().neutral2[100]?.toArgb()
        } ?: monet.getBackgroundColor(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUpdateBadge()
        setupUpdateSnackbar()
        setupPluginRepository()
        viewModel.showShizukuDialogIfNeeded()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() = with(binding.containerBottomNavigation) {
        val container = binding.containerBottomNavigationBlurView
        NavigationUI.setupWithNavController(this, navController)
        setOnItemSelectedListener {  item ->
            //Clear the back stack back to the root if set, to prevent going back between tabs
            navController.popBackStack(rootDestinationId, false)
            navController.onNavDestinationSelected(item)
        }
        onApplyInsets { view, insets ->
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            val bottomInsets = systemInsets.bottom
            val leftInsets = systemInsets.left
            val rightInsets = systemInsets.right
            container.updatePadding(bottom = bottomInsets, left = leftInsets, right = rightInsets)
        }
        val indicatorColor = if(requireContext().isDarkMode){
            monet.getMonetColors().accent2[700]?.toArgb()
        }else{
            monet.getMonetColors().accent2[200]?.toArgb()
        }
        blur.setBlur(1f)
        itemActiveIndicatorColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(indicatorColor ?: Color.TRANSPARENT, Color.TRANSPARENT)
        )
        whenResumed {
            blur.blurAvailable.collect {
                if (it) {
                    container.setBackgroundColor(bottomNavigationBackgroundColour.withAlpha(0.75f))
                } else {
                    container.setBackgroundColor(bottomNavigationBackgroundColour)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(requireContext())) {
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
        viewModel.tabLabel.collect {
            handleUpdateBadge(it)
        }
    }

    private fun handleUpdateBadge(label: String?) = with(binding.containerBottomNavigation) {
        if(label != null){
            getOrCreateBadge(R.id.nav_graph_plugin_repository).apply {
                backgroundColor = badgeBackgroundColour
            }.text = label
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

    override fun setBottomNavigationVisibility(visible: Boolean) {
        binding.containerBottomNavigationBlurView.isVisible = visible
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