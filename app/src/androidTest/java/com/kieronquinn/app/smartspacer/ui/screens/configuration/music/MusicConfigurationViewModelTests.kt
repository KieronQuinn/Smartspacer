package com.kieronquinn.app.smartspacer.ui.screens.configuration.music

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.targets.MusicTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.music.MusicConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MusicConfigurationViewModelTests: BaseTest<MusicConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_MUSIC = "${BuildConfig.APPLICATION_ID}.target.music"

        private fun getMockTargetData(): TargetData {
            return TargetData(
                showAlbumArt = true,
                useDoorbell = false,
                hiddenPackages = setOf("hidden")
            )
        }
    }

    private val targetDataMock = MutableStateFlow(getMockTargetData())
    private val mockId = randomString()

    private val dataRepositoryMock = mock<DataRepository> {
        every { getTargetDataFlow(any(), TargetData::class.java) } returns targetDataMock
        every {
            updateTargetData(
                any(),
                TargetData::class.java,
                TargetDataType.MUSIC,
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

    override val sut by lazy {
        MusicConfigurationViewModelImpl(
            dataRepositoryMock,
            scope
        )
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_MUSIC
            }
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
        }
    }

    @Test
    fun testOnShowAlbumArtChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onShowAlbumArtChanged(false)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertFalse(updatedItem.showAlbumArt)
            sut.onShowAlbumArtChanged(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnUseDoorbellChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onUseDoorbellChanged(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.useDoorbell)
            sut.onUseDoorbellChanged(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnUseNotificationIconChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onUseNotificationIconChanged(false)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertFalse(updatedItem.useNotificationIcon)
            sut.onUseNotificationIconChanged(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnClearPackagesClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onClearPackagesClicked()
            assertTrue(targetDataMock.value.hiddenPackages.isEmpty())
        }
    }

}