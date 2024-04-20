package com.kieronquinn.app.smartspacer.repositories

import android.graphics.Bitmap
import com.kieronquinn.app.smartspacer.test.BaseTest
import junit.framework.TestCase.assertTrue
import org.junit.Test

class AtAGlanceRepositoryTests: BaseTest<AtAGlanceRepository>() {

    override val sut by lazy {
        AtAGlanceRepositoryImpl()
    }

    @Test
    fun testState() {
        assertTrue(sut.getStates().isEmpty())
        val mockState = listOf(AtAGlanceRepository.State(
            "Title",
            "Subtitle",
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8),
            "Content Description"
        ))
        sut.setStates(mockState)
        assertTrue(sut.getStates() == mockState)
    }

}