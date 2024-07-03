package com.kieronquinn.app.smartspacer.sdk.client.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.SMARTSPACER_PACKAGE_NAME
import com.kieronquinn.app.smartspacer.sdk.client.R
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient
import com.kieronquinn.app.smartspacer.sdk.client.helper.SmartspacerHelper
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.sdk.client.utils.repeatOnAttached
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.client.views.popup.BalloonPopupFactory
import com.kieronquinn.app.smartspacer.sdk.client.views.popup.Popup
import com.kieronquinn.app.smartspacer.sdk.client.views.popup.PopupFactory
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.shouldExcludeFromSmartspacer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

open class BcSmartspaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), SmartspaceTargetInteractionListener {

    open val config = SmartspaceConfig(
        5, UiSurface.HOMESCREEN, context.packageName
    )

    private val client = SmartspacerClient.getInstance(context)

    private val provider by lazy {
        SmartspacerHelper(client, config)
    }

    private lateinit var viewPager: ViewPager
    private lateinit var indicator: PageIndicator
    private val adapter = CardPagerAdapter(this)
    private var scrollState = ViewPager.SCROLL_STATE_IDLE
    private var pendingTargets: List<SmartspaceTarget>? = null
    private var runningAnimation: Animator? = null
    private var isResumed = false
    private var popup: Popup? = null

    //Defaults to Balloon factory, can be replaced if required
    var popupFactory: PopupFactory = BalloonPopupFactory

    override fun onFinishInflate() {
        super.onFinishInflate()
        viewPager = findViewById(R.id.smartspace_card_pager)
        viewPager.isSaveEnabled = false
        indicator = findViewById(R.id.smartspace_page_indicator)

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                indicator.setPageOffset(position, positionOffset)
                popup?.dismiss()
                popup = null
            }

            override fun onPageSelected(position: Int) {
                //No-op
            }

