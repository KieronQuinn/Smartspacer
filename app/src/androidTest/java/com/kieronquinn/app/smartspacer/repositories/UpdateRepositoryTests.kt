package com.kieronquinn.app.smartspacer.repositories

import com.kieronquinn.app.smartspacer.model.update.Release
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateRepositoryTests: BaseTest<UpdateRepository>() {

    companion object {
        private fun getMockRelease(): Release {
            return Release(
                tag = "1.0",
                versionName = "1.0",
                downloadUrl = "https://github.com/KieronQuinn/Smartspacer/releases/download/1.0/Smartspacer-v1.0.apk",
                fileName = "Smartspacer-v1.0.apk",
                gitHubUrl = "https://github.com/KieronQuinn/Smartspacer/releases/tag/1.0",
                body = "- Changelog"
            )
        }
    }

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()

    override val sut by lazy {
        UpdateRepositoryImpl(settingsRepositoryMock)
    }

    @Test
    fun testGetUpdateNotEnabled() = runTest {
        every {
            settingsRepositoryMock.updateCheckEnabled
        } returns mockSmartspacerSetting(false)
        val update = sut.getUpdate("")
        assertTrue(update == null)
    }

    @Test
    fun testGetUpdateUpdateAvailable() = runTest {
        UpdateRepositoryImpl.BASE_URL = "https://kieronquinn.co.uk/smartspacer/"
        every {
            settingsRepositoryMock.updateCheckEnabled
        } returns mockSmartspacerSetting(true)
        val update = sut.getUpdate("")
        assertTrue(update == getMockRelease())
    }

    @Test
    fun testGetUpdateNoUpdateAvailable() = runTest {
        UpdateRepositoryImpl.BASE_URL = "https://kieronquinn.co.uk/smartspacer/"
        every {
            settingsRepositoryMock.updateCheckEnabled
        } returns mockSmartspacerSetting(true)
        val update = sut.getUpdate("1.0")
        assertTrue(update == null)
    }

}