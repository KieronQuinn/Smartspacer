package com.kieronquinn.app.smartspacer.repositories

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import app.cash.turbine.test
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.model.doodle.DDLJson
import com.kieronquinn.app.smartspacer.model.doodle.Doodle
import com.kieronquinn.app.smartspacer.model.doodle.DoodleImage
import com.kieronquinn.app.smartspacer.model.doodle.Image
import com.kieronquinn.app.smartspacer.repositories.SearchRepositoryImpl.CachedDoodle
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Test
import java.io.File

class SearchRepositoryTests: BaseTest<SearchRepository>() {

    companion object {
        private fun getMockDoodleImage(includePrefix: Boolean = false): DoodleImage {
            //Should match https://kieronquinn.co.uk/smartspacer/doodle/async/ddljson?async=ntp:1
            val prefix = if(includePrefix){
                "https://kieronquinn.co.uk/smartspacer/doodle/"
            }else ""
            return DoodleImage(
                "$prefix/logos/doodles/2022/seasonal-holidays-2022-6753651837109831.6-l.png",
                null,
                "$prefix/search?q=Seasonal+Holidays&oi=ddle&ct=248061952&hl=en-GB",
                "Seasonal Holidays 2022",
                16
            )
        }

        private fun getMockDoodle(mockImage: DoodleImage): Doodle {
            return Doodle(
                DDLJson(
                    mockImage.altText!!,
                    Image(mockImage.url),
                    null,
                    mockImage.searchUrl!!,
                    1114852000
                )
            )
        }

        private fun getMockCachedDoodle(
            mockImage: DoodleImage = getMockDoodleImage()
        ): CachedDoodle {
            return CachedDoodle(getMockDoodle(mockImage), 0L)
        }
    }

    private val expandedSearchPackageMock = mockSmartspacerSetting("")
    private val onPackageChangedMock = MutableStateFlow(System.currentTimeMillis())
    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder().build()

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { expandedSearchPackage } returns expandedSearchPackageMock
    }

    private val packageRepositoryMock = mock<PackageRepository> {
        every { onPackageChanged(any<CoroutineScope>(), any()) } returns onPackageChangedMock
    }

    override val sut by lazy {
        SearchRepositoryImpl(
            actualContext, //Required for local file access
            packageRepositoryMock,
            settingsRepositoryMock,
            gson,
            okHttpClient,
            scope,
            packageManagerMock
        )
    }

    override fun setup() {
        super.setup()
        //Static doodle is hosted as a mock, since they change daily on prod.
        SearchRepositoryImpl.BASE_URL = "https://kieronquinn.co.uk/smartspacer/doodle/"
        every { packageManagerMock.queryIntentActivities(any(), any<ResolveInfoFlags>()) } answers {
            when (firstArg<Intent>().`package`) {
                //Invalid search package
                "com.example.invalid" -> {
                    emptyList<ResolveInfo>()
                }
                //Valid search package
                "com.example.valid" -> {
                    listOf(mock())
                }
                //Not provided = doing a search for all
                null -> {
                    listOf(
                        ResolveInfo().apply {
                            activityInfo = ActivityInfo().apply {
                                packageName = "com.example.default"
                            }
                        },
                        ResolveInfo().apply {
                            activityInfo = ActivityInfo().apply {
                                packageName = "com.example.valid"
                            }
                        }
                    )
                }
                else -> {
                    emptyList<ResolveInfo>()
                }
            }
        }
    }

    @Test
    fun testExpandedSearchApp() = runTest {
        sut.expandedSearchApp.test {
            assertTrue(awaitItem()!!.packageName == "com.example.default")
            expandedSearchPackageMock.emit("com.example.valid")
            assertTrue(awaitItem()!!.packageName == "com.example.valid")
            expandedSearchPackageMock.emit("com.example.invalid")
            assertTrue(awaitItem()!!.packageName == "com.example.default")
        }
    }

    @Test
    fun testGetDoodle() = runTest {
        val cacheFile = File(actualContext.cacheDir, "doodle.json")
        cacheFile.delete()
        assertTrue(sut.getDoodle() == getMockDoodleImage(true))
        val mockCache = getMockCachedDoodle()
        val actualCache = gson.fromJson(cacheFile.readText(), CachedDoodle::class.java)
        assertTrue(mockCache.doodle == actualCache.doodle)
        assertFalse(actualCache.hasExpired())
        cacheFile.delete()
        //Write an expired cache to file and verify it is not used
        val variantImage = getMockDoodleImage().copy(
            altText = "Expired Cached Doodle"
        )
        val expiredCache = getMockCachedDoodle(variantImage)
        assertTrue(expiredCache.hasExpired())
        cacheFile.writeText(gson.toJson(expiredCache))
        assertTrue(sut.getDoodle() == getMockDoodleImage(true))
    }

    @Test
    fun testGetAllSearchApps() = runTest {
        val apps = sut.getAllSearchApps()
        val mock = listOf("com.example.default", "com.example.valid")
        assertTrue(apps.map { it.packageName } == mock)
    }

}