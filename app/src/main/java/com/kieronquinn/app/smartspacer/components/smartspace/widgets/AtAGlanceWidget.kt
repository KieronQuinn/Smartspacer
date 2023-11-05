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
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.RemoteAdapter
import com.kieronquinn.app.smartspacer.sdk.utils.dumpToLog
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
        val state = loadLegacyAssistant() ?: loadLegacyNoAssistant()
        atAGlance.setState(state)
        return true
    }

    private fun View.loadLegacyAssistant(): State? {
        val content = findViewByIdentifier<View>(IDENTIFIER_CONTENT)
        val title = TITLE_IDS.firstNotNullOfOrNull {
            val text = findViewByIdentifier<TextView>("$PACKAGE_NAME:id/$it")?.text
            if (text.isNullOrEmpty()) null else text
        } ?: run {
            return null
        }
        val subtitleText = findViewByIdentifier<TextView>(IDENTIFIER_SUBTITLE)?.text
            ?: return null
        val subtitleIcon = findViewByIdentifier<ImageView>(IDENTIFIER_ICON) ?: return null
        val icon = subtitleIcon.drawable?.toBitmap() ?: return null
        return State(
            title,
            subtitleText,
            icon,
            clickPendingIntent = content?.getClickPendingIntent()
        )
    }

    private fun View.loadLegacyNoAssistant(): State? {
        val content = findViewByIdentifier<View>(IDENTIFIER_CONTENT_ALT)
        val title = TITLE_IDS_ALT.firstNotNullOfOrNull {
            val text = findViewByIdentifier<TextView>("$PACKAGE_NAME:id/$it")?.text
            if (text.isNullOrEmpty()) null else text
        } ?: run {
            return null
        }
        val subtitleText = findViewByIdentifier<TextView>(IDENTIFIER_SUBTITLE_ALT)?.text
            ?: return null
        val subtitleIcon = findViewByIdentifier<ImageView>(IDENTIFIER_ICON_ALT) ?: return null
        val icon = subtitleIcon.drawable?.toBitmap() ?: return null
        return State(
            title,
            subtitleText,
            icon,
            clickPendingIntent = content?.getClickPendingIntent()
        )
    }

    override fun View.loadTNG(smartspacerId: String): Boolean {
        this as ViewGroup
        dumpToLog("Views")
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
            atAGlance.setState(state)
            notifyChange(smartspacerId)
            true
        }
    }

    override fun onAdapterConnected(smartspacerId: String, adapter: RemoteAdapter) {
        super.onAdapterConnected(smartspacerId, adapter)
        val adapterItem = adapter.getViewAt(0) ?: return
        val intent = adapterItem.onClickResponses.firstOrNull()?.response?.fillInIntent
        val optionsIntent = adapterItem.onClickResponses.lastOrNull()?.response?.fillInIntent
        val views = adapterItem.remoteViews.load() as ViewGroup
        val state = views.getStateFromFlatView(true)?.copy(
            clickIntent = intent,
            clickPendingIntent = adapter.adapterViewPendingIntent,
            optionsIntent = optionsIntent
        )
        atAGlance.setState(state)
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
                clickPendingIntent = clickable?.getClickPendingIntent()
            )
        }else null
    }

    override fun notifyChange(smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(
            provideContext(), AtAGlanceTarget::class.java, smartspacerId
        )
    }

}