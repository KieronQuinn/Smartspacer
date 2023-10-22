package com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.name

import androidx.lifecycle.ViewModel

abstract class GeofenceRequirementConfigurationNameViewModel: ViewModel() {

    abstract fun setInitialName(name: String)
    abstract fun setName(name: String)
    abstract fun getName(): String

}

class GeofenceRequirementConfigurationNameViewModelImpl: GeofenceRequirementConfigurationNameViewModel() {

    private var name: String? = null

    override fun setInitialName(name: String) {
        if(this.name == null){
            this.name = name
        }
    }

    override fun setName(name: String) {
        this.name = name
    }

    override fun getName(): String {
        return name ?: ""
    }

}