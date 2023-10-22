package com.kieronquinn.app.smartspacer.ui.base

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.viewbinding.ViewBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.BaseNavigation
import com.kieronquinn.app.smartspacer.components.navigation.setupWithNavigation
import com.kieronquinn.app.smartspacer.utils.extensions.*
import com.kieronquinn.monetcompat.extensions.toArgb

abstract class BaseContainerFragment<V: ViewBinding>(inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): BoundFragment<V>(inflate) {

    companion object {
        private val SYSTEM_INSETS = setOf(
            WindowInsetsCompat.Type.systemBars(),
            WindowInsetsCompat.Type.ime(),
            WindowInsetsCompat.Type.statusBars(),
            WindowInsetsCompat.Type.displayCutout()
        ).or()
    }

    abstract val navigation: BaseNavigation
    abstract val bottomNavigation: BottomNavigationView?
    abstract val collapsingToolbar: CollapsingToolbarLayout?
    abstract val appBar: AppBarLayout?
    abstract val toolbar: Toolbar?
    abstract val fragment: FragmentContainerView
    abstract val navHostFragment: NavHostFragment

    private val googleSansMedium by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
    }

    protected val googleSansTextMedium by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
    }

    private val navController by lazy {
        navHostFragment.navController
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStack()
        setupCollapsedState()
        setupNavigation()
        setupCollapsingToolbar()
        setupToolbar()
        setupBack()
        setupAppBar()
        setupInsets()
        bottomNavigation?.let {
            it.setupBottomNavigation()
            NavigationUI.setupWithNavController(it, navController)
        }
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
    }

    private fun BottomNavigationView.setupBottomNavigation() {
        onApplyInsets { view, insets ->
            val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height).toInt()
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            val bottomInsets = systemInsets.bottom
            val leftInsets = systemInsets.left
            val rightInsets = systemInsets.right
            view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                height = bottomNavHeight + bottomInsets
            }
            view.updatePadding(bottom = bottomInsets, left = leftInsets, right = rightInsets)
        }
        setBackgroundColor(monet.getBackgroundColor(context))
        val color = if(requireContext().isDarkMode){
            monet.getMonetColors().neutral2[800]?.toArgb()
        }else{
            monet.getMonetColors().neutral2[100]?.toArgb()
        } ?: monet.getBackgroundColor(requireContext())
        val indicatorColor = if(requireContext().isDarkMode){
            monet.getMonetColors().accent2[700]?.toArgb()
        }else{
            monet.getMonetColors().accent2[200]?.toArgb()
        }
        setBackgroundColor(ColorUtils.setAlphaComponent(color, 235))
        itemActiveIndicatorColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(indicatorColor ?: Color.TRANSPARENT, Color.TRANSPARENT)
        )
    }

    @SuppressLint("RestrictedApi")
    private fun setupCollapsingToolbar() = collapsingToolbar?.run {
        setBackgroundColor(monet.getBackgroundColor(requireContext()))
        setContentScrimColor(monet.getBackgroundColorSecondary(requireContext()) ?: monet.getBackgroundColor(requireContext()))
        setExpandedTitleTypeface(googleSansMedium)
        setCollapsedTitleTypeface(googleSansMedium)
        lineSpacingMultiplier = 1.1f
    }

    private fun setupStack() {
        whenResumed {
            navController.onDestinationChanged().collect {
                onTopFragmentChanged(
                    navHostFragment.getTopFragment() ?: return@collect, it
                )
            }
        }
        whenCreated {
            onTopFragmentChanged(
                navHostFragment.getTopFragment() ?: return@whenCreated,
                navController.currentDestination ?: return@whenCreated
            )
        }
    }

    private fun setupCollapsedState() = whenResumed {
        appBar?.collapsedState()?.collect {
            navHostFragment.getTopFragment()?.rememberAppBarCollapsed(it)
        }
    }

    private fun setupToolbar() = toolbar?.run {
        whenResumed {
            onNavigationIconClicked().collect {
                (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@collect
                }
                (this@BaseContainerFragment as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@collect
                }
                if(!navController.popBackStack()) {
                    requireActivity().finish()
                }
            }
        }
    }

    private fun setupBack() {
        val callback = requireActivity().onBackPressedDispatcher.addCallback(
            this,
            shouldBackDispatcherBeEnabled()
        ) {
            (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
                if(it.onBackPressed()) return@addCallback
            }
            if(!navController.popBackStack()) {
                requireActivity().finish()
            }
        }
        whenResumed {
            navController.onDestinationChanged().collect {
                callback.isEnabled = shouldBackDispatcherBeEnabled()
            }
        }
    }

    private fun shouldBackDispatcherBeEnabled(): Boolean {
        val top = navHostFragment.getTopFragment()
        return top is ProvidesBack || top !is Root
    }

    private fun setupAppBar() = appBar?.run {
        addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            fragment.updatePadding(bottom = appBarLayout.totalScrollRange + verticalOffset)
        }
    }

    open fun onTopFragmentChanged(topFragment: Fragment, currentDestination: NavDestination){
        val backIcon = if(topFragment is BackAvailable || this is BackAvailable){
            val icon = (topFragment as? BackAvailable)?.backIcon
                ?: (this as? BackAvailable)?.backIcon ?: R.drawable.ic_back
            ContextCompat.getDrawable(requireContext(), icon)
        } else null
        if(topFragment is ProvidesOverflow){
            setupMenu(topFragment)
        }else{
            setupMenu(null)
        }
        if(topFragment is LockCollapsed || requireContext().isLandscape()) {
            appBar?.setExpanded(false)
        }else {
            appBar?.setExpanded(!topFragment.getRememberedAppBarCollapsed())
        }
        appBar?.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            height = if(topFragment !is NoToolbar){
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
            }else 0
        }
        bottomNavigation?.let {
            it.isVisible = !(topFragment is HideBottomNavigation && topFragment.shouldHideBottomNavigation())
        }
        (topFragment as? ProvidesTitle)?.let {
            val label = it.getTitle() ?: return@let
            collapsingToolbar?.title = label
            toolbar?.title = label
        } ?: run {
            val label = currentDestination.label
            if(label == null || label.isBlank()) return@run
            collapsingToolbar?.title = label
            toolbar?.title = label
        }
        toolbar?.navigationIcon = backIcon
    }

    private fun setupNavigation() = whenCreated {
        navHostFragment.setupWithNavigation(navigation)
    }

    private fun setupMenu(menuProvider: ProvidesOverflow?){
        val menu = toolbar?.menu ?: return
        val menuInflater = MenuInflater(requireContext())
        menu.clear()
        menuProvider?.inflateMenu(menuInflater, menu)
        toolbar?.setOnMenuItemClickListener {
            menuProvider?.onMenuItemSelected(it) ?: false
        }
    }

    private fun setupInsets() = with(fragment) {
        val expandedMargin = resources.getDimensionPixelSize(R.dimen.expanded_title_margin)
        onApplyInsets { view, insets ->
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            val leftInsets = systemInsets.left
            val rightInsets = systemInsets.right
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(left = leftInsets, right = rightInsets)
            }
            toolbar?.updatePadding(left = leftInsets, right = rightInsets)
            val startMargin = if(isRtl()) rightInsets else leftInsets
            val endMargin = if(isRtl()) leftInsets else rightInsets
            collapsingToolbar?.expandedTitleMarginStart = expandedMargin + startMargin
            collapsingToolbar?.expandedTitleMarginEnd = expandedMargin + endMargin
        }
    }

}