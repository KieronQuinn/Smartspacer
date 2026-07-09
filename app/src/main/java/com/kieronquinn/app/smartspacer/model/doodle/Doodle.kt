package com.kieronquinn.app.smartspacer.model.doodle

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.repositories.DoodleApi
import com.kieronquinn.app.smartspacer.utils.extensions.Exclude

data class Doodle(
    @SerializedName("ddljson")
    private val ddlJson: DDLJson
) {

    fun getDoodleImage(baseUrl: String): DoodleImage? {
        val image = ddlJson.image ?: return null
        return image.toDoodleImage(
            baseUrl,
            ddlJson.altText,
            ddlJson.searchUrl,
            ddlJson.darkImage
        )
    }

    fun getExpiryTime(): Long {
        return ddlJson.timeToLiveMs + System.currentTimeMillis()
    }

    fun fillDarkImage(doodleApi: DoodleApi, baseUrl: String): Doodle = apply {
        // If dark_large_image was already parsed from the JSON, nothing to do.
        if (ddlJson.darkImage != null) return@apply
        val image = ddlJson.image ?: return@apply
        val darkGifUrl = doodleApi.getEquivalentDarkGif(image, baseUrl) ?: return@apply
        ddlJson.darkImage = image.copy(url = darkGifUrl, alternateUrl = null)
    }

    /**
     *  If the image is a GIF, there may be an equivalent dark version available. Replacing the
     *  suffix to be -ladc.gif will return it if so.
     */
    private fun DoodleApi.getEquivalentDarkGif(image: Image, baseUrl: String): String? {
        if (!image.url.endsWith("-2xa.gif")) return null
        val checkRelUrl = image.url.replace("-2xa.gif", "-ladc.gif")
        val checkUrl = image.alternateUrl?.replace(image.url, checkRelUrl) ?: (baseUrl + checkRelUrl)
        return try {
            if (getDoodle(checkUrl).execute().isSuccessful) checkRelUrl else null
        } catch (e: Exception) {
            null
        }
    }

}

data class DDLJson(
    @SerializedName("alt_text")
    val altText: String,
    @SerializedName("large_image")
    val image: Image,
    @SerializedName("dark_large_image")
    var darkImage: Image? = null,
    @SerializedName("search_url")
    val searchUrl: String,
    @Exclude
    @SerializedName("time_to_live_ms")
    val timeToLiveMs: Long
)

data class Image(
    @SerializedName("url")
    val url: String,
    @SerializedName("alternate_url")
    val alternateUrl: String? = null
) {

    /** Returns an absolute URL, preferring the CDN alternate if available. */
    fun absoluteUrl(baseUrl: String): String = alternateUrl ?: (baseUrl + url)

    fun toDoodleImage(
        baseUrl: String,
        altText: String?,
        searchUrl: String?,
        darkImage: Image?
    ): DoodleImage {
        return DoodleImage(
            absoluteUrl(baseUrl),
            darkImage?.absoluteUrl(baseUrl),
            baseUrl + searchUrl,
            altText
        )
    }

}

data class DoodleImage(
    val url: String,
    var darkUrl: String?,
    val searchUrl: String?,
    val altText: String?,
    val padding: Int = 16
) {

    companion object {
        val DEFAULT = DoodleImage(
            "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png",
            null,
            null,
            null,
            24
        )
    }

}