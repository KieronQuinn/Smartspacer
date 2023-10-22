package com.kieronquinn.app.smartspacer.ui.screens.configuration.gmail

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.complications.GmailComplication.ActionData
import com.kieronquinn.app.smartspacer.model.database.ActionDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.GmailRepository
import com.kieronquinn.app.smartspacer.repositories.GmailRepository.Label
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.gmail.GmailComplicationConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.awaitItemOrTwo
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GmailComplicationConfigurationViewModelTests: BaseTest<GmailComplicationConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_GMAIL = "${BuildConfig.APPLICATION_ID}.complication.gmail"

        private fun getMockActionData(): ActionData {
            return ActionData(randomString(), randomString(), enabledLabels = setOf("enabled"))
        }

        private fun getMockLabels(): List<Label> {
            return listOf(
                Label(randomString(), "enabled"),
                Label(randomString(), "disabled")
            )
        }
    }

    private val actionDataMock = MutableStateFlow(getMockActionData())
    private val labelsMock = getMockLabels()
    private val mockId = randomString()

    private val dataRepositoryMock = mock<DataRepository> {
        every { getActionDataFlow(any(), ActionData::class.java) } returns actionDataMock
        every {
            updateActionData(
                any(),
                ActionData::class.java,
                ActionDataType.GMAIL,
                any(),
                any()
            )
        } coAnswers {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(actionDataMock.value)
            actionDataMock.emit(newData as ActionData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    private val navigationMock = mock<ConfigurationNavigation>()

    private val gmailRepositoryMock = mock<GmailRepository> {
        coEvery { getAllLabels(any()) } returns labelsMock
    }

    override val sut by lazy {
        GmailComplicationConfigurationViewModelImpl(
            dataRepositoryMock,
            navigationMock,
            gmailRepositoryMock,
            scope
        )
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_GMAIL
            }
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            sut.setHasGrantedPermission(true)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testSetHasGrantedPermission() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            sut.setHasGrantedPermission(false)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.labels.isEmpty())
            sut.setHasGrantedPermission(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.labels.isNotEmpty())
        }
    }

    @Test
    fun testOnAccountSelected() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            sut.setHasGrantedPermission(true)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val newAccount = randomString()
            sut.onAccountSelected(newAccount)
            val updatedItem = awaitItemOrTwo()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.settings.accountName == newAccount)
        }
    }

    @Test
    fun testOnLabelChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            sut.setHasGrantedPermission(true)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val label = labelsMock.first()
            sut.onLabelChanged(label, false)
            val updatedItem = awaitItemOrTwo()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertFalse(updatedItem.settings.enabledLabels.contains("enabled"))
            sut.onLabelChanged(label, true)
            cancelAndIgnoreRemainingEvents()
        }
    }

}