package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import com.kieronquinn.app.smartspacer.databinding.ItemReadYouArticleBinding
import com.kieronquinn.app.smartspacer.model.readyou.ReadYouArticle
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReadYouArticleAdapter(
    private var articles: List<ReadYouArticle>,
    private val isRead: (String) -> Boolean,
    private val onMarkRead: (String) -> Unit,
    /** Background colour applied to every card. 0 = use the XML default (?attr/colorSurfaceContainer). */
    private val cardBackgroundColor: Int = 0,
    private val onArticleClick: (ReadYouArticle) -> Unit
) : RecyclerView.Adapter<ReadYouArticleAdapter.ViewHolder>() {

    fun updateArticles(newArticles: List<ReadYouArticle>, clearRead: Boolean = false) {
        if (clearRead) {
            // Pull-to-refresh: drop read articles, show only what the provider returns.
            articles = newArticles.filterNot { isRead(it.id) }
        } else {
            // Background refresh: keep read articles in place (greyed out) so the list
            // stays stable. Merge provider results with locally-read ones.
            val newById = newArticles.associateBy { it.id }
            val oldIds  = articles.map { it.id }.toSet()
            val merged  = articles.mapNotNull { old ->
                newById[old.id] ?: if (isRead(old.id)) old else null
            }
            val brandNew = newArticles.filter { it.id !in oldIds && !isRead(it.id) }
            articles = brandNew + merged
        }
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemReadYouArticleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemReadYouArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = articles.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]
        // Dim text of read articles: 50% dark / 40% light.
        val isNightMode = (holder.binding.root.context.resources.configuration.uiMode
            and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val readAlpha = if (isNightMode) 0.5f else 0.4f
        val textAlpha = if (isRead(article.id)) readAlpha else 1.0f
        holder.binding.readYouArticleFeedName.alpha = textAlpha
        holder.binding.readYouArticleTime.alpha     = textAlpha
        holder.binding.readYouArticleTitle.alpha    = textAlpha

        holder.binding.root.setOnClickListener {
            if (!isRead(article.id)) {
                onMarkRead(article.id)
                holder.binding.readYouArticleFeedName.alpha = readAlpha
                holder.binding.readYouArticleTime.alpha     = readAlpha
                holder.binding.readYouArticleTitle.alpha    = readAlpha
            }
            onArticleClick(article)
        }
        with(holder.binding) {
            readYouArticleFeedName.text = article.feedName
            readYouArticleTitle.text = article.title

            val dateMs = article.publishedDate
            if (dateMs != null) {
                readYouArticleTime.text = formatDate(dateMs)
                readYouArticleTime.isVisible = true
            } else {
                readYouArticleTime.isVisible = false
            }

            val isFirst = position == 0
            val isLast = position == articles.lastIndex
            val topRadius = (if (isFirst) 32 else 8).dp.toFloat()
            val bottomRadius = (if (isLast) 32 else 8).dp.toFloat()
            val card = root as MaterialCardView
            card.shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(topRadius)
                .setTopRightCornerSize(topRadius)
                .setBottomLeftCornerSize(bottomRadius)
                .setBottomRightCornerSize(bottomRadius)
                .build()
            if (cardBackgroundColor != 0) card.setCardBackgroundColor(cardBackgroundColor)
        }
    }

    private fun formatDate(epochMs: Long): String {
        val now = System.currentTimeMillis()
        return if (now - epochMs < TimeUnit.HOURS.toMillis(24)) {
            SimpleDateFormat("h:mma", Locale.getDefault()).format(Date(epochMs)).lowercase()
        } else {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
        }
    }

    /** Adds a 4dp gap above every card except the first. */
    class ItemDecoration(private val gap: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
        ) {
            if (parent.getChildAdapterPosition(view) > 0) outRect.top = gap
        }
    }
}
