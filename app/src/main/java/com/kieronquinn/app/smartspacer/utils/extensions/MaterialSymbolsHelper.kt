package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.graphics.Typeface
import com.kieronquinn.app.smartspacer.R

/**
 * Loads and caches the Material Symbols Outlined font + icon list.
 *
 * Font: assets/fonts/material_symbols_outlined.ttf (bundled, ~925 KB)
 * Codepoints: res/raw/material_symbols_codepoints.txt (4211 icons from Google)
 */
object MaterialSymbolsHelper {

    data class Symbol(val name: String, val codepoint: Int) {
        /** Unicode character string to display with the Material Symbols font. */
        val char: String get() = String(Character.toChars(codepoint))

        /** Human-readable name: "arrow_back" → "Arrow Back" */
        val displayName: String
            get() = name.replace('_', ' ')
                .split(' ')
                .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    @Volatile private var typeface: Typeface? = null
    @Volatile private var symbols: List<Symbol>? = null

    fun getTypeface(context: Context): Typeface =
        typeface ?: Typeface.createFromAsset(
            context.applicationContext.assets,
            "fonts/material_symbols_outlined.ttf"
        ).also { typeface = it }

    fun getSymbols(context: Context): List<Symbol> =
        symbols ?: loadSymbols(context).also { symbols = it }

    /** Returns icons whose name contains [query] (case-insensitive). Full list if query is blank. */
    fun search(context: Context, query: String): List<Symbol> {
        val all = getSymbols(context)
        if (query.isBlank()) return all
        val lower = query.lowercase().trim()
        return all.filter { it.name.contains(lower) }
    }

    private fun loadSymbols(context: Context): List<Symbol> =
        context.applicationContext.resources
            .openRawResource(R.raw.material_symbols_codepoints)
            .bufferedReader()
            .useLines { lines ->
                lines.mapNotNull { line ->
                    val parts = line.trim().split(' ')
                    if (parts.size == 2) {
                        val cp = parts[1].toIntOrNull(16) ?: return@mapNotNull null
                        Symbol(parts[0], cp)
                    } else null
                }.toList()
            }
}
