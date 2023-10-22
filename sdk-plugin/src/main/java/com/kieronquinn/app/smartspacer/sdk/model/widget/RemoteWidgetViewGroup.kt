package com.kieronquinn.app.smartspacer.sdk.model.widget

import android.os.Bundle
import android.view.View
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat

abstract class RemoteWidgetViewGroup<T: View>(
    override val identifier: String?,
    private val children: List<RemoteWidgetView<*>>
): RemoteWidgetView<T> {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(bundle: Bundle): this(
        bundle.getString(RemoteWidgetView.KEY_IDENTIFIER),
        loadChildren(bundle)
    )

    companion object {
        private const val KEY_CHILDREN = "children"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun loadChildren(bundle: Bundle): List<RemoteWidgetView<*>> {
            val children = bundle.getParcelableArrayListCompat(KEY_CHILDREN, Bundle::class.java)
                ?: return emptyList()
            return children.map {
                RemoteWidgetView.fromBundle(it)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun toBundle(bundle: Bundle): Bundle {
        super.toBundle(bundle)
        val childrenBundles = children.map {
            it.toBundle()
        }.let {
            ArrayList(it)
        }
        bundle.putParcelableArrayList(KEY_CHILDREN, childrenBundles)
        return bundle
    }

}