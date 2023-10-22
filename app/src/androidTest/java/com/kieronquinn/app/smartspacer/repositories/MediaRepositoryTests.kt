package com.kieronquinn.app.smartspacer.repositories

import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.model.media.MediaContainer
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MediaRepositoryTests: BaseTest<MediaRepository>() {

    companion object {
        private const val AUTHORITY_MUSIC = "${BuildConfig.APPLICATION_ID}.target.music"

        private fun getMockMediaContainer(): MediaContainer {
            return MediaContainer(randomString(), mock(), mock())
        }
    }

    override val sut by lazy {
        MediaRepositoryImpl(contextMock)
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
    fun testMediaController() = runTest {
        sut.mediaController.test {
            assertTrue(awaitItem() == null)
            val mock = getMockMediaContainer()
            sut.setMediaContainer(mock)
            assertTrue(awaitItem() == mock)
            delay(500L)
            verify {
                contentResolverMock.notifyChange(createUri(), null, 0)
            }
        }
    }

    @Test
    fun testMediaPlaying() = runTest {
        sut.mediaPlaying.test {
            assertFalse(awaitItem())
            val mock = getMockMediaContainer()
            sut.setMediaContainer(mock)
            assertTrue(awaitItem())
        }
    }

    private fun createUri(): Uri {
        return Uri.Builder().apply {
            scheme("content")
            authority(AUTHORITY_MUSIC)
        }.build()
    }

}