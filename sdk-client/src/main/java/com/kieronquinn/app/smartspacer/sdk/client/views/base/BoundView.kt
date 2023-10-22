package com.kieronquinn.app.smartspacer.sdk.client.views.base

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.viewbinding.ViewBinding

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("ViewConstructor")
abstract class BoundView<V: ViewBinding>(
    context: Context,
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> V,
    attrs: AttributeSet? = null,
): FrameLayout(context, attrs), LifecycleOwner {

    private val layoutInflater = LayoutInflater.from(context)
    private var _binding: V? = null
    private var isResumed = false

    private val lifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    protected val binding
        get() = _binding ?: throw RuntimeException(
            "Unable to access binding before onAttachedToWindow or after onDetachedFromWindow"
        )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeAllViews()
        _binding = inflate(layoutInflater, this, false)
        addView(binding.root)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        resume()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _binding = null
        if(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if(visibility == View.VISIBLE){
            resume()
        }else{
            pause()
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if(isVisible){
            resume()
        }else{
            pause()
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private fun resume() {
        if(isResumed) return
        isResumed = true
        onResume()
    }

    private fun pause() {
        if(!isResumed) return
        isResumed = false
        pause()
    }

    @CallSuper
    open fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    @CallSuper
    open fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

}