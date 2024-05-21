package com.kieronquinn.app.smartspacer.ui.screens.native

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentNativeBinding
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport.Companion.isNativeModeAvailable
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesBack
import com.kieronquinn.app.smartspacer.ui.base.ProvidesOverflow
import com.kieronquinn.app.smartspacer.ui.screens.native.NativeModeViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class NativeModeFragment: BoundFragment<FragmentNativeBinding>(FragmentNativeBinding::inflate), BackAvailable, LockCollapsed, ProvidesOverflow, HideBottomNavigation, ProvidesBack {

    companion object {
        const val EXTRA_ENABLE_AND_FINISH = "enable_and_finish"
    }

    private val viewModel by viewModel<NativeModeViewModel>()
    private val args by navArgs<NativeModeFragmentArgs>()

    private val adapter by lazy {
        NativeModeAdapter(binding.nativeInfoCompatibilityList, emptyList())
    }

    override val backIcon: Int
        get() {
            return if(wasLaunchedFromNotification()) {
                R.drawable.ic_close
            }else{
                R.drawable.ic_back
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoading()
        setupInfo()
        setupCompatibility()
        setupReboot()
        setupSwitch()
        setupState()
        setupScroll()
        setupControls()
        setupCompatibilityList()
        setupOpenShizuku()
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menu.clear()
        if(!args.isSetup){
            menuInflater.inflate(R.menu.menu_native_smartspace, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        viewModel.onSettingsClicked(args.isFromSettings)
        return true
    }

    override fun shouldHideBottomNavigation(): Boolean {
        return !args.isFromSettings
    }

    override fun interceptBack(): Boolean {
        return wasLaunchedFromDeepLink() || wasLaunchedFromNotification()
    }

    override fun onBackPressed(): Boolean {
        return when {
            wasLaunchedFromDeepLink() -> {
                //Since this was launched by a deep link, re-open the main activity on back
                startActivity(Intent(requireContext(), MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
                clearDeepLink()
                true
            }
            wasLaunchedFromNotification() -> {
                //When launched from a notification, don't annoy the user and just close
                requireActivity().finish()
                true
            }
            else -> false
        }
    }

    private fun wasLaunchedFromDeepLink(): Boolean {
        return requireActivity().intent.data == Uri.parse("smartspacer://native")
    }

    private fun clearDeepLink() {
        requireActivity().intent.data = null
    }

    private fun wasLaunchedFromNotification(): Boolean {
        return requireActivity().intent.data ==
                Uri.parse("smartspacer://native-from-notification")
    }

    private fun setupLoading() = with(binding.nativeLoading) {
        loadingProgress.applyMonet()
    }

    private fun setupInfo() = with(binding.nativeInfoCard) {
        applyBackgroundTint(monet)
    }

    private fun setupReboot() = with(binding.nativeInfoReboot) {
        applyBackgroundTint(monet)
    }

    private fun setupCompatibility() = with(binding.nativeInfoCompatibility) {
        applyBackgroundTint(monet)
    }

    private fun setupScroll() = with(binding.nativeLoaded) {
        isNestedScrollingEnabled = false
        if(args.isSetup){
            onApplyInsets { view, insets ->
                view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            }
        }else {
            applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
        }
    }

    private fun setupSwitch() = with(binding.nativeSwitch) {
        whenResumed {
            onClicked().collect {
                viewModel.onSwitchClicked()
            }
        }
    }

    private fun setupCompatibilityList() = with(binding.nativeInfoCompatibilityList) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@NativeModeFragment.adapter
    }

    private fun setupControls() = with(binding.nativeControls) {
        isVisible = args.isSetup
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.nativeControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.nativeControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.nativeControlsNext.onClicked().collect {
                if(!args.isSetup) return@collect
                viewModel.onNextClicked()
            }
        }
    }

    private fun setupOpenShizuku() = with(binding.nativeShizukuErrorOpen) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onOpenShizukuClicked(requireContext(), args.isSetup)
            }
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.nativeLoading.root.isVisible = true
                binding.nativeShizukuError.isVisible = false
                binding.nativeSwitch.isVisible = false
                binding.nativeLoaded.isVisible = false
            }
            is State.Loaded -> {
                binding.nativeLoading.root.isVisible = false
                binding.nativeSwitch.isVisible = state.shizukuReady
                binding.nativeLoaded.isVisible = state.shizukuReady
                binding.nativeShizukuError.isVisible = !state.shizukuReady
                binding.nativeSwitch.isChecked = state.isEnabled
                binding.nativeSwitchContent.text = state.compatibility.getContent()
                adapter.items = state.compatibility
                adapter.notifyDataSetChanged()
                state.autoEnableIfRequired()
            }
            is State.Dismiss -> {
                viewModel.dismiss()
            }
        }
    }

    private fun State.Loaded.autoEnableIfRequired() {
        if(!requireActivity().intent.getAndRemoveExtra(EXTRA_ENABLE_AND_FINISH)) return
        if(!shizukuReady) return
        if(isEnabled) return
        if(!compatibility.isNativeModeAvailable()) return
        viewModel.onSwitchClicked()
        requireActivity().finish()
    }

    private fun Intent.getAndRemoveExtra(key: String): Boolean {
        if(!hasExtra(key)) return false
        val extra = getBooleanExtra(key, false)
        removeExtra(key)
        return extra
    }

    private fun List<CompatibilityReport>.getContent(): String {
        val format = if(size == 1){
            first().label
        }else{
            val joiner = getString(R.string.native_switch_content_joiner)
            val items = subList(0, size - 1).joinToString("$joiner ") {
                it.label
            }
            val last = last().label
            getString(R.string.native_switch_content_many, items, last)
        }
        return getString(R.string.native_switch_content, format)
    }

}