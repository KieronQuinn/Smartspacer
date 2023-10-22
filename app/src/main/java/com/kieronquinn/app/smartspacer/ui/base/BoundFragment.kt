package com.kieronquinn.app.smartspacer.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.viewbinding.ViewBinding
import com.google.android.material.transition.FadeThroughProvider
import com.google.android.material.transition.MaterialSharedAxis
import com.google.android.material.transition.SlideDistanceProvider
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.monetcompat.app.MonetFragment
import kotlin.math.roundToInt

abstract class BoundFragment<V: ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): MonetFragment() {

    private var _binding: V? = null

    protected val binding: V
        get() = _binding ?: throw NullPointerException("Unable to access binding before onCreateView or after onDestroyView")

    open val applyTransitions = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(applyTransitions) {
            exitTransition = getMaterialSharedAxis(requireContext(), true)
            enterTransition = getMaterialSharedAxis(requireContext(), true)
            returnTransition = getMaterialSharedAxis(requireContext(), false)
            reenterTransition = getMaterialSharedAxis(requireContext(), false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getMaterialSharedAxis(context: Context, forward: Boolean): MaterialSharedAxis {
        return MaterialSharedAxis(MaterialSharedAxis.X, forward).apply {
            (primaryAnimatorProvider as SlideDistanceProvider).slideDistance =
                context.resources.getDimension(R.dimen.shared_axis_x_slide_distance).roundToInt()
            duration = 450L
            (secondaryAnimatorProvider as FadeThroughProvider).progressThreshold = 0.22f
            interpolator = AnimationUtils.loadInterpolator(context, R.anim.fast_out_extra_slow_in)
        }
    }

}