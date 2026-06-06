package com.kieronquinn.app.smartspacer.ui.activities.permission.client

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate.BlurMode
import com.kieronquinn.app.smartspacer.databinding.ActivityPermissionSmartspaceBinding
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.ui.activities.BoundActivity
import com.kieronquinn.app.smartspacer.utils.extensions.addDimming
import com.kieronquinn.app.smartspacer.utils.extensions.clearDimming
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.android.ext.android.inject
import java.util.UUID

class SmartspacerClientPermissionActivity: BoundActivity<ActivityPermissionSmartspaceBinding>(ActivityPermissionSmartspaceBinding::inflate) {

    companion object {
        private const val EXTRA_GRANT = "package_name"

        fun createPendingIntent(context: Context, grant: Grant): PendingIntent {
            val intent = createIntent(context, grant)
            return PendingIntent.getActivity(
                context,
                UUID.randomUUID().hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun createIntent(context: Context, grant: Grant): Intent {
            return Intent(context, SmartspacerClientPermissionActivity::class.java).apply {
                putExtra(EXTRA_GRANT, grant)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                applySecurity(context)
            }
        }
    }

    private val grant by lazy {
        intent.getParcelableExtraCompat(EXTRA_GRANT, Grant::class.java)
            ?: throw RuntimeException("No grant provided")
    }

    private val blur by lazy {
        BlurDelegate.get(BlurMode.Window(this, window), lifecycleScope)
    }

    private val background by lazy {
        ContextCompat.getDrawable(this, R.drawable.background_permission_dialog)
    }

    private val grantRepository by inject<GrantRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        intent.verifySecurity()
        whenCreated {
            monet.awaitMonetReady()
            setupMonet()
            setupMonet()
            setupTitle()
            setupAllow()
            setupDeny()
        }
        whenCreated {
            blur.animateBlurTo(1f)
            blur.blurAvailable.collect {
                if (it) {
                    window?.clearDimming()
                    background?.setTint(monet.getPrimaryColor(this@SmartspacerClientPermissionActivity))
                    // We don't know if the blurred background will be light or dark so use more alpha
                    background?.alpha = 191
                } else {
                    window?.addDimming()
                    background?.setTint(monet.getBackgroundColor(this@SmartspacerClientPermissionActivity))
                    background?.alpha = 255
                }
            }
        }
    }

    private fun setupMonet(){
        val backgroundTint = monet.getBackgroundColorSecondary(this)
            ?: monet.getBackgroundColor(this)
        background?.setTint(backgroundTint)
        binding.permissionSmartspace.background = background
        val accent = monet.getAccentColor(this)
        val accentTint = ColorStateList.valueOf(accent)
        binding.permissionSmartspaceAllow.backgroundTintList = accentTint
        binding.permissionSmartspaceDeny.backgroundTintList = accentTint
    }

    private fun setupTitle() = with(binding.permissionSmartspaceTitle){
        val packageLabel = context.packageManager.getPackageLabel(grant.packageName)
            ?: getString(R.string.permission_dialog_widget_title_unknown)
        val title = SpannableStringBuilder().apply {
            append(getString(R.string.permission_dialog_widget_title_prefix))
            append(" ")
            append(packageLabel, StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(" ")
            append(getString(R.string.permission_dialog_widget_title_suffix))
        }
        text = title
    }

    private fun setupAllow() = with(binding.permissionSmartspaceAllow){
        whenResumed {
            onClicked().collect {
                grant.smartspace = true
                grantRepository.addGrant(grant)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun setupDeny() = with(binding.permissionSmartspaceDeny){
        whenResumed {
            onClicked().collect {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

}