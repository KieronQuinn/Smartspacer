package com.kieronquinn.app.smartspacer.repositories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AccessibilityRepository {

    val foregroundPackage: StateFlow<String>

    suspend fun setForegroundPackage(foregroundPackage: String)

}

class AccessibilityRepositoryImpl: AccessibilityRepository {

    override val foregroundPackage = MutableStateFlow("")

    override suspend fun setForegroundPackage(foregroundPackage: String) {
        this.foregroundPackage.emit(foregroundPackage)
    }

}