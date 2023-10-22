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
import com.kieronquinn.app.smartspacer.model.database.Action
import com.kieronquinn.app.smartspacer.model.database.Target
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.kieronquinn.app.smartspacer.model.database.Requirement as DatabaseRequirement

@Suppress("CloseTarget", "CloseAction")
class RequirementsRepositoryTests: BaseTest<RequirementsRepository>() {

    companion object {
        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockContentProviders(): List<ResolveInfo> {
            return listOf(
                getMockResolveInfo("com.example.one", "one"),
                getMockResolveInfo("com.example.two", "two"),
                getMockResolveInfo("com.example.three", "three")
            )
        }

        private fun getMockResolveInfo(packageName: String, authority: String): ResolveInfo {
            return ResolveInfo().apply {
                providerInfo = ProviderInfo().apply {
                    this.packageName = packageName
                    this.authority = authority
                }
            }
        }

        private fun getMockRequirements(): List<DatabaseRequirement> {
            return listOf(
                DatabaseRequirement(
                    authority = randomString(),
                    packageName = randomString(),
                    invert = randomBoolean()
                ),
                DatabaseRequirement(
                    authority = randomString(),
                    packageName = randomString(),
                    invert = randomBoolean()
                ),
                DatabaseRequirement(
                    authority = randomString(),
                    packageName = randomString(),
                    invert = randomBoolean()
                )
            )
        }

        private fun createMockContentProviderClient() = mock<ContentProviderClient> {
            every { call("get_requirement_config", any(), any()) } answers {
                SmartspacerRequirementProvider.Config(
                    randomString(),
                    randomString(),
                    DUMMY_ICON
                ).toBundle()
            }
        }
    }

    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val contentProviderClient = createMockContentProviderClient()

    override val sut by lazy {
        RequirementsRepositoryImpl(contextMock, databaseRepositoryMock, scope)
    }

