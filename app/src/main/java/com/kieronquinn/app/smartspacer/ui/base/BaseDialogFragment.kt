package com.kieronquinn.app.smartspacer.ui.base

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate
import com.kieronquinn.app.smartspacer.components.blur.BlurDelegate.BlurMode
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.core.MonetCompat
import eightbitlab.com.blurview.BlurView
import org.koin.android.ext.android.inject

abstract class BaseDialogFragment<T: ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T): DialogFragment() {

    internal val monet by lazy {
        MonetCompat.getInstance()
    }

    internal val navigation by inject<ContainerNavigation>()

    internal val binding: T
        get() = _binding ?: throw NullPointerException("Cannot access binding before onCreate or after onDestroy")

    internal var _binding: T? = null

    abstract val blurView: BlurView

    private val blur by lazy {
        BlurDelegate.get(
            BlurMode.Window(requireContext(), requireDialog().window!!),
            lifecycleScope
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflate.invoke(layoutInflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it.decorView) { view, insets ->
                val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(left = navigationInsets.left, right = navigationInsets.right)
                insets
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            dismissWithAnimation()
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        requireDialog().window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blur.animateBlurTo(1f)
        whenResumed {
            blur.blurAvailable.collect {
                onBlurApplied(it)
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        blur.setBlur(0f)
        super.onCancel(dialog)
    }

    override fun dismiss() {
        blur.setBlur(0f)
        super.dismiss()
    }

    abstract fun onBlurApplied(applied: Boolean)

    protected fun dismissWithAnimation() = blur.animateBlurTo(0f) {
        dismiss()
    }

    override fun onResume() {
        super.onResume()
        blur.setBlur(1f)
    }

    override fun getTheme(): Int {
        return if(requireContext().isDarkMode){
            R.style.BaseDialog_Dark
        }else{
            R.style.BaseDialog
        }
    }

}