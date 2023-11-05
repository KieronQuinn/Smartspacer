package com.kieronquinn.app.smartspacer.ui.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.NavigationEvent
import com.kieronquinn.app.smartspacer.components.navigation.WidgetOptionsNavigation
import com.kieronquinn.app.smartspacer.databinding.ActivityWidgetOptionsBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.sdk.annotations.LimitedNativeSupport
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.utils.shouldExcludeFromSmartspacer
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.kieronquinn.app.smartspacer.sdk.client.R as ClientR

class WidgetOptionsMenuActivity: BoundActivity<ActivityWidgetOptionsBinding>(ActivityWidgetOptionsBinding::inflate) {

    companion object {
        private const val KEY_TARGET = "target"
        private const val KEY_APP_WIDGET_ID = "app_widget_id"
        private const val KEY_OWNER = "owner"

        fun getIntent(
            context: Context,
            target: SmartspaceTarget,
            appWidgetId: Int,
            owner: String?
        ): Intent {
            return Intent(context, WidgetOptionsMenuActivity::class.java).apply {
                putExtra(KEY_TARGET, target)
                putExtra(KEY_APP_WIDGET_ID, appWidgetId)
                putExtra(KEY_OWNER, owner)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }
    }

    private val viewModel by viewModel<WidgetOptionsMenuViewModel>()
    private val navigation by inject<WidgetOptionsNavigation>()

    private val target by lazy {
        intent.getParcelableExtraCompat(KEY_TARGET, SmartspaceTarget::class.java)
    }

    private val appWidgetId by lazy {
        intent.getIntExtra(KEY_APP_WIDGET_ID, -1).takeIf { it != -1 }
    }

    private val owner by lazy {
        intent.getStringExtra(KEY_OWNER)
    }

    private val adapter by lazy {
        Adapter(loadItems())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        whenCreated {
            monet.awaitMonetReady()
            setupMonet()
            binding.widgetOptions.clipToOutline = true
            binding.widgetOptionsList.run {
                layoutManager = LinearLayoutManager(context)
                adapter = this@WidgetOptionsMenuActivity.adapter
            }
            binding.widgetOptionsClose.onClicked().collect {
                finishAndRemoveTask()
            }
        }
        whenCreated {
            navigation.navigationBus.collect {
                when(it) {
                    is NavigationEvent.Intent -> {
                        try {
                            startActivity(it.intent)
                        }catch (e: ActivityNotFoundException) {
                            it.onActivityNotFound?.invoke()
                        }
                    }
                    else -> throw NotImplementedError()
                }
            }
        }
    }

    @OptIn(LimitedNativeSupport::class)
    private fun loadItems(): List<BaseSettingsItem> {
        val target = target ?: return emptyList()
        val appWidgetId = appWidgetId ?: return emptyList()
        val owner = owner ?: return emptyList()
        return listOfNotNull(
            GenericSettingsItem.Setting(
                getString(ClientR.string.smartspace_long_press_popup_dismiss),
                "",
                ContextCompat.getDrawable(
                    this,
                    ClientR.drawable.ic_smartspace_long_press_dismiss
                )
            ) {
                viewModel.onDismissClicked(target)
                finishAndRemoveTask()
            }.takeIf { target.canBeDismissed },
            GenericSettingsItem.Setting(
                getString(ClientR.string.smartspace_long_press_popup_about),
                "",
                ContextCompat.getDrawable(
                    this,
                    ClientR.drawable.ic_smartspace_long_press_about
                )
            ) {
                viewModel.onAboutClicked(target, ::onError)
                finishAndRemoveTask()
            }.takeIf { target.aboutIntent != null &&
                    target.aboutIntent?.shouldExcludeFromSmartspacer() == false },
            GenericSettingsItem.Setting(
                getString(ClientR.string.smartspace_long_press_popup_settings),
                "",
                ContextCompat.getDrawable(
                    this,
                    ClientR.drawable.ic_smartspace_long_press_settings
                )
            ) {
                viewModel.onSettingsClicked(this)
                finishAndRemoveTask()
            },
            GenericSettingsItem.Setting(
                getString(ClientR.string.smartspace_long_press_popup_feedback),
                "",
                ContextCompat.getDrawable(
                    this,
                    ClientR.drawable.ic_smartspace_long_press_feedback
                )
            ) {
                viewModel.onAboutClicked(target, ::onError)
                finishAndRemoveTask()
            }.takeIf { target.feedbackIntent != null &&
                    target.aboutIntent?.shouldExcludeFromSmartspacer() == false },
            GenericSettingsItem.Setting(
                getString(R.string.widget_options_configure),
                "",
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_settings
                )
            ) {
                viewModel.onConfigureClicked(this, appWidgetId, owner)
                finishAndRemoveTask()
            }
        )
    }

    private fun onError() {
        Toast.makeText(
            this,
            ClientR.string.smartspace_long_press_popup_failed_to_launch,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setupMonet(){
        val background = monet.getBackgroundColorSecondary(this)
            ?: monet.getBackgroundColor(this)
        window?.setBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.background_permission_dialog
            )?.apply {
                setTint(background)
            })
        val accent = monet.getAccentColor(this)
        val accentTint = ColorStateList.valueOf(accent)
        binding.widgetOptionsClose.backgroundTintList = accentTint
    }

    inner class Adapter(list: List<BaseSettingsItem>):
        BaseSettingsAdapter(binding.widgetOptionsList, list)

}