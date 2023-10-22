package com.kieronquinn.app.smartspacer.repositories

import android.app.WallpaperColors
import android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT
import android.app.WallpaperManager
import android.app.WallpaperManager.OnColorsChangedListener
import android.content.Context
import android.graphics.Color
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WallpaperRepositoryTests: BaseTest<WallpaperRepository>() {

    companion object {
        private fun createWallpaperColours(supportsDarkText: Boolean): WallpaperColors {
            val flag = if(supportsDarkText){
                0 or HINT_SUPPORTS_DARK_TEXT
            }else 0
            return WallpaperColors(
                Color.valueOf(Color.TRANSPARENT),
                Color.valueOf(Color.TRANSPARENT),
                Color.valueOf(Color.TRANSPARENT),
                flag
            )
        }
    }

    private val colorsChangedListeners = ArrayList<OnColorsChangedListener>()

    private var homeColours = createWallpaperColours(true)
    private var lockColours = createWallpaperColours(true)

    private val wallpaperManagerMock = mock<WallpaperManager> {
        every { addOnColorsChangedListener(any(), any()) } answers {
            colorsChangedListeners.add(firstArg())
        }
        every { removeOnColorsChangedListener(any()) } answers {
            colorsChangedListeners.remove(firstArg())
        }
        every { getWallpaperColors(WallpaperManager.FLAG_SYSTEM) } answers {
            homeColours
        }
        every { getWallpaperColors(WallpaperManager.FLAG_LOCK) } answers {
            lockColours
        }
    }

    override val sut by lazy {
        WallpaperRepositoryImpl(contextMock)
    }

    override fun Context.context() {
        every { getSystemService(Context.WALLPAPER_SERVICE) } returns wallpaperManagerMock
    }

    @Test
    fun testLockscreenWallpaperDarkTextColour() = runTest {
        sut.lockscreenWallpaperDarkTextColour.test {
            assertTrue(awaitItem())
            lockColours = createWallpaperColours(false)
            notifyColoursChanged(lockColours, 0 or WallpaperManager.FLAG_LOCK)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testHomescreenWallpaperDarkTextColour() = runTest {
        sut.homescreenWallpaperDarkTextColour.test {
            assertTrue(awaitItem())
            homeColours = createWallpaperColours(false)
            notifyColoursChanged(homeColours, 0 or WallpaperManager.FLAG_SYSTEM)
            assertFalse(awaitItem())
        }
    }

    private fun notifyColoursChanged(colours: WallpaperColors, which: Int) {
        colorsChangedListeners.forEach {
            it.onColorsChanged(colours, which)
        }
    }

}