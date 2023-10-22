package com.kieronquinn.app.smartspacer.repositories

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.test.BaseTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccessibilityRepositoryTests: BaseTest<AccessibilityRepository>() {

    override val sut = AccessibilityRepositoryImpl()

    @Test
    fun testForegroundPackageStartsEmpty() = runTest {
        assertTrue(sut.foregroundPackage.value.isEmpty())
    }

    @Test
    fun testForegroundPackageUpdatesAfterCall() = runTest {
        val testPackage = "com.example.test"
        sut.foregroundPackage.test {
            assertTrue(awaitItem().isEmpty())
            sut.setForegroundPackage(testPackage)
            assertTrue(awaitItem() == testPackage)
        }
    }

}