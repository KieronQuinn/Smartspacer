package com.kieronquinn.app.smartspacer.components.blur

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.drawables.SmartspacerBackgroundBlurDrawable
import com.kieronquinn.app.smartspacer.utils.extensions.findActivity
import com.kieronquinn.app.smartspacer.utils.extensions.getViewRootImpl
import com.kieronquinn.app.smartspacer.utils.extensions.isAttached
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BlurDelegate(
    private val mode: BlurMode,
    private val scope: CoroutineScope
): KoinComponent {

    companion object {
        fun get(
            mode: BlurMode,
            scope: CoroutineScope
        ): BlurDelegate {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    BlurDelegate31(mode, scope)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    BlurDelegate30(mode, scope)
                }
                else -> BlurDelegateStub(mode, scope)
            }
        }

        private fun lerp(start: Float, stop: Float, amount: Float): Float {
            return start + (stop - start) * amount
        }
    }

    private val minBlurRadius by lazy {
        mode.context.resources.getDimension(R.dimen.min_window_blur_radius)
    }

    private val maxBlurRadius by lazy {
        mode.context.resources.getDimension(R.dimen.max_window_blur_radius)
    }

    private val settings by inject<SmartspacerSettingsRepository>()
    protected var lastAppliedBlur: Float? = null
    protected var lastRequestedBlur: Float? = null
    private val animationLock = Any()
    private var blurAnimationJob: Job? = null

    val blurAvailable by lazy {
        mode.view.isAttached().flatMapLatest { attached ->
            when {
                !attached -> flowOf(null)
                !isModeSupported(mode) -> flowOf(false)
                else -> combine(settings.blurEnabled.asFlow(), isBlurAvailable(mode.view.context)) {
                    it.all { i -> i }
                }
            }
        }.distinctUntilChanged().filterNotNull().shareIn(scope, SharingStarted.Eagerly)
    }

    val view: View
        get() = mode.view

    fun getMode() = mode

    @CallSuper
    protected open fun applyBlur(ratio: Float) {
        when (mode) {
            is BlurMode.View -> {
                val radius = blurRadiusOfRatio(ratio)
                mode.view.setBlurRadius(radius / 5f)
                lastAppliedBlur = ratio
            }
            is BlurMode.Views -> {
                val radius = blurRadiusOfRatio(ratio)
                mode.views.forEach {
                    it.setBlurRadius(radius / 5f)
                }
                lastAppliedBlur = ratio
            }
            else -> {
                // Not handled here
            }
        }
    }

    protected abstract fun isModeSupported(mode: BlurMode): Boolean
    abstract fun isBlurAvailable(context: Context): Flow<Boolean>

    /**
     *  Immediately applies blur if it is available, otherwise stores the value to be set when it
     *  becomes available. If an animation is running, cancels it before applying.
     */
    fun setBlur(ratio: Float) {
        synchronized(animationLock) {
            blurAnimationJob?.cancel()
        }
        applyBlur(ratio)
    }

    /**
     *  Animates blur to a given value, storing the value as it goes for if blur becomes available
     *  during the process.
     */
    fun animateBlurTo(ratio: Float, onFinish: () -> Unit = {}) {
        synchronized(animationLock) {
            blurAnimationJob?.cancel()
            blurAnimationJob = scope.launch {
                animate(lastAppliedBlur ?: 0f, ratio).collect {
                    val value = it.animatedValue as Float
                    applyBlur(value)
                    if (value <= 0f) {
                        onFinish()
                    }
                }
            }
        }
    }

    protected fun blurRadiusOfRatio(ratio: Float): Int {
        return if (ratio != 0.0f) {
            lerp(minBlurRadius, maxBlurRadius, ratio).toInt()
        } else 0
    }

    private fun animate(from: Float, to: Float) = callbackFlow {
        val animation = ValueAnimator.ofFloat(from, to).apply {
            duration = 250L
            addUpdateListener {
                trySend(it)
            }
            start()
        }
        awaitClose {
            animation.cancel()
        }
    }

    private fun setBlurOnEnable(scope: CoroutineScope) = scope.launch {
        isBlurAvailable(view.context).collect { enabled ->
            if (enabled) {
                lastRequestedBlur?.let {
                    applyBlur(it)
                }
            }
        }
    }

    init {
        view.post {
            setBlurOnEnable(scope)
        }
    }

    sealed class BlurMode {
        data class Window(
            override val context: Context,
            override val window: android.view.Window,
            val blurBehind: Boolean = false
        ) : BlurMode() {
            override val view get() = window.decorView
        }

        data class View(
            private val v: android.view.View,
            private val target: BlurTarget,
            private val background: Drawable = Color.TRANSPARENT.toDrawable(),
            private val overlay: Int = Color.TRANSPARENT
        ) : BlurMode() {

            private val activity by lazy {
                v.context.findActivity()
                    ?: throw IllegalStateException("Attached activity is required for View blur")
            }

            private val blurView by lazy {
                val blurView = if (v !is BlurView) {
                    val parent = v.parent as? ViewGroup
                        ?: throw IllegalStateException("Invalid parent")
                    BlurView(v.context).also {
                        parent.post {
                            if (!parent.isAttachedToWindow) return@post
                            parent.removeView(v)
                            it.addView(v)
                            parent.addView(it)
                        }
                    }
                } else v
                blurView.also {
                    it.setupWith(target).setFrameClearDrawable(background).setOverlayColor(overlay)
                }
            }

            override val view
                get() = blurView
            override val window: android.view.Window
                get() = activity.window
            override val context: Context
                get() = blurView.context
        }

        data class Views(
            val views: List<BlurView>,
            private val target: BlurTarget,
            private val background: Drawable = Color.TRANSPARENT.toDrawable(),
            private val overlay: Int = Color.TRANSPARENT
        ) : BlurMode() {

            private val activity by lazy {
                view.context.findActivity()
                    ?: throw IllegalStateException("Attached activity is required for Views blur")
            }

            override val view: android.view.View
                get() = views.first()
            override val window: android.view.Window
                get() = activity.window
            override val context: Context
                get() = view.context

            init {
                views.forEach {
                    it.setupWith(target).setFrameClearDrawable(background).setOverlayColor(overlay)
                }
            }
        }

        data class Background(
            override val view: android.view.View,
            private val colour: Int,
            private val disabledColour: Int = colour
        ) : BlurMode() {

            private val activity by lazy {
                view.context.findActivity()
                    ?: throw IllegalStateException("Attached activity is required for Background blur")
            }

            private val delegate = BlurBackgroundDelegate.get(view, colour, disabledColour)

            override val window: android.view.Window
                get() = activity.window
            override val context: Context
                get() = view.context

            fun setBlurEnabled(enabled: Boolean) {
                view.post {
                    delegate.setBlurEnabled(enabled)
                }
            }

            fun setBlurRadius(radius: Int) = delegate.setBlurRadius(radius)
            fun setCornerRadius(cornerRadius: Float) = delegate.setCornerRadius(cornerRadius)
            fun setCornerRadius(
                cornerRadiusTL: Float,
                cornerRadiusTR: Float,
                cornerRadiusBL: Float,
                cornerRadiusBR: Float
            ) = delegate.setCornerRadius(cornerRadiusTL, cornerRadiusTR, cornerRadiusBL, cornerRadiusBR)

            open class BlurBackgroundDelegate private constructor(
                open val view: android.view.View,
                open val colour: Int
            ) {

                companion object {
                    fun get(
                        view: android.view.View,
                        colour: Int,
                        disabledColour: Int
                    ): BlurBackgroundDelegate {
                        return when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                BlurBackgroundDelegate31(view, colour, disabledColour)
                            }
                            else -> BlurBackgroundDelegate(view, disabledColour)
                        }
                    }
                }

                private val colourDrawable = colour.toDrawable()

                @CallSuper
                open fun setBlurEnabled(enabled: Boolean) {
                    // Always set the colour if blur is not supported
                    view.background = colourDrawable
                }

                open fun setBlurRadius(radius: Int) {
                    // No-op
                }

                open fun setCornerRadius(radius: Float) {
                    // No-op
                }

                open fun setCornerRadius(
                    cornerRadiusTL: Float,
                    cornerRadiusTR: Float,
                    cornerRadiusBL: Float,
                    cornerRadiusBR: Float
                ) {
                    // No-op
                }

                @RequiresApi(Build.VERSION_CODES.S)
                class BlurBackgroundDelegate31 internal constructor(
                    override val view: android.view.View,
                    private val blurColour: Int,
                    disabledColour: Int
                ): BlurBackgroundDelegate(view, disabledColour) {

                    private val blurDrawable by lazy {
                        view.rootView.getViewRootImpl()
                            ?.createBackgroundBlurDrawable()
                            ?.let { SmartspacerBackgroundBlurDrawable(it) }
                            ?.also { it.setColor(blurColour) }
                    }

                    override fun setBlurEnabled(enabled: Boolean) {
                        if (enabled && blurDrawable != null) {
                            view.background = blurDrawable
                        } else {
                            super.setBlurEnabled(enabled)
                        }
                    }

                    override fun setBlurRadius(radius: Int) {
                        blurDrawable?.setBlurRadius(radius)
                    }

                    override fun setCornerRadius(radius: Float) {
                        blurDrawable?.setCornerRadius(radius)
                    }

                    override fun setCornerRadius(
                        cornerRadiusTL: Float,
                        cornerRadiusTR: Float,
                        cornerRadiusBL: Float,
                        cornerRadiusBR: Float
                    ) {
                        blurDrawable?.setCornerRadius(
                            cornerRadiusTL,
                            cornerRadiusTR,
                            cornerRadiusBL,
                            cornerRadiusBR
                        )
                    }
                }
            }

        }

        data class None(
            override val view: android.view.View,
            override val window: android.view.Window,
            override val context: Context
        ): BlurMode()

        abstract val view: android.view.View
        abstract val window: android.view.Window
        abstract val context: Context
    }

}