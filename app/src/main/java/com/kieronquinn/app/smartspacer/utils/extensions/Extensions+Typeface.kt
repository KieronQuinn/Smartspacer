package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import org.koin.java.KoinJavaComponent.inject

private val googleSansText by lazy {
    val context by inject<Context>(Context::class.java)
    ResourcesCompat.getFont(context, R.font.google_sans_text)
}

private val googleSansTextMedium by lazy {
    val context by inject<Context>(Context::class.java)
    ResourcesCompat.getFont(context, R.font.google_sans_text_medium)
}

private val googleSansTextMono by lazy {
    val context by inject<Context>(Context::class.java)
    ResourcesCompat.getFont(context, R.font.google_sans_mono)
}

private val shouldInjectGoogleSans: Boolean
    get() {
        val expandedRepository by inject<ExpandedRepository>(ExpandedRepository::class.java)
        return expandedRepository.widgetUseGoogleSans
    }

fun Typeface.convertToGoogleSans(): Typeface {
    if(!shouldInjectGoogleSans) return this
    return when(this) {
        Typeface.DEFAULT if(weight == 500) -> googleSansTextMedium
        Typeface.DEFAULT -> googleSansText
        Typeface.DEFAULT_BOLD -> googleSansTextMedium
        Typeface.MONOSPACE -> googleSansTextMono
        else -> null
    } ?: this
}

private fun supportsFont(name: String): Boolean {
    return Typeface.create(name, Typeface.NORMAL) != Typeface.DEFAULT
}

val supportsNativeGoogleSansFlex by lazy {
    supportsFont("variable-title-medium-emphasized") && supportsFont("variable-title-medium")
}