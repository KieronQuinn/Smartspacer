package com.kieronquinn.app.smartspacer.ui.screens.permission

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.google.android.material.shape.CornerFamily
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentPermissionWidgetBinding
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseDialogFragment
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.android.ext.android.inject

class WidgetPermissionDialogFragment: BaseDialogFragment<FragmentPermissionWidgetBinding>(FragmentPermissionWidgetBinding::inflate) {

    companion object {
        const val REQUEST_WIDGET_PERMISSION = "widget_permission"
        const val RESULT_GRANTED = "granted"
    }

    private val grantRepository by inject<GrantRepository>()
    private val args by navArgs<WidgetPermissionDialogFragmentArgs>()

    private val buttonCornerExtraRound by lazy {
        resources.getDimension(R.dimen.margin_16)
    }

    private val buttonCornerRound by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupTitle()
        setupAllowOnce()
        setupAllowAlways()
        setupDeny()
    }

    private fun setupMonet(){
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        dialog?.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.background_permission_dialog
        )?.apply {
            setTint(background)
        })
        val accent = monet.getAccentColor(requireContext())
        val accentTint = ColorStateList.valueOf(accent)
        binding.permissionWidgetAllowAlways.backgroundTintList = accentTint
        binding.permissionWidgetAllowOnce.backgroundTintList = accentTint
        binding.permissionWidgetDeny.backgroundTintList = accentTint
    }

    private fun setupTitle() = with(binding.permissionWidgetTitle){
        val grant = args.grant
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

    private fun setupAllowOnce() = with(binding.permissionWidgetAllowOnce){
        shapeAppearanceModel = shapeAppearanceModel.toBuilder().apply {
            setTopLeftCorner(CornerFamily.ROUNDED, buttonCornerExtraRound)
            setTopRightCorner(CornerFamily.ROUNDED, buttonCornerExtraRound)
            setBottomLeftCorner(CornerFamily.ROUNDED, buttonCornerRound)
            setBottomRightCorner(CornerFamily.ROUNDED, buttonCornerRound)
        }.build()
        whenResumed {
            onClicked().collect {
                setFragmentResult(REQUEST_WIDGET_PERMISSION, bundleOf(RESULT_GRANTED to true))
                dismissWithAnimation()
            }
        }
    }

    private fun setupAllowAlways() = with(binding.permissionWidgetAllowAlways){
        shapeAppearanceModel = shapeAppearanceModel.toBuilder().apply {
            setAllCorners(CornerFamily.ROUNDED, buttonCornerRound)
        }.build()
        whenResumed {
            onClicked().collect {
                val grant = args.grant.apply {
                    widget = true
                }
                grantRepository.addGrant(grant)
                setFragmentResult(REQUEST_WIDGET_PERMISSION, bundleOf(RESULT_GRANTED to true))
                dismissWithAnimation()
            }
        }
    }

    private fun setupDeny() = with(binding.permissionWidgetDeny){
        shapeAppearanceModel = shapeAppearanceModel.toBuilder().apply {
            setTopLeftCorner(CornerFamily.ROUNDED, buttonCornerRound)
            setTopRightCorner(CornerFamily.ROUNDED, buttonCornerRound)
            setBottomLeftCorner(CornerFamily.ROUNDED, buttonCornerExtraRound)
            setBottomRightCorner(CornerFamily.ROUNDED, buttonCornerExtraRound)
        }.build()
        whenResumed {
            onClicked().collect {
                setFragmentResult(REQUEST_WIDGET_PERMISSION, bundleOf(RESULT_GRANTED to false))
                dismissWithAnimation()
            }
        }
    }

}