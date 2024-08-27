package com.kieronquinn.app.smartspacer.ui.screens.settings.language

import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Card
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.RadioCard
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.settings.SettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.settings.language.SettingsLanguageViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.capitalise
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class SettingsLanguageFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsLanguageViewModel>()

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        SettingsAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) = with(binding) {
        when(state) {
            is State.Loading -> {
                settingsBaseLoading.isVisible = true
                settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                settingsBaseLoading.isVisible = false
                settingsBaseRecyclerView.isVisible = true
                adapter.update(state.getItems(), settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded.getItems(): List<BaseSettingsItem> {
        return listOf(
            Card(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                Html.fromHtml(
                    getString(R.string.settings_language_info),
                    Html.FROM_HTML_MODE_COMPACT
                ),
                contentHash = -1L
            ),
            RadioCard(
                selectedLocale == null,
                getString(R.string.settings_language_default),
                null
            ) {
                onLanguageClicked(null)
            }
        ) + supportedLocales.map {
            RadioCard(
                it == selectedLocale,
                it.displayName.capitalise(),
                null
            ) {
                onLanguageClicked(it)
            }
        }
    }

    private fun onLanguageClicked(locale: Locale?) {
        val locales = locale?.let { LocaleListCompat.create(locale) }
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

}