package com.kieronquinn.app.smartspacer.ui.screens.repository.details.screenshot

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.kieronquinn.app.smartspacer.databinding.FragmentPluginDetailsScreenshotBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesTitle
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationMarginShort

class PluginDetailsScreenshotFragment: BoundFragment<FragmentPluginDetailsScreenshotBinding>(FragmentPluginDetailsScreenshotBinding::inflate), LockCollapsed, BackAvailable, ProvidesTitle {

    private val navArgs by navArgs<PluginDetailsScreenshotFragmentArgs>()

    private val glide by lazy {
        Glide.with(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pluginDetailsScreenshot.applyBottomNavigationMarginShort()
        glide.load(navArgs.url).transition(withCrossFade()).into(binding.pluginDetailsScreenshot)
    }

    override fun getTitle(): CharSequence? {
        return ""
    }

}