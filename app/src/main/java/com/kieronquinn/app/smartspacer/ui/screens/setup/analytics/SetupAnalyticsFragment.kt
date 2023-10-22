package com.kieronquinn.app.smartspacer.ui.screens.setup.analytics

import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupAnalyticsBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.LockCollapsed
import com.kieronquinn.app.smartspacer.ui.base.ProvidesBack
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.onNavigationIconClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupAnalyticsFragment: BoundFragment<FragmentSetupAnalyticsBinding>(FragmentSetupAnalyticsBinding::inflate), BackAvailable, ProvidesBack, LockCollapsed {

    private val viewModel by viewModel<SetupAnalyticsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupContent()
        setupAllow()
        setupDeny()
        binding.root.isNestedScrollingEnabled = false
    }

    private fun setupToolbar() = with(binding.setupAnalyticsToolbar) {
        onApplyInsets { view, insets ->
            view.updateLayoutParams<LinearLayout.LayoutParams> {
                updateMargins(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            }
        }
        whenResumed {
            onNavigationIconClicked().collect {
                viewModel.onBackPressed()
            }
        }
    }

    private fun setupContent() = with(binding.setupAnalyticsContent) {
        text = Html.fromHtml(
            getString(R.string.setup_analytics_content),
            Html.FROM_HTML_MODE_LEGACY
        )
        Linkify.addLinks(this, Linkify.WEB_URLS)
        setLinkTextColor(monet.getAccentColor(requireContext()))
        highlightColor = monet.getAccentColor(requireContext())
        movementMethod = BetterLinkMovementMethod.newInstance().setOnLinkClickListener { _, url ->
            viewModel.onLinkClicked(url)
            true
        }
    }

    private fun setupAllow() = with(binding.setupAnalyticsAllow) {
        applyMonet()
        whenResumed {
            onClicked().collect {
                viewModel.onAllowClicked()
            }
        }
    }

    private fun setupDeny() = with(binding.setupAnalyticsDeny) {
        val accentColor = monet.getAccentColor(requireContext())
        setTextColor(accentColor)
        whenResumed {
            onClicked().collect {
                viewModel.onDenyClicked()
            }
        }
    }

    override fun onBackPressed() = viewModel.onBackPressed()

}