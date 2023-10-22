package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_CHANGED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.content.Intent.ACTION_PACKAGE_REPLACED
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PackageRepositoryTests: BaseTest<PackageRepository>() {

    companion object {
        private fun getUpdateIntent(action: String, packageName: String): Intent {
            return Intent(action).apply {
                data = Uri.parse("package:$packageName")
            }
        }

        private fun getMockLaunchableApps(): List<ResolveInfo> {
            return listOf(
                mockResolveInfo("com.example.launchableone", "One"),
                mockResolveInfo("com.example.launchabletwo", "Two")
            )
        }

        private fun mockResolveInfo(packageName: String, label: String): ResolveInfo {
            return ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    this.packageName = packageName
                }
            }
        }

        private fun getMockInstalledApps(): List<ApplicationInfo> {
            return listOf(
                mockApplicationInfo("com.example.one", "One"),
                mockApplicationInfo("com.example.two", "Two")
            )
        }

        private fun mockApplicationInfo(packageName: String, label: String): ApplicationInfo {
            return ApplicationInfo().apply {
                this.packageName = packageName
            }
        }
    }

    override val sut by lazy {
        PackageRepositoryImpl(contextMock)
    }

    override fun Context.context() {
        every {
            packageManagerMock.getInstalledApplications(any<ApplicationInfoFlags>())
        } returns getMockInstalledApps()
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        every {
            packageManagerMock.queryIntentActivities(launchIntent, any<ResolveInfoFlags>())
        } returns getMockLaunchableApps()
    }

    @Test
    fun testOnPackageChanged() = runTest {
        sut.onPackageChanged.test {
            expectNoEvents()
            //Test ACTION_PACKAGE_ADDED
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_ADDED, "com.example.one")
            )
            assertTrue(awaitItem() == "com.example.one")
            //Test ACTION_PACKAGE_CHANGED
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_CHANGED, "com.example.two")
            )
            assertTrue(awaitItem() == "com.example.two")
            //Test ACTION_PACKAGE_REPLACED
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_REPLACED, "com.example.three")
            )
            assertTrue(awaitItem() == "com.example.three")
            //Test ACTION_PACKAGE_REMOVED
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_REMOVED, "com.example.four")
            )
            assertTrue(awaitItem() == "com.example.four")
        }
    }

    @Test
    fun testOnPackageChangedForPackages() = runTest {
        sut.onPackageChanged("com.example.one", "com.example.two").test {
            expectNoEvents()
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_CHANGED, "com.example.one")
            )
            val previous = awaitItem()
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_CHANGED, "com.example.two")
            )
            assertFalse(previous == awaitItem())
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_CHANGED, "com.example.three")
            )
            expectNoEvents()
        }
    }

    @Test
    fun testOnPackageChangedForPackagesState() = runTest {
        sut.onPackageChanged(scope, "com.example.one", "com.example.two").test {
            //Same as last but starts with a value
            var previous = awaitItem()
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_CHANGED, "com.example.one")
            )
            val new = awaitItem()
            assertFalse(previous == new)
            previous = new
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_CHANGED, "com.example.two")
            )
            assertFalse(previous == awaitItem())
            contextMock.sendBroadcast(
                getUpdateIntent(ACTION_PACKAGE_CHANGED, "com.example.three")
            )
            expectNoEvents()
        }
    }

    @Test
    fun testGetInstalledAppsLaunchable() = runTest {
        val actual = sut.getInstalledApps()
        actual.zip(getMockInstalledApps()).forEach {
            val a = it.first
            val m = it.second
            assertTrue(a.packageName == m.packageName)
            assertTrue(a.label == m.loadLabel(packageManagerMock))
        }
    }

    @Test
    fun testGetInstalledAppsNotLaunchable() = runTest {
        val actual = sut.getInstalledApps(true)
        actual.zip(getMockInstalledApps()).forEach {
            val a = it.first
            val m = it.second
            assertTrue(a.packageName == m.packageName)
            assertTrue(a.label == m.loadLabel(packageManagerMock))
        }
    }

}