            override fun onPageScrollStateChanged(state: Int) {
                scrollState = state
                if (state == 0) {
                    pendingTargets?.let {
                        pendingTargets = null
                        onSmartspaceTargetsUpdate(it)
                    }
                }
            }
        })

        onResume()

        val targets = provider.targets
        repeatOnAttached {
            viewPager.adapter = adapter
            targets.onEach(::onSmartspaceTargetsUpdate)
                .launchIn(this)
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if(visibility == View.VISIBLE){
            onResume()
        }else{
            onPause()
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if(isVisible){
            onResume()
        }else{
            onPause()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if(hasWindowFocus){
            onResume()
        }else{
            onPause()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        provider.onCreate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        provider.onDestroy()
    }

    private fun onPause() {
        if(!isResumed) return
        isResumed = false
        provider.onPause()
    }

    private fun onResume() {
        if(isResumed) return
        isResumed = true
        provider.onResume()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val smartspaceHeight =
            context.resources.getDimensionPixelSize(R.dimen.smartspace_height)
        if (height <= 0 || height >= smartspaceHeight) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            scaleX = 1f
            scaleY = 1f
            return
        }

        val scale = height.toFloat() / smartspaceHeight.toFloat()
        val width = (MeasureSpec.getSize(widthMeasureSpec).toFloat() / scale).roundToInt()
        super.onMeasure(
            makeMeasureSpec(width, EXACTLY),
            makeMeasureSpec(smartspaceHeight, EXACTLY)
        )
        scaleX = scale
        scaleY = scale
        pivotX = 0f
        pivotY = smartspaceHeight.toFloat() / 2f
    }

    fun setTintColour(tintColour: Int) {
        adapter.setTintColour(tintColour)
    }

    fun setApplyShadowIfRequired(applyShadowIfRequired: Boolean) {
        adapter.setApplyShadowIfRequired(applyShadowIfRequired)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        viewPager.setOnLongClickListener(l)
    }

    open fun onSmartspaceTargetsUpdate(targets: List<SmartspaceTarget>) {
        if (adapter.count > 1 && scrollState != ViewPager.SCROLL_STATE_IDLE) {
            pendingTargets = targets
            return
        }

        val sortedTargets = targets.sortedByDescending { it.score }.toMutableList()
        val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val currentItem = viewPager.currentItem
        val index = if (isRtl) adapter.count - currentItem else currentItem
        if (isRtl) {
            sortedTargets.reverse()
        }

        val oldCard = adapter.getCardAtPosition(currentItem)
        adapter.setTargets(sortedTargets)
        val count = adapter.count
        if (isRtl) {
            viewPager.setCurrentItem((count - index).coerceIn(0 until count), false)
        }
        indicator.setNumPages(targets.size)
        oldCard?.let { animateSmartspaceUpdate(it) }
        adapter.notifyDataSetChanged()
    }

    private fun animateSmartspaceUpdate(oldCard: View) {
        if (runningAnimation != null || oldCard.parent != null) return

        val animParent = viewPager.parent as ViewGroup
        oldCard.measure(makeMeasureSpec(viewPager.width, EXACTLY), makeMeasureSpec(viewPager.height, EXACTLY))
        oldCard.layout(viewPager.left, viewPager.top, viewPager.right, viewPager.bottom)
        val shift = resources.getDimension(R.dimen.smartspace_dismiss_margin)
        val animator = AnimatorSet()
        animator.play(
            ObjectAnimator.ofFloat(
                oldCard,
                View.TRANSLATION_Y,
                0f,
                (-height).toFloat() - shift
            )
        )
        animator.play(ObjectAnimator.ofFloat(oldCard, View.ALPHA, 1f, 0f))
        animator.play(
            ObjectAnimator.ofFloat(
                viewPager,
                View.TRANSLATION_Y,
                height.toFloat() + shift,
                0f
            )
        )
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                animParent.overlay.add(oldCard)
            }

            override fun onAnimationEnd(animation: Animator) {
                animParent.overlay.remove(oldCard)
                runningAnimation = null
            }
        })
        runningAnimation = animator
        animator.start()
    }

    override fun onInteraction(target: SmartspaceTarget, actionId: String?) {
        provider.onTargetInteraction(target, actionId)
    }

    @SuppressLint("RestrictedApi")
    override fun onLongPress(target: SmartspaceTarget): Boolean {
        val current = adapter.getTargetAtPosition(viewPager.currentItem) ?: return false
        if(current != target) return false //Page has changed
        val background = context.getAttrColor(android.R.attr.colorBackground)
        val textColour = background.getContrastColor()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SMARTSPACER_PACKAGE_NAME)
        val feedbackIntent = target.baseAction?.extras
            ?.getParcelableCompat(SmartspaceAction.KEY_EXTRA_FEEDBACK_INTENT, Intent::class.java)
            ?.takeIf { !it.shouldExcludeFromSmartspacer() }
        val aboutIntent = target.baseAction?.extras
            ?.getParcelableCompat(SmartspaceAction.KEY_EXTRA_ABOUT_INTENT, Intent::class.java)
            ?.takeIf { !it.shouldExcludeFromSmartspacer() }
        val shouldShowSettings = launchIntent != null
        val shouldShowDismiss = target.featureType != SmartspaceTarget.FEATURE_WEATHER
                && target.canBeDismissed
        if(!shouldShowDismiss && !shouldShowSettings && feedbackIntent == null
            && aboutIntent == null) return false
        val dismissAction = if(shouldShowDismiss){
            ::dismissAction
        }else null
        this.popup = popupFactory.createPopup(
            context,
            this,
            current,
            background,
            textColour,
            ::launchIntent,
            dismissAction,
            aboutIntent,
            feedbackIntent,
            launchIntent
        )
        return true
    }

    private fun dismissAction(smartspaceTarget: SmartspaceTarget) {
        provider.onTargetDismiss(smartspaceTarget)
    }

    private fun launchIntent(intent: Intent?){
        if(intent == null) return
        try {
            context.startActivity(intent)
        }catch (e: Exception){
            Toast.makeText(
                context, R.string.smartspace_long_press_popup_failed_to_launch, Toast.LENGTH_LONG
            ).show()
        }
    }

    //https://stackoverflow.com/a/47104940/1088334
    private fun Int.getContrastColor(): Int {
        val a = 1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
        return if (a < 0.5) Color.BLACK else Color.WHITE
    }

}
