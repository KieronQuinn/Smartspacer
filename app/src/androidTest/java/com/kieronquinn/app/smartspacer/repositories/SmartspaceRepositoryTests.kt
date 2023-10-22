package com.kieronquinn.app.smartspacer.repositories

import android.content.ComponentName
import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Process
import android.util.Log
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.ISmartspacerShizukuService
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.model.smartspace.ActionHolder
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.extensions.Icon_createEmptyIcon
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import android.graphics.drawable.Icon as AndroidIcon

@Suppress("CloseTarget", "CloseAction")
class SmartspaceRepositoryTests: BaseTest<SmartspaceRepository>() {

    companion object {
        private val DUMMY_ICON = AndroidIcon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockTargets(vararg targets: TargetHolder): List<Target> {
            return targets.map {
                createTargetMock(MutableStateFlow(it))
            }
        }

        private fun getMockComplications(vararg complications: ActionHolder): List<Action> {
            return complications.map {
                createComplicationMock(MutableStateFlow(it))
            }
        }

        private fun createTargetMock(targets: StateFlow<TargetHolder>): Target {
            return mock {
                coEvery { collect(any()) } coAnswers  {
                    targets.collect(firstArg())
                }
            }
        }

        private fun createComplicationMock(targets: StateFlow<ActionHolder>): Action {
            return mock {
                coEvery { collect(any()) } coAnswers  {
                    targets.collect(firstArg())
                }
            }
        }

        private fun createMockTargetForParent(
            context: Context,
            config: Target.Config = Target.Config()
        ): Target {
            return Target(context, randomString(), randomString(), randomString(), config = config)
        }

        private fun createMockComplicationForParent(
            context: Context,
            config: Action.Config = Action.Config()
        ): Action {
            return Action(context, randomString(), randomString(), randomString(), config = config)
        }

        private fun createMockSmartspaceTarget(): SmartspaceTarget {
            return TargetTemplate.Basic(
                randomString(),
                ComponentName(randomString(), randomString()),
                SmartspaceTarget.FEATURE_UNDEFINED,
                Text(randomString()),
                Text(randomString()),
                Icon(Icon_createEmptyIcon()),
                null,
                null
            ).create().apply {
                headerAction = headerAction?.copy(skipPendingIntent = true)
                baseAction = baseAction?.copy(skipPendingIntent = true)
            }
        }

        private fun createMockSmartspaceAction(): SmartspaceAction {
            return ComplicationTemplate.Basic(
                randomString(),
                Icon(Icon_createEmptyIcon()),
                Text(randomString()),
                null
            ).create()
        }

        private fun getMockSystemSmartspaceTargets(): List<SmartspaceTarget> {
            return listOf(
                createMockSmartspaceTarget().copy(
                    featureType = SmartspaceTarget.FEATURE_WEATHER
                ),
                createMockSmartspaceTarget(),
                createMockSmartspaceTarget()
            )
        }

        private fun getMockPageHolders(context: Context): List<SmartspacePageHolder> {
            return listOf(
                SmartspacePageHolder(
                    createMockSmartspaceTarget(),
                    createMockTargetForParent(context),
                    listOf(createMockComplicationForParent(context))
                ),
                SmartspacePageHolder(
                    createMockSmartspaceTarget(),
                    createMockTargetForParent(context),
                    listOf(createMockComplicationForParent(context))
                )
            )
        }
    }

    private val shizukuServiceRepositoryMock = mock<ShizukuServiceRepository>()
    private val systemSmartspaceRepositoryMock = mock<SystemSmartspaceRepository>()
    private val targetsRepositoryMock = mock<TargetsRepository>()

