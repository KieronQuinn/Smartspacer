package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kieronquinn.app.smartspacer.model.expanded.ExpandedTabConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ExpandedTabRepository {

    /** Live list of configured tabs, updated whenever [saveTabs] is called. */
    val tabs: StateFlow<List<ExpandedTabConfig>>

    /** Returns the current list of tabs from SharedPreferences. */
    fun getTabs(): List<ExpandedTabConfig>

    /** Persists [tabs] to SharedPreferences and emits the new list. */
    fun saveTabs(tabs: List<ExpandedTabConfig>)
}

class ExpandedTabRepositoryImpl(
    context: Context,
    private val gson: Gson
) : ExpandedTabRepository {

    companion object {
        private const val PREFS_NAME = "expanded_tab_config"
        private const val KEY_TABS = "tabs"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _tabs = MutableStateFlow(loadFromPrefs())
    override val tabs: StateFlow<List<ExpandedTabConfig>> = _tabs.asStateFlow()

    override fun getTabs(): List<ExpandedTabConfig> = _tabs.value

    override fun saveTabs(tabs: List<ExpandedTabConfig>) {
        val json = gson.toJson(tabs)
        prefs.edit().putString(KEY_TABS, json).apply()
        _tabs.value = tabs
    }

    private fun loadFromPrefs(): List<ExpandedTabConfig> {
        val json = prefs.getString(KEY_TABS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ExpandedTabConfig>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
