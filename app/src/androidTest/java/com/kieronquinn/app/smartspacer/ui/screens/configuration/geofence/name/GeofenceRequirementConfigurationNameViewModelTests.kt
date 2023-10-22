package com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence.name

import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomString
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GeofenceRequirementConfigurationNameViewModelTests: BaseTest<GeofenceRequirementConfigurationNameViewModel>() {

    override val sut by lazy {
        GeofenceRequirementConfigurationNameViewModelImpl()
    }

    @Test
    fun testName() = runTest {
        val initialName = randomString()
        sut.setInitialName(initialName)
        assertTrue(sut.getName() == initialName)
        val updatedName = randomString()
        sut.setName(updatedName)
        assertTrue(sut.getName() == updatedName)
    }

}