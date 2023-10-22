package com.kieronquinn.app.smartspacer.ui.screens.contributors

import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.contributors.ContributorsViewModel.ContributorsSettingsItem
import com.kieronquinn.app.smartspacer.utils.extensions.getResourceIdArray
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class ContributorsFragment: BaseSettingsFragment(), BackAvailable {

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    private val viewModel by viewModel<ContributorsViewModel>()

    override val adapter by lazy {
        ContributorsAdapter(binding.settingsBaseRecyclerView, emptyList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsBaseLoading.isVisible = true
        binding.settingsBaseRecyclerView.isVisible = false
        whenResumed {
            adapter.update(createItems(), binding.settingsBaseRecyclerView)
            binding.settingsBaseLoading.isVisible = false
            binding.settingsBaseRecyclerView.isVisible = true
        }
    }

    private fun createItems(): List<BaseSettingsItem> {
        val icons = ContributorsSettingsItem.LinkedSetting(
            getString(R.string.about_contributors_icons),
            Html.fromHtml(getString(R.string.about_contributors_icons_content), Html.FROM_HTML_MODE_COMPACT),
            R.drawable.ic_contributions_icons,
            viewModel::onLinkClicked
        )
        val sdk = ContributorsSettingsItem.LinkedSetting(
            getString(R.string.about_contributors_sdk),
            Html.fromHtml(getString(R.string.about_contributors_sdk_content), Html.FROM_HTML_MODE_COMPACT),
            R.drawable.ic_contributors_sdk,
            viewModel::onLinkClicked
        )
        return listOf(icons, sdk, *getTranslatorsList())
    }

    private fun getTranslatorsList(): Array<BaseSettingsItem> {
        val headings = resources.getResourceIdArray(R.array.about_translators_headings)
        val content = resources.getResourceIdArray(R.array.about_translators_content)
        val flags = resources.getResourceIdArray(R.array.about_translators_flags)
        return headings.mapIndexed { index, resource ->
            GenericSettingsItem.Setting(
                getString(resource),
                getString(content[index]),
                ContextCompat.getDrawable(requireContext(), flags[index])
            ){}
        }.toTypedArray()
    }

}