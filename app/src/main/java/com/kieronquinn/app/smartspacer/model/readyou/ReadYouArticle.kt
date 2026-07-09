package com.kieronquinn.app.smartspacer.model.readyou

data class ReadYouArticle(
    val id: String,
    val feedName: String,
    val title: String,
    val publishedDate: Long? = null,
    val feedId: String? = null,
    val isRead: Boolean = false
)
