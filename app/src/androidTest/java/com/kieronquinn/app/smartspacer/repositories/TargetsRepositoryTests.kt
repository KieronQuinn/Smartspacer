package com.kieronquinn.app.smartspacer.repositories

import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DefaultComplication
import com.kieronquinn.app.smartspacer.components.smartspace.targets.AtAGlanceTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget
import com.kieronquinn.app.smartspacer.model.database.Action
import com.kieronquinn.app.smartspacer.model.database.Target
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TargetsRepositoryTests: BaseTest<TargetsRepository>() {

    companion object {
        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockTargets(): List<Target> {
            return listOf(
                createMockTarget(),
                createMockTarget(),
                createMockTarget()
            )
        }

        private fun createMockTarget(authority: String = randomString()): Target {
            return Target(
                randomString(),
                authority,
                randomInt(),
                randomString(),
                setOf(randomString(), randomString()),
                setOf(randomString(), randomString()),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean()
            )
        }

        private fun getMockActions(): List<Action> {
            return listOf(
                createMockAction(),
                createMockAction(),
                createMockAction()
            )
        }

        private fun createMockAction(authority: String = randomString()): Action {
            return Action(
                randomString(),
                authority,
                randomInt(),
                randomString(),
                setOf(randomString(), randomString()),
                setOf(randomString(), randomString()),
                randomBoolean(),
                randomBoolean(),
                randomBoolean()
            )
        }

        private fun getMockProviders(): List<ResolveInfo> {
            return listOf(
                createProviderInfo("com.example.one", "one"),
                createProviderInfo("com.example.two", "two"),
                createProviderInfo("com.example.three", "three")
            ).map {
                ResolveInfo().apply {
                    providerInfo = it
                }
            }
        }

        private fun createProviderInfo(packageName: String, authority: String): ProviderInfo {
            return ProviderInfo().apply {
                this.packageName = packageName
                this.authority = authority
            }
        }
    }

    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val mockProviders = getMockProviders()

    override val sut by lazy {
        TargetsRepositoryImpl(contextMock, databaseRepositoryMock, scope)
    }

    private val contentProviderClient = mock<ContentProviderClient> {
        every { call("get_targets_config", any(), any()) } answers {
            SmartspacerTargetProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON
            ).toBundle()
        }
        every { call("get_actions_config", any(), any()) } answers {
            SmartspacerComplicationProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON
            ).toBundle()
        }
    }

    override fun Context.context() {
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
        every {
            contentResolver.acquireUnstableContentProviderClient(any<String>())
        } answers {
            contentProviderClient
        }
        every {
            packageManagerMock.queryIntentContentProviders(any(), any<ResolveInfoFlags>())
        } returns mockProviders
    }

    @Test
    fun testGetAvailableTargets() = runTest {
        val targets = MutableStateFlow<List<Target>>(emptyList())
        every { databaseRepositoryMock.getTargets() } returns targets
        sut.getAvailableTargets().test {
            assertTrue(awaitItem().isEmpty())
            val mock = getMockTargets()
            targets.emit(mock)
            val actual = awaitItem()
            mock.zip(actual).forEach {
                val m = it.first
                val a = it.second
                assertTrue(m.id == a.id)
                assertTrue(m.authority == a.authority)
                assertTrue(m.packageName == a.sourcePackage)
                assertTrue(m.showOnHomeScreen == a.config.showOnHomeScreen)
                assertTrue(m.showOnLockScreen == a.config.showOnLockScreen)
                assertTrue(m.showOnMusic == a.config.showOverMusic)
                assertTrue(m.showAppShortcuts == a.config.showAppShortcuts)
                assertTrue(m.showShortcuts == a.config.showShortcuts)
                assertTrue(m.showRemoteViews == a.config.showRemoteViews)
                assertTrue(m.showWidget == a.config.showWidget)
            }
        }
    }

    @Test
    fun testGetAvailableActions() = runTest {
        val actions = MutableStateFlow<List<Action>>(emptyList())
        every { databaseRepositoryMock.getActions() } returns actions
        sut.getAvailableComplications().test {
            assertTrue(awaitItem().isEmpty())
            val mock = getMockActions()
            actions.emit(mock)
            val actual = awaitItem()
            mock.zip(actual).forEach {
                val m = it.first
                val a = it.second
                assertTrue(m.id == a.id)
                assertTrue(m.authority == a.authority)
                assertTrue(m.packageName == a.sourcePackage)
                assertTrue(m.showOnHomeScreen == a.config.showOnHomeScreen)
                assertTrue(m.showOnLockScreen == a.config.showOnLockScreen)
                assertTrue(m.showOnMusic == a.config.showOverMusic)
            }
        }
    }

    @Test
    fun testSetSmartspaceVisibility() = runTest {
        sut.smartspaceVisible.test {
            assertFalse(awaitItem())
            sut.setSmartspaceVisibility(true)
            assertTrue(awaitItem())
            sut.setSmartspaceVisibility(false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testGetAllTargets() = runTest {
        val actual = sut.getAllTargets()
        mockProviders.zip(actual).forEach {
            val m = it.first
            val a = it.second
            assertTrue(m.providerInfo.authority == a.authority)
            assertTrue(m.providerInfo.packageName == a.sourcePackage)
            assertTrue(a.id == null)
        }
    }

    @Test
    fun testGetAllActions() = runTest {
        val actual = sut.getAllComplications()
        mockProviders.zip(actual).forEach {
            val m = it.first
            val a = it.second
            assertTrue(m.providerInfo.authority == a.authority)
            assertTrue(m.providerInfo.packageName == a.sourcePackage)
            assertTrue(a.id == null)
        }
    }

    @Test
    fun testForceReloadAll() = runTest {
        val current = sut.forceReload.value
        sut.forceReloadAll()
        assertFalse(sut.forceReload.value == current)
    }

    @Test
    fun testGetRecommendedTargets() = runTest {
        val targets = listOf(
            createMockTarget(DefaultTarget.AUTHORITY),
            createMockTarget(AtAGlanceTarget.AUTHORITY)
        )
        every { databaseRepositoryMock.getTargets() } returns flowOf(targets)
        val mock = TargetsRepositoryImpl.RECOMMENDED_TARGETS.toList().let {
            it.subList(2, it.size)
        }
        val actual = sut.getRecommendedTargets()
        mock.zip(actual).forEach {
            val m = it.first
            val a = it.second
            assertTrue(m == a.authority)
        }
    }

    @Test
    fun testGetRecommendedActions() = runTest {
        val actions = listOf(createMockAction(DefaultComplication.AUTHORITY))
        every { databaseRepositoryMock.getActions() } returns flowOf(actions)
        val mock = TargetsRepositoryImpl.RECOMMENDED_COMPLICATIONS.toList().let {
            it.subList(1, it.size)
        }
        val actual = sut.getRecommendedComplications()
        mock.zip(actual).forEach {
            val m = it.first
            val a = it.second
            assertTrue(m == a.authority)
        }
    }

    @Test
    fun testPerformTargetRestore() = runTest {
        val authority = randomString()
        val id = randomString()
        val backup = Backup(randomString(), randomString())
        sut.performTargetRestore(authority, id, backup)
        verify {
            contentProviderClient.call(
                SmartspacerTargetProvider.METHOD_RESTORE,
                any(),
                any()
            )
        }
    }

    @Test
    fun testPerformComplicationRestore() = runTest {
        val authority = randomString()
        val id = randomString()
        val backup = Backup(randomString(), randomString())
        sut.performComplicationRestore(authority, id, backup)
        verify {
            contentProviderClient.call(
                SmartspacerComplicationProvider.METHOD_RESTORE,
                any(),
                any()
            )
        }
    }

}