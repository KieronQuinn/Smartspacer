package com.kieronquinn.app.smartspacer.model.doodle

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.repositories.DoodleApi
import com.kieronquinn.app.smartspacer.utils.extensions.Exclude

data class Doodle(
    @SerializedName("ddljson")
    private val ddlJson: DDLJson
) {

    fun getDoodleImage(baseUrl: String): DoodleImage {
        return ddlJson.image.toDoodleImage(
            baseUrl,
            ddlJson.altText,
            ddlJson.searchUrl,
            ddlJson.darkImage
        )
    }

    fun getExpiryTime(): Long {
        return ddlJson.timeToLiveMs + System.currentTimeMillis()
    }

    fun fillDarkImage(doodleApi: DoodleApi): Doodle = apply {
        val darkGif = doodleApi.getEquivalentDarkGif(ddlJson.image.url) ?: return@apply
        ddlJson.darkImage = ddlJson.image.copy(url = darkGif)
    }

    /**
     *  If the image is a GIF, there may be an equivalent dark version available. Replacing the
     *  suffix to be -ladc.gif will return it if so.
     */
    private fun DoodleApi.getEquivalentDarkGif(url: String): String? {
        if(!url.endsWith("-2xa.gif")) return null
        val checkUrl = url.replace("-2xa.gif", "-ladc.gif")
        return try {
            if(getDoodle(checkUrl).execute().isSuccessful){
                checkUrl
            }else null
        }catch (e: Exception){
            null
        }
    }

}

data class DDLJson(
    @SerializedName("alt_text")
    val altText: String,
    @SerializedName("large_image")
    val image: Image,
    @SerializedName("dark_image")
    var darkImage: Image? = null,
    @SerializedName("search_url")
    val searchUrl: String,
    @Exclude
    @SerializedName("time_to_live_ms")
    val timeToLiveMs: Long
)

data class Image(
    @SerializedName("url")
    val url: String
) {

    fun toDoodleImage(
        baseUrl: String,
        altText: String?,
        searchUrl: String?,
        darkImage: Image?
    ): DoodleImage {
        return DoodleImage(
            baseUrl + url,
            darkImage?.url?.let { baseUrl + it },
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