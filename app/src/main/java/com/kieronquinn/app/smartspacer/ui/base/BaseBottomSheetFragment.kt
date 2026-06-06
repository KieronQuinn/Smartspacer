package com.kieronquinn.app.smartspacer.ui.base

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate.BlurMode
import com.kieronquinn.app.smartspacer.utils.extensions.addDimming
import com.kieronquinn.app.smartspacer.utils.extensions.clearDimming
import com.kieronquinn.app.smartspacer.utils.extensions.getBackgroundForBlur
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.or
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.monetcompat.core.MonetCompat

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

    private val bottomSheetCallback = object: BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {}

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            _binding?.root?.alpha = 1f + slideOffset
            blur.setBlur(1f + slideOffset)
        }

    }

    open val cancelable: Boolean = true
    open val fullScreen: Boolean = false
    private var behavior: BottomSheetBehavior<*>? = null

    private val blur by lazy {
        BlurDelegate.get(
            BlurMode.Window(requireContext(), requireDialog().window!!),
            lifecycleScope
        )
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blur.animateBlurTo(1f)
        whenCreated {
            val parent = binding.root.parent as View
            parent.background.setTint(monet.getBackgroundColor(requireContext()))
            blur.blurAvailable.collect { available ->
                val parent = binding.root.parent as View
                if (available) {
                    requireDialog().window?.clearDimming()
                    parent.background.setTint(monet.getBackgroundForBlur(requireContext()))
                } else {
                    requireDialog().window?.addDimming()
                    parent.background.setTint(monet.getBackgroundColor(requireContext()))
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        blur.animateBlurTo(0f)
        super.onCancel(dialog)
    }

    override fun getTheme(): Int {
        return if(requireContext().isDarkMode){
            R.style.BaseBottomSheetDialog_Dark
        }else{
            R.style.BaseBottomSheetDialog
        }
    }

}