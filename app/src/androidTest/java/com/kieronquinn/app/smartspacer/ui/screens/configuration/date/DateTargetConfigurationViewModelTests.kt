package com.kieronquinn.app.smartspacer.ui.screens.configuration.date

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DateTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.DateTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DateTargetConfigurationViewModelTests: BaseTest<DateTargetConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_DATE = "${BuildConfig.APPLICATION_ID}.target.date"
        private fun getMockTargetData(): TargetData {
            return TargetData()
        }
    }

    override val sut by lazy {
        DateTargetConfigurationViewModelImpl(
            dataRepositoryMock,
            navigationMock,
            scope
        )
    }

    private val targetDataMock = MutableStateFlow(getMockTargetData())
    private val navigationMock = mock<ConfigurationNavigation>()
    private val mockId = randomString()

    private val dataRepositoryMock = mock<DataRepository> {
        every { getTargetDataFlow(any(), TargetData::class.java) } returns targetDataMock
        every {
            updateTargetData(
                any(),
                TargetData::class.java,
                TargetDataType.DATE,
                any(),
                any()
            )
        } coAnswers {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(targetDataMock.value)
            targetDataMock.emit(newData as TargetData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_DATE
            }
        }
    }

    @Test
    fun testSetup() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setup(randomString())
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.data == targetDataMock.value)
        }
    }

    @Test
    fun testDateFormatChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setup(randomString())
            var item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.data.dateFormat == null)
            sut.onDateFormatChanged("dd/MM/yyyy")
            item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.data.dateFormat == "dd/MM/yyyy")
        }
    }

}