    private val contentProviderClient = mock<ContentProviderClient> {
        every { call("get_targets_config", any(), any()) } answers {
            SmartspacerTargetProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON,
                refreshPeriodMinutes = 15
            ).toBundle()
        }
        every { call("get_actions_config", any(), any()) } answers {
            SmartspacerComplicationProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON,
                refreshPeriodMinutes = 15
            ).toBundle()
        }
    }

    private val targets = MutableSharedFlow<List<Target>>()
    private val complications = MutableSharedFlow<List<Action>>()

    override val sut by lazy {
        SmartspaceRepositoryImpl(
            contextMock,
            shizukuServiceRepositoryMock,
            systemSmartspaceRepositoryMock,
            targetsRepositoryMock,
            scope,
            Dispatchers.Main
        )
    }

    override fun setup() {
        super.setup()
        every { targetsRepositoryMock.getAvailableTargets() } returns targets
        every { targetsRepositoryMock.getAvailableComplications() } returns complications
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = randomString()
            }
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
    }

    @Test
    fun testTargets() = runTest {
        val mockParentTargetOne = createMockTargetForParent(contextMock)
        val mockParentTargetTwo = createMockTargetForParent(contextMock)
        val targetOne = TargetHolder(mockParentTargetOne, emptyList())
        val targetTwo = TargetHolder(mockParentTargetTwo, emptyList())
        val mockTargets = getMockTargets(targetOne, targetTwo)
        targets.emit(mockTargets)
        sut.targets.test {
            assertTrue(awaitItem().isEmpty())
            val one = TargetHolder(mockParentTargetOne, listOf(createMockSmartspaceTarget()))
            val two = TargetHolder(mockParentTargetTwo, listOf(createMockSmartspaceTarget()))
            val targets = getMockTargets(one, two)
            this@SmartspaceRepositoryTests.targets.emit(targets)
            val actual = awaitItem()
            assertTrue(actual[0].targets == one.targets)
            assertTrue(actual[1].targets == two.targets)
            assertTrue(actual[0].parent.authority == one.parent.authority)
            assertTrue(actual[1].parent.authority == two.parent.authority)
        }
    }

    @Test
    fun testComplications() = runTest {
        val mockParentComplicationOne = createMockComplicationForParent(contextMock)
        val mockParentComplicationTwo = createMockComplicationForParent(contextMock)
        val actionOne = ActionHolder(mockParentComplicationOne, emptyList())
        val actionTwo = ActionHolder(mockParentComplicationTwo, emptyList())
        val mockComplications = getMockComplications(actionOne, actionTwo)
        complications.emit(mockComplications)
        sut.actions.test {
            assertTrue(awaitItem().isEmpty())
            val one = ActionHolder(mockParentComplicationOne, listOf(createMockSmartspaceAction()))
            val two = ActionHolder(mockParentComplicationTwo, listOf(createMockSmartspaceAction()))
            val complications = getMockComplications(one, two)
            this@SmartspaceRepositoryTests.complications.emit(complications)
            val actual = awaitItem()
            assertTrue(actual[0].actions == one.actions)
            assertTrue(actual[1].actions == two.actions)
            assertTrue(actual[0].parent.authority == one.parent.authority)
            assertTrue(actual[1].parent.authority == two.parent.authority)
        }
    }

    @Test
    fun testMergeTargetsAndActions() = runTest {
        val targetOne = TargetHolder(
            createMockTargetForParent(contextMock),
            listOf(createMockSmartspaceTarget(), createMockSmartspaceTarget())
        )
        val targetTwo = TargetHolder(
            createMockTargetForParent(contextMock, Target.Config(showOnLockScreen = false)),
            listOf(createMockSmartspaceTarget())
        )
        val actionOne = ActionHolder(
            createMockComplicationForParent(contextMock),
            listOf(createMockSmartspaceAction(), createMockSmartspaceAction())
        )
        val actionTwo = ActionHolder(
            createMockComplicationForParent(contextMock, Action.Config(showOnLockScreen = false)),
            listOf(createMockSmartspaceAction())
        )
        val home = sut.mergeTargetsAndActions(
            listOf(targetOne, targetTwo),
            listOf(actionOne, actionTwo),
            ExpandedOpenMode.ALWAYS,
            UiSurface.HOMESCREEN,
            doesHaveSplitSmartspace = false,
            isNative = false
        )
        val mockHomeTargets = listOfNotNull(targetOne.targets, targetTwo.targets).flatten()
        val mockHomeActions = listOfNotNull(actionOne.actions, actionTwo.actions).flatten()
        home.zip(mockHomeTargets).forEach {
            val actual = it.first.page
            val mock = it.second
            assertTrue(actual.templateData!!.primaryItem == mock.templateData!!.primaryItem)
        }
        home.zip(mockHomeActions).forEach {
            val actual = it.first.page.templateData!!.subtitleSupplementalItem!!.text
            val mock = it.second.subItemInfo!!.text
            assertTrue(actual == mock)
        }
        val lock = sut.mergeTargetsAndActions(
            listOf(targetOne, targetTwo),
            listOf(actionOne, actionTwo),
            ExpandedOpenMode.ALWAYS,
            UiSurface.LOCKSCREEN,
            doesHaveSplitSmartspace = false,
            isNative = false
        )
        val mockLockTargets = listOfNotNull(targetOne.targets).flatten()
        val mockLockActions = listOfNotNull(actionOne.actions).flatten()
        lock.zip(mockLockTargets).forEach {
            val actual = it.first.page
            val mock = it.second
            assertTrue(actual.templateData!!.primaryItem == mock.templateData!!.primaryItem)
        }
        lock.zip(mockLockActions).forEach {
            val actual = it.first.page.templateData!!.subtitleSupplementalItem!!.text
            val mock = it.second.subItemInfo!!.text
            assertTrue(actual == mock)
        }
        val split = sut.mergeTargetsAndActions(
            listOf(targetOne, targetTwo),
            listOf(actionOne, actionTwo),
            ExpandedOpenMode.ALWAYS,
            UiSurface.LOCKSCREEN,
            doesHaveSplitSmartspace = true,
            isNative = false
        )
        val splitFirstPage = split.subList(0, 1)
        val splitRemainder = split.subList(1, split.size)
        val mockLockActionsSplit = mockLockActions.subList(0, 1)
        val mockLockActionsMinusSplit = mockLockActions.subList(1, mockLockActions.size)
        splitRemainder.zip(mockLockTargets).forEach {
            val actual = it.first.page
            val mock = it.second
            assertTrue(actual.templateData!!.primaryItem == mock.templateData!!.primaryItem)
        }
        splitRemainder.zip(mockLockActionsMinusSplit).forEach {
            val actual = it.first.page.templateData!!.subtitleSupplementalItem!!.text
            val mock = it.second.subItemInfo!!.text
            assertTrue(actual == mock)
        }
        splitFirstPage.zip(mockLockActionsSplit).forEach {
            val actual = it.first.page.headerAction!!
            val mock = it.second
            assertTrue(actual.subtitle == mock.subtitle)
        }
    }

    @Test
    fun testSetSmartspaceVisible() = runTest {
        val sessionIdOne = SmartspaceSessionId(randomString(), Process.myUserHandle())
        sut.setSmartspaceVisible(sessionIdOne, true)
        assertTrue(sut.smartspaceVisible[sessionIdOne.id] == true)
        sut.setSmartspaceVisible(sessionIdOne, false)
        assertTrue(sut.smartspaceVisible[sessionIdOne.id] == false)
        val sessionIdTwo = SmartspaceSessionId(randomString(), Process.myUserHandle())
        sut.setSmartspaceVisible(sessionIdTwo, true)
        assertTrue(sut.smartspaceVisible[sessionIdTwo.id] == true)
    }

    @Test
    fun testNotifyClickEventFlashlight() = runTest {
        sut.notifyClickEvent("ambient_light_${randomString()}", "FLASHLIGHT")
        coVerify {
            shizukuServiceRepositoryMock
                .runWithService(any<suspend (ISmartspacerShizukuService) -> Any>())
        }
    }

    @Test
    fun testNotifyClickEventComplication() = runTest {
        val mockSmartspaceTarget = createMockSmartspaceTarget()
        val mockParentTargetOne = createMockTargetForParent(contextMock)
        val mockParentTargetTwo = createMockTargetForParent(contextMock)
        val mockParentTargetThree = mock<Target> {
            every { this@mock.id } returns randomString()
            every { this@mock.authority } returns randomString()
            every { this@mock.sourcePackage } returns randomString()
        }
        val mockParentTargetFour = mock<Target> {
            every { this@mock.id } returns randomString()
            every { this@mock.authority } returns randomString()
            every { this@mock.sourcePackage } returns randomString()
        }
        val targetOne = TargetHolder(mockParentTargetOne, listOf(createMockSmartspaceTarget()))
        val targetTwo = TargetHolder(mockParentTargetTwo, listOf(createMockSmartspaceTarget()))
        val mockTargets = getMockTargets(targetOne, targetTwo)
        targets.emit(mockTargets)
        sut.targets.test {
            awaitItem()
            val one = TargetHolder(mockParentTargetThree, listOf(createMockSmartspaceTarget()))
            val two = TargetHolder(mockParentTargetFour, listOf(mockSmartspaceTarget))
            val targets = getMockTargets(one, two)
            this@SmartspaceRepositoryTests.targets.emit(targets)
            val actual = awaitItem()
            assertTrue(actual[0].targets == one.targets)
            assertTrue(actual[1].targets == two.targets)
            assertTrue(actual[0].parent.authority == one.parent.authority)
            assertTrue(actual[1].parent.authority == two.parent.authority)
        }
        val mockSmartspaceAction = createMockSmartspaceAction()
        val mockParentComplicationOne = createMockComplicationForParent(contextMock)
        val mockParentComplicationTwo = createMockComplicationForParent(contextMock)
        val mockParentComplicationThree = mock<Action> {
            every { this@mock.id } returns randomString()
            every { this@mock.authority } returns randomString()
            every { this@mock.sourcePackage } returns randomString()
        }
        val mockParentComplicationFour = mock<Action> {
            every { this@mock.id } returns randomString()
            every { this@mock.authority } returns randomString()
            every { this@mock.sourcePackage } returns randomString()
        }
        val actionOne = ActionHolder(mockParentComplicationOne, emptyList())
        val actionTwo = ActionHolder(mockParentComplicationTwo, emptyList())
        val mockComplications = getMockComplications(actionOne, actionTwo)
        complications.emit(mockComplications)
        sut.actions.test {
            awaitItem().isEmpty()
            val one = ActionHolder(mockParentComplicationThree, listOf(createMockSmartspaceAction()))
            val two = ActionHolder(mockParentComplicationFour, listOf(mockSmartspaceAction))
            val complications = getMockComplications(one, two)
            this@SmartspaceRepositoryTests.complications.emit(complications)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testGetDefaultHomeTargets() = runTest {
        val targets = getMockSystemSmartspaceTargets()
        every {
            systemSmartspaceRepositoryMock.homeTargets
        } returns MutableStateFlow(targets)
        sut.getDefaultHomeTargets().test {
            //Should only contain fully qualified targets (not weather)
            assertTrue(awaitItem() == targets.subList(1, targets.size))
        }
    }

    @Test
    fun testGetDefaultHomeActions() = runTest {
        val targets = getMockSystemSmartspaceTargets()
        every {
            systemSmartspaceRepositoryMock.homeTargets
        } returns MutableStateFlow(targets)
        sut.getDefaultHomeActions().test {
            //Should only contain weather target(s)
            val mock = targets.first().let {
                listOfNotNull(it.headerAction)
            }
            val actual = awaitItem()
            assertTrue(actual == mock)
        }
    }

    @Test
    fun testGetDefaultLockTargets() = runTest {
        val targets = getMockSystemSmartspaceTargets()
        every {
            systemSmartspaceRepositoryMock.lockTargets
        } returns MutableStateFlow(targets)
        sut.getDefaultLockTargets().test {
            val item = awaitItem()
            Log.d("SRT", "Actual: ${item.joinToString(", ")}")
            Log.d("SRT", "Mock: ${targets.subList(1, targets.size)}")
            //Should only contain fully qualified targets (not weather)
            assertTrue(item == targets.subList(1, targets.size))
        }
    }

    @Test
    fun testGetDefaultLockActions() = runTest {
        val targets = getMockSystemSmartspaceTargets()
        every {
            systemSmartspaceRepositoryMock.lockTargets
        } returns MutableStateFlow(targets)
        sut.getDefaultLockActions().test {
            //Should only contain weather target(s)
            val mock = targets.first().let {
                listOfNotNull(it.headerAction)
            }
            val actual = awaitItem()
            assertTrue(actual == mock)
        }
    }

    @Test
    fun testDismissDefaultTarget() = runTest {
        val id = randomString()
        sut.dismissDefaultTarget(id)
        verify(exactly = 1) {
            systemSmartspaceRepositoryMock.dismissDefaultTarget(id)
        }
    }

    @Test
    fun requestSmartspaceUpdate() = runTest {
        val mockHolders = getMockPageHolders(contextMock)
        sut.requestSmartspaceUpdate(mockHolders)
        verify {
            contextMock.sendBroadcast(any())
        }
    }

    @Test
    fun testRequestPluginUpdates() = runTest {
        val mockHolders = getMockPageHolders(contextMock)
        sut.requestPluginUpdates(mockHolders)
        mockHolders.forEach {
            assertTrue((sut.lastPluginUpdateTimes[it.target!!.id] ?: return@forEach) != 0L)
            it.actions.forEach { action ->
                assertTrue((sut.lastPluginUpdateTimes[action.id] ?: return@forEach) != 0L)
            }
        }
        var lastTimes = sut.lastPluginUpdateTimes.toMutableMap()
        //Requesting a second time should not re-trigger updates
        sut.requestPluginUpdates(mockHolders)
        assertTrue(sut.lastPluginUpdateTimes == lastTimes)
        //Reset second ID and re-request, second time should change but rest not
        lastTimes = sut.lastPluginUpdateTimes.toMutableMap()
        val idToRemove = mockHolders.first().target!!.id!!
        sut.lastPluginUpdateTimes[idToRemove] = 0L
        sut.requestPluginUpdates(mockHolders)
        mockHolders.forEach {
            val id = it.target!!.id
            val last = lastTimes[id] ?: return@forEach
            if(id == idToRemove) {
                assertFalse((sut.lastPluginUpdateTimes[it.target!!.id] ?: return@forEach) == last)
            }else{
                assertTrue((sut.lastPluginUpdateTimes[it.target!!.id] ?: return@forEach) == last)
            }
            it.actions.forEach { action ->
                val actionLast = lastTimes[action.id] ?: return@forEach
                assertTrue((sut.lastPluginUpdateTimes[action.id] ?: return@forEach) == actionLast)
            }
        }
    }

    @Test
    fun testRequestPluginUpdatesLimited() = runTest {
        val mockHolders = getMockPageHolders(contextMock)
        val limitTo = mockHolders.first().target!!.sourcePackage
        val limitToId = mockHolders.first().target!!.id
        val shouldNotUpdate = setOf(
            mockHolders.map { it.target!!.id },
            mockHolders.map { it.actions.map { action -> action.id } }
        ).flatten().minus(limitToId)
        sut.requestPluginUpdates(mockHolders, limitTo)
        //The times list should now only contain the limitTo ID and nothing else
        assertTrue(sut.lastPluginUpdateTimes.containsKey(limitToId))
        shouldNotUpdate.forEach {
            assertFalse(sut.lastPluginUpdateTimes.containsKey(it))
        }
    }

}