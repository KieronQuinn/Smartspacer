package com.kieronquinn.app.smartspacer.repositories

import android.graphics.Bitmap
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.TodayState
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomString
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GoogleWeatherRepositoryTests: BaseTest<GoogleWeatherRepository>() {

    companion object {
        private fun getMockState(): TodayState {
            return TodayState(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8),
                randomString()
            )
        }
    }

    override val sut by lazy {
        GoogleWeatherRepositoryImpl()
    }

    @Test
    fun testState() = runTest {
        assertTrue(sut.getTodayState() == null)
        val mock = getMockState()
        sut.setTodayState(mock)
        assertTrue(sut.getTodayState() == mock)
    }

}