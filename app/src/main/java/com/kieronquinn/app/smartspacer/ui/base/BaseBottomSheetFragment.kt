package com.kieronquinn.app.smartspacer.ui.base

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurProvider
import com.kieronquinn.app.smartspacer.utils.extensions.awaitPost
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.or
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat
import org.koin.android.ext.android.inject

abstract class BaseBottomSheetFragment<T: ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T): BottomSheetDialogFragment() {

    companion object {
        private val SYSTEM_INSETS = setOf(
            WindowInsetsCompat.Type.systemBars(),
            WindowInsetsCompat.Type.ime(),
            WindowInsetsCompat.Type.statusBars(),
            WindowInsetsCompat.Type.displayCutout()
        ).or()
    }

    internal val monet by lazy {
        MonetCompat.getInstance()
    }

    internal val binding: T
        get() = _binding ?: throw NullPointerException("Cannot access binding before onCreate or after onDestroy")

    internal var _binding: T? = null

    private val blurProvider by inject<BlurProvider>()
    private var isBlurShowing = false
    private val bottomSheetCallback = object: BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {}

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            showBlurAnimation?.cancel()
            applyBlur(1f + slideOffset)
        }

    }

    open val cancelable: Boolean = true
    open val fullScreen: Boolean = false
    private var behavior: BottomSheetBehavior<*>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = object : BottomSheetDialog(requireContext(), theme) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()

                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                }

                findViewById<View>(com.google.android.material.R.id.container)?.apply {
                    fitsSystemWindows = false
                    val topMargin = marginTop
                    val leftMargin = marginLeft
                    val rightMargin = marginRight
                    ViewCompat.setOnApplyWindowInsetsListener(this){ view, insets ->
                        val systemInsets = insets.getInsets(SYSTEM_INSETS)
                        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            updateMargins(
                                top = topMargin + systemInsets.top,
                                left = leftMargin + systemInsets.left,
                                right = rightMargin + systemInsets.right
                            )
                        }
                        insets
                    }
                }

                findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
            }
        }
        dialog.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.navigationBarColor = Color.TRANSPARENT
            ViewCompat.setOnApplyWindowInsetsListener(it.decorView) { view, insets ->
                val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(left = navigationInsets.left, right = navigationInsets.right)
                insets
            }
        }
        dialog.setOnShowListener {
            if(view == null) return@setOnShowListener
            val parent = binding.root.parent as View
            parent.backgroundTintList = ColorStateList.valueOf(monet.getBackgroundColor(requireContext()))
            if(fullScreen){
                parent.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = resources.displayMetrics.heightPixels
                }
            }
            behavior = dialog.behavior.apply {
                isDraggable = cancelable
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
                if(fullScreen){
                    peekHeight = resources.displayMetrics.heightPixels
                }
                addBottomSheetCallback(bottomSheetCallback)
            }
        }
        isCancelable = cancelable
        return dialog
    }

    override fun onDestroyView() {
        if(isBlurShowing){
            val dialogWindow = dialog?.window ?: return
            val appWindow = activity?.window ?: return
            blurProvider.applyDialogBlur(dialogWindow, appWindow, 0f)
            isBlurShowing = false
        }
        super.onDestroyView()
        behavior?.removeBottomSheetCallback(bottomSheetCallback)
        behavior = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = inflate.invoke(layoutInflater, container, false)
        return binding.root
    }

    private var showBlurAnimation: ValueAnimator? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showBlurAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250L
            addUpdateListener {
                applyBlur(it.animatedValue as Float)
            }
            addListener(onEnd = {
                isBlurShowing = true
            })
            start()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        applyBlur(0f)
        super.onCancel(dialog)
    }

    override fun dismiss() {
        applyBlur(0f)
        super.dismiss()
    }

    private fun applyBlur(ratio: Float){
        val dialogWindow = dialog?.window ?: return
        val appWindow = activity?.window ?: return
        whenResumed {
            dialogWindow.decorView.awaitPost()
            blurProvider.applyDialogBlur(dialogWindow, appWindow, ratio)
        }
    }

    override fun onResume() {
        super.onResume()
        if(isBlurShowing){
            whenResumed {
                view?.awaitPost()
                applyBlur(1f)
            }
        }
    }

    override fun getTheme(): Int {
        return if(requireContext().isDarkMode){
            R.style.BaseBottomSheetDialog_Dark
        }else{
            R.style.BaseBottomSheetDialog
        }
    }

}