    override fun Context.context() {
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } returns contentProviderClient
        every {
            contentResolver.acquireUnstableContentProviderClient(any<String>())
        } returns contentProviderClient
    }

    override fun setup() {
        super.setup()
        every {
            packageManagerMock.queryIntentContentProviders(any(), any<ResolveInfoFlags>())
        } returns getMockContentProviders()
    }

    @Test
    fun testGetAllRequirements() = runTest {
        val actual = sut.getAllRequirements()
        val mock = getMockContentProviders()
        assertTrue(actual.map { it.sourcePackage } == mock.map { it.providerInfo.packageName })
        assertTrue(actual.map { it.authority } == mock.map { it.providerInfo.authority })
        //IDs should not be set at this stage
        assertTrue(actual.all { it.id == null })
    }

    @Test
    fun testGetAllInUseRequirements() = runTest {
        val mock = getMockRequirements()
        every { databaseRepositoryMock.getRequirements() } returns flowOf(mock)
        sut.getAllInUseRequirements().test {
            val actual = awaitItem()
            assertTrue(actual.map { it.sourcePackage } == mock.map { it.packageName })
            assertTrue(actual.map { it.authority } == mock.map { it.authority })
            assertTrue(actual.map { it.id } == mock.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testGetAnyRequirementsForTarget() = runTest {
        val mock = DatabaseRequirement(
            authority = randomString(),
            packageName = randomString(),
            invert = randomBoolean()
        )
        val target = Target(
            randomString(),
            randomString(),
            randomInt(),
            randomString(),
            anyRequirements = setOf(mock.id)
        )
        every { databaseRepositoryMock.getTargetById(any()) } returns flowOf(target)
        every { databaseRepositoryMock.getRequirementById(mock.id) } returns flowOf(mock)
        sut.getAnyRequirementsForTarget(randomString()).test {
            val actual = awaitItem().first()
            assertTrue(actual.id == mock.id)
            assertTrue(actual.authority == mock.authority)
            assertTrue(actual.sourcePackage == mock.packageName)
            assertTrue(actual.invert == mock.invert)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testGetAllRequirementsForTarget() = runTest {
        val mock = DatabaseRequirement(
            authority = randomString(),
            packageName = randomString(),
            invert = randomBoolean()
        )
        val target = Target(
            randomString(),
            randomString(),
            randomInt(),
            randomString(),
            allRequirements = setOf(mock.id)
        )
        every { databaseRepositoryMock.getTargetById(any()) } returns flowOf(target)
        every { databaseRepositoryMock.getRequirementById(mock.id) } returns flowOf(mock)
        sut.getAllRequirementsForTarget(randomString()).test {
            val actual = awaitItem().first()
            assertTrue(actual.id == mock.id)
            assertTrue(actual.authority == mock.authority)
            assertTrue(actual.sourcePackage == mock.packageName)
            assertTrue(actual.invert == mock.invert)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testGetAnyRequirementsForComplication() = runTest {
        val mock = DatabaseRequirement(
            authority = randomString(),
            packageName = randomString(),
            invert = randomBoolean()
        )
        val complication = Action(
            randomString(),
            randomString(),
            randomInt(),
            randomString(),
            anyRequirements = setOf(mock.id)
        )
        every { databaseRepositoryMock.getActionById(any()) } returns flowOf(complication)
        every { databaseRepositoryMock.getRequirementById(mock.id) } returns flowOf(mock)
        sut.getAnyRequirementsForComplication(randomString()).test {
            val actual = awaitItem().first()
            assertTrue(actual.id == mock.id)
            assertTrue(actual.authority == mock.authority)
            assertTrue(actual.sourcePackage == mock.packageName)
            assertTrue(actual.invert == mock.invert)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testGetAllRequirementsForComplication() = runTest {
        val mock = DatabaseRequirement(
            authority = randomString(),
            packageName = randomString(),
            invert = randomBoolean()
        )
        val complication = Action(
            randomString(),
            randomString(),
            randomInt(),
            randomString(),
            allRequirements = setOf(mock.id)
        )
        every { databaseRepositoryMock.getActionById(any()) } returns flowOf(complication)
        every { databaseRepositoryMock.getRequirementById(mock.id) } returns flowOf(mock)
        sut.getAllRequirementsForComplication(randomString()).test {
            val actual = awaitItem().first()
            assertTrue(actual.id == mock.id)
            assertTrue(actual.authority == mock.authority)
            assertTrue(actual.sourcePackage == mock.packageName)
            assertTrue(actual.invert == mock.invert)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testAny() = runTest {
        val requirementOne = MutableStateFlow(false)
        val requirementTwo = MutableStateFlow(false)
        val requirements = listOf(mockRequirement(requirementOne), mockRequirement(requirementTwo))
        sut.any(flowOf(requirements)).test {
            assertFalse(awaitItem())
            requirementOne.emit(true)
            assertTrue(awaitItem())
            requirementTwo.emit(true)
            assertTrue(awaitItem())
            requirementOne.emit(false)
            assertTrue(awaitItem())
            requirementTwo.emit(false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testAll() = runTest {
        val requirementOne = MutableStateFlow(false)
        val requirementTwo = MutableStateFlow(false)
        val requirements = listOf(mockRequirement(requirementOne), mockRequirement(requirementTwo))
        sut.all(flowOf(requirements)).test {
            assertFalse(awaitItem())
            requirementOne.emit(true)
            assertFalse(awaitItem())
            requirementTwo.emit(true)
            assertTrue(awaitItem())
            requirementOne.emit(false)
            assertFalse(awaitItem())
            requirementTwo.emit(false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testForceReloadAll() = runTest {
        sut.forceReload.test {
            val current = awaitItem()
            sut.forceReloadAll()
            assertFalse(awaitItem() == current)
        }
    }

    @Test
    fun testPerformRequirementRestore() = runTest {
        val authority = randomString()
        val id = randomString()
        val backup = Backup(randomString(), randomString())
        sut.performRequirementRestore(authority, id, false, backup)
        verify {
            contentProviderClient.call(
                SmartspacerRequirementProvider.METHOD_RESTORE,
                any(),
                any()
            )
        }
    }

    /**
     *  Mocks a [Requirement] for state use by redirecting calls of [Requirement.collect] to
     *  [MutableStateFlow.collect] on the given [stateFlow].
     */
    private fun mockRequirement(stateFlow: MutableStateFlow<Boolean>) = mock<Requirement> {
        coEvery { collect(any()) } coAnswers {
            stateFlow.collect(firstArg())
        }
    }

}