package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.net.Uri
import com.kieronquinn.app.smartspacer.model.readyou.ReadYouArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ReadYouRepository {
    companion object {
        /** Content Provider authority declared in Read You's AndroidManifest. */
        const val AUTHORITY = "me.ash.reader.widget.articles"
    }

    /**
     * Queries Read You's Content Provider for the feed configured for [appWidgetId].
     * Returns at most [MAX_ARTICLES] articles, or an empty list if Read You is not
     * installed or the Content Provider is unavailable.
     */
    suspend fun getArticles(appWidgetId: Int): List<ReadYouArticle>

    /** Mark an article as read (dimmed in the list). Persists across process restarts. */
    fun markArticleRead(id: String)

    /** Returns true if [id] has been marked read. */
    fun isArticleRead(id: String): Boolean

    /**
     * Returns the widget-level filter IDs for [appWidgetId] from the last [getArticles] call.
     * First = filter_feed_id, second = filter_group_id. Both null if unconfigured or not yet fetched.
     */
    fun getFilterIds(appWidgetId: Int): Pair<String?, String?>
}

class ReadYouRepositoryImpl(private val context: Context) : ReadYouRepository {

    companion object {
        const val COLUMN_ID             = "id"
        const val COLUMN_FEED_NAME      = "feed_name"
        const val COLUMN_TITLE          = "title"
        const val COLUMN_DATE           = "date"   // epoch milliseconds (java.util.Date.time)
        const val COLUMN_FEED_ID         = "feed_id"
        const val COLUMN_FILTER_FEED_ID  = "filter_feed_id"
        const val COLUMN_FILTER_GROUP_ID = "filter_group_id"
        const val COLUMN_IS_READ         = "is_read"

        const val MAX_ARTICLES = 15

    }

    private val readArticleIds = mutableSetOf<String>()

    override fun markArticleRead(id: String) {
        readArticleIds.add(id)
    }

    override fun isArticleRead(id: String) = id in readArticleIds

    // Widget-level filter IDs cached from the last getArticles call, keyed by appWidgetId.
    private val filterIdsCache = mutableMapOf<Int, Pair<String?, String?>>()

    override fun getFilterIds(appWidgetId: Int): Pair<String?, String?> =
        filterIdsCache[appWidgetId] ?: Pair(null, null)

    override suspend fun getArticles(appWidgetId: Int): List<ReadYouArticle> =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse("content://${ReadYouRepository.AUTHORITY}/$appWidgetId")
                val cursor = context.contentResolver.query(
                    uri, null, null, null, null
                ) ?: return@withContext emptyList()

                cursor.use {
                    val idCol           = it.getColumnIndex(COLUMN_ID)
                    val feedCol         = it.getColumnIndex(COLUMN_FEED_NAME)
                    val titleCol        = it.getColumnIndex(COLUMN_TITLE)
                    val dateCol         = it.getColumnIndex(COLUMN_DATE)
                    val feedIdCol       = it.getColumnIndex(COLUMN_FEED_ID)
                    val filterFeedCol   = it.getColumnIndex(COLUMN_FILTER_FEED_ID)
                    val filterGroupCol  = it.getColumnIndex(COLUMN_FILTER_GROUP_ID)
                    val isReadCol       = it.getColumnIndex(COLUMN_IS_READ)

                    val articles = mutableListOf<ReadYouArticle>()
                    var filterFeedId: String? = null
                    var filterGroupId: String? = null

                    while (it.moveToNext() && articles.size < MAX_ARTICLES) {
                        // Widget-level columns are identical on every row — read once.
                        if (articles.isEmpty()) {
                            filterFeedId  = if (filterFeedCol  >= 0) it.getString(filterFeedCol)  else null
                            filterGroupId = if (filterGroupCol >= 0) it.getString(filterGroupCol) else null
                        }
                        articles.add(
                            ReadYouArticle(
                                id            = if (idCol      >= 0) it.getString(idCol)              else "",
                                feedName      = if (feedCol    >= 0) it.getString(feedCol)             else "",
                                title         = if (titleCol   >= 0) it.getString(titleCol)            else "",
                                publishedDate = if (dateCol    >= 0) it.getLong(dateCol).takeIf { v -> v > 0 } else null,
                                feedId        = if (feedIdCol  >= 0) it.getString(feedIdCol)           else null,
                                isRead        = if (isReadCol  >= 0) it.getInt(isReadCol) != 0         else false
                            )
                        )
                    }

                    filterIdsCache[appWidgetId] = Pair(filterFeedId, filterGroupId)
                    articles
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
}
