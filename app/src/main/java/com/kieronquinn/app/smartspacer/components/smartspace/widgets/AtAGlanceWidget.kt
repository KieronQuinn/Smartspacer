package com.kieronquinn.app.smartspacer.components.smartspace.widgets

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.targets.AtAGlanceTarget
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository.State
import com.kieronquinn.app.smartspacer.sdk.model.RemoteAdapterItem
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.RemoteAdapter
import com.kieronquinn.app.smartspacer.sdk.utils.findViewByIdentifier
import com.kieronquinn.app.smartspacer.sdk.utils.findViewsByType
import com.kieronquinn.app.smartspacer.sdk.utils.getClickPendingIntent
import org.koin.android.ext.android.inject

class AtAGlanceWidget : GlanceWidget() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.widget.ataglance"

        private const val IDENTIFIER_ICON = "$PACKAGE_NAME:id/assistant_subtitle_icon"
        private const val IDENTIFIER_ICON_ALT = "$PACKAGE_NAME:id/subtitle_icon"
        private const val IDENTIFIER_SUBTITLE = "$PACKAGE_NAME:id/assistant_subtitle_text"
        private const val IDENTIFIER_SUBTITLE_ALT = "$PACKAGE_NAME:id/subtitle_text"
        private const val IDENTIFIER_CONTENT = "$PACKAGE_NAME:id/assistant_smartspace_content"
        private const val IDENTIFIER_CONTENT_ALT = "$PACKAGE_NAME:id/smartspace_content"

        /**
         *  Legacy At a Glance has three possible title texts, each with different formatting. We
         *  don't care about the formatting, so need to load the best one available to us
         */
        private val TITLE_IDS = arrayOf(
            "assistant_title_fixed_text",
            "assistant_title_trailing_truncatable",
            "assistant_title_leading_truncatable"
        )

        private val TITLE_IDS_ALT = arrayOf(
            "title_fixed_text",
            "title_trailing_truncatable",
            "title_leading_truncatable"
        )
    }

    private val atAGlance by inject<AtAGlanceRepository>()

    override fun View.loadLegacy(): Boolean {
        val state = loadLegacyAssistant().ifEmpty { loadLegacyNoAssistant() }
        atAGlance.setStates(state)
        return true
    }

    private fun View.loadLegacyAssistant(): List<State> {
        val content = findViewByIdentifier<View>(IDENTIFIER_CONTENT)
        val title = TITLE_IDS.firstNotNullOfOrNull {
            val text = findViewByIdentifier<TextView>("$PACKAGE_NAME:id/$it")?.text
            if (text.isNullOrEmpty()) null else text
        } ?: run {
            return emptyList()
        }
        val subtitleText = findViewByIdentifier<TextView>(IDENTIFIER_SUBTITLE)?.text
            ?: return emptyList()
        val subtitleIcon = findViewByIdentifier<ImageView>(IDENTIFIER_ICON) ?: return emptyList()
        val icon = subtitleIcon.drawable?.toBitmap() ?: return emptyList()
        return listOf(
            State(
                title,
                subtitleText,
                icon,
                subtitleIcon.contentDescription,
                clickPendingIntent = content?.getClickPendingIntent()
            )
        )
    }

    private fun View.loadLegacyNoAssistant(): List<State> {
        val content = findViewByIdentifier<View>(IDENTIFIER_CONTENT_ALT)
        val title = TITLE_IDS_ALT.firstNotNullOfOrNull {
            val text = findViewByIdentifier<TextView>("$PACKAGE_NAME:id/$it")?.text
            if (text.isNullOrEmpty()) null else text
        } ?: run {
            return emptyList()
        }
        val subtitleText = findViewByIdentifier<TextView>(IDENTIFIER_SUBTITLE_ALT)?.text
            ?: return emptyList()
        val subtitleIcon = findViewByIdentifier<ImageView>(IDENTIFIER_ICON_ALT)
            ?: return emptyList()
        val icon = subtitleIcon.drawable?.toBitmap() ?: return emptyList()
        return listOf(
            State(
                title,
                subtitleText,
                icon,
                subtitleIcon.contentDescription,
                clickPendingIntent = content?.getClickPendingIntent()
            )
        )
    }

    override fun View.loadTNG(smartspacerId: String): Boolean {
        this as ViewGroup
        val listView = findViewsByType(ListView::class.java).firstOrNull()
        return if(listView != null){
            val listViewId = listView.id
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getRemoteCollectionItems(smartspacerId, listViewId)
            }else{
                getAdapter(smartspacerId, listViewId)
            }
            false
        }else{
            val state = getStateFromFlatView(false)
            atAGlance.setStates(listOfNotNull(state))
            notifyChange(smartspacerId)
            true
        }
    }

    override fun onAdapterConnected(smartspacerId: String, adapter: RemoteAdapter) {
        super.onAdapterConnected(smartspacerId, adapter)
        val count = adapter.getCount()
        val adapterItems = ArrayList<RemoteAdapterItem>()
        for(i in 0 until count) {
            adapterItems.add(adapter.getViewAt(i) ?: continue)
        }
        val states = adapterItems.mapNotNull {
            val intent = it.onClickResponses.firstOrNull()?.response?.fillInIntent
            val optionsIntent = it.onClickResponses.lastOrNull()?.response?.fillInIntent
            val views = it.remoteViews.load() as ViewGroup
            views.getStateFromFlatView(true)?.copy(
                clickIntent = intent,
                clickPendingIntent = adapter.adapterViewPendingIntent,
                optionsIntent = optionsIntent
            )
        }
        atAGlance.setStates(states)
        notifyChange(smartspacerId)
    }

    private fun ViewGroup.getStateFromFlatView(isFromList: Boolean): State? {
        val textViews = findViewsByType(TextView::class.java).filter {
            it.text.isNotBlank()
        }
        val imageViews = findViewsByType(ImageView::class.java).filter {
            it.drawable is BitmapDrawable
        }
        val clickable = findViewsByType(View::class.java).firstOrNull {
            it.isClickable
        }
        val requiredTextViews = if(isFromList) 2 else 3
        val requiredImageViews = if(isFromList) 1 else 2
        return if(textViews.size == requiredTextViews && imageViews.size == requiredImageViews) {
            val title = textViews[0].text
            val subtitle = textViews[1].text
            State(
                title,
                subtitle,
                imageViews[0].drawable.toBitmap(),
                imageViews[0].contentDescription,
                clickPendingIntent = clickable?.getClickPendingIntent()
            )
        }else null
    }

    /**
     *  The only icons which have content description in the widget are weather related and should
     *  not be tinted
     */
    private fun ImageView.isTinted(): Boolean {
        return !contentDescription.isNullOrEmpty()
    }

    override fun notifyChange(smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(
            provideContext(), AtAGlanceTarget::class.java, smartspacerId
        )
    }

}