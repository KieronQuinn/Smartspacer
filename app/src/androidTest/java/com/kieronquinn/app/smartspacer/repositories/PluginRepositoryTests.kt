package com.kieronquinn.app.smartspacer.repositories

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.repositories.PluginApi.RemotePluginInfo
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Test

class PluginRepositoryTests: BaseTest<PluginRepository>() {

    companion object {
        //Mock plugin repository which returns fake data and is not impacted by live
        private const val MOCK_URL =
            "https://kieronquinn.co.uk/smartspacer/plugins/mock/plugins.json"

        private val MOCK_VERSION_CODES = mapOf(
            "com.example.one" to 1L,
            "com.example.two" to 1L, //Should show as update available
            "com.example.four" to 2L
        )

        private fun getMockContentProviders(): List<ResolveInfo> {
            return listOf(
                getMockResolveInfo("com.example.one"),
                getMockResolveInfo("com.example.two"),
                getMockResolveInfo("com.example.four") //Mock not available on repo
            )
        }

        private fun getMockResolveInfo(packageName: String): ResolveInfo {
            return ResolveInfo().apply {
                providerInfo = ProviderInfo().apply {
                    this.packageName = packageName
                }
            }
        }

        //Should match https://kieronquinn.co.uk/smartspacer/plugins/mock/plugins.json + locals
        private fun getMockPlugins(): List<Plugin> {
            return listOf(
                Plugin.Remote(
                    isInstalled = true,
                    packageName = "com.example.one",
                    name = "Example One",
                    description = "Description One",
                    url = "https://kieronquinn.co.uk/smartspacer/plugins/mock/1/plugin.json",
                    author = "Author One",
                    supportedPackages = listOf("com.example.one.core"),
                    updateAvailable = false,
                    recommendedApps = emptyList()
                ),
                Plugin.Remote(
                    isInstalled = true,
                    packageName = "com.example.two",
                    name = "Example Two",
                    description = "Description Two",
                    url = "https://kieronquinn.co.uk/smartspacer/plugins/mock/2/plugin.json",
                    author = "Author Two",
                    supportedPackages = listOf("com.example.two.core"),
                    updateAvailable = true,
                    recommendedApps = emptyList()
                ),
                Plugin.Remote(
                    isInstalled = false,
                    packageName = "com.example.three",
                    name = "Example Three",
                    description = "Description Three",
                    url = "https://kieronquinn.co.uk/smartspacer/plugins/mock/3",
                    author = "Author Three",
                    supportedPackages = listOf("com.example.three.core"),
                    updateAvailable = false,
                    recommendedApps = emptyList()
                ),
                Plugin.Local(
                    packageName = "com.example.four",
                    name = "Label",
                    launchIntent = Intent()
                )
            )
        }
    }

    private val packageRepositoryMock = mock<PackageRepository> {
        every { onPackageChanged } answers { packageChanged }
    }

    private var repositoryEnabled = mockSmartspacerSetting(true)

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { pluginRepositoryUrl } returns mockSmartspacerSetting(MOCK_URL)
        every { pluginRepositoryEnabled } returns repositoryEnabled
    }

    private val okHttpClient = OkHttpClient().newBuilder().build()
    private val packageChanged = MutableSharedFlow<String>()

    override val sut by lazy {
        PluginRepositoryImpl(
            actualContext,
            packageRepositoryMock,
            okHttpClient,
            settingsRepositoryMock,
            scope,
            packageManagerMock
        )
    }

    override fun setup() {
        super.setup()
        every {
            packageManagerMock.queryIntentContentProviders(any(), any<ResolveInfoFlags>())
        } returns getMockContentProviders()
        every {
            packageManagerMock.getPackageInfo(any<String>(), any<PackageInfoFlags>())
        } answers {
            MOCK_VERSION_CODES[firstArg()]?.let {
                PackageInfo().apply { longVersionCode = it }
            } ?: throw NameNotFoundException()
        }
        every { packageManagerMock.getLaunchIntentForPackage(any()) } returns Intent()
        every { packageManagerMock.getApplicationLabel(any()) } returns "Label"
    }

    @Test
    fun testGetPlugins() = runTest {
        sut.getPlugins().test {
            val item = awaitItem()
            //Intent does not have .equals() so compare the strings
            assertTrue(item.toString() == getMockPlugins().toString())
        }
    }

    @Test
    fun testGetUpdateCount() = runTest {
        sut.getUpdateCount().test {
            val item = awaitItem()
            assertTrue(item == 1)
        }
    }

    @Test
    fun testReload() = runTest {
        val currentRefreshTime = sut.refreshBus.value
        sut.reload(false)
        //Didn't meet minimum time so it should not have changed
        assertTrue(sut.refreshBus.value == currentRefreshTime)
        sut.reload(true)
        //Forcing should always reload
        assertFalse(sut.refreshBus.value == currentRefreshTime)
        //Reset the refresh time to 0 and then a non-forced refresh should work
        sut.refreshBus.value = 0L
        sut.reload(false)
        assertFalse(sut.refreshBus.value == 0L)
    }

    @Test
    fun testGetPluginInfoJson() = runTest {
        val plugin = sut.getPlugins().first().first() as Plugin.Remote
        val mock = RemotePluginInfo.UpdateJson(
            icon = null,
            description = "Plugin description one",
            downloadUrl = "https://www.example.com/one",
            minimumSmartspacerVersion = null,
            versionCode = 1
        )
        assertTrue(sut.getPluginInfo(plugin) == mock)
    }

    @Test
    fun testGetPluginInfoUrl() = runTest {
        val plugin = sut.getPlugins().first()[2] as Plugin.Remote
        val mock = RemotePluginInfo.Url("https://kieronquinn.co.uk/smartspacer/plugins/mock/3")
        assertTrue(sut.getPluginInfo(plugin) == mock)
    }

}