package androidx.recyclerview.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView.ItemAnimator.ItemAnimatorListener

/**
 *  Hacky workaround for "`Tmp detached view should be removed from RecyclerView before it can be
 *  recycled`" issue in [RecyclerView], uses reflection to implement a fix from a PR that's not
 *  been merged since the 8th October 2021 - come on Google!
 *
 *  nb: If you've come across this class and want to use it for your own project, go ahead. Just
 *  remember you will need to exclude RecyclerView's fields from obfuscation for it to work on
 *  release builds with Proguard enabled.
 */
open class AnimationFixedRecyclerView: RecyclerView {

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes)
    constructor(context: Context, attributeSet: AttributeSet?):
            this(context, attributeSet, 0)
    constructor(context: Context):
            this(context, null, 0)

    /**
     * Version of [RecyclerView.ItemAnimatorRestoreListener] with these changes applied:
     * https://github.com/androidx/androidx/pull/252/files#diff-7ce233edce4e999e05f71d3e7ec147ca007785d7238dc226cf34b5da50df54b8L13257
     */
    private inner class ItemAnimatorRestoreListener: ItemAnimatorListener {
        override fun onAnimationFinished(item: ViewHolder) {
            item.setIsRecyclable(true)
            if (item.mShadowedHolder != null && item.mShadowingHolder == null) { // old vh
                item.mShadowedHolder = null
            }
            // always null this because an OldViewHolder can never become NewViewHolder w/o being
            // recycled.
            item.mShadowingHolder = null
            if (!item.shouldBeKeptAsChild()) {
                if (item.isTmpDetached) {
                    removeDetachedView(item.itemView, false)
                    return
                }
                try {
                    if (!removeAnimatingView(item.itemView)) {
                        removeDetachedView(item.itemView, false)
                    }
                }catch (e: IllegalArgumentException) {
                    //Absorb
                }
            }
        }
    }

    private val mItemAnimatorListenerField = RecyclerView::class.java
        .getDeclaredField("mItemAnimatorListener").apply { isAccessible = true }

    private var mItemAnimatorListener: ItemAnimatorListener
        get() = mItemAnimatorListenerField.get(this) as ItemAnimatorListener
        set(value) = mItemAnimatorListenerField.set(this, value)

    init {
        mItemAnimatorListener = ItemAnimatorRestoreListener()
    }

}