package com.kieronquinn.app.smartspacer.repositories

import android.content.ComponentName
import android.content.ContentProviderClient
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.model.smartspace.ActionHolder
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.extensions.getUniqueId
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 *  Assumes device running tests is running AOSP Android, with native Smartspace, and exactly one
 *  OEM-supporting launcher installed:
 *  https://play.google.com/store/apps/details?id=amirz.rootless.nexuslauncher.
 *
 *  This is due to complex APK loading for manifest information, which would be very difficult to
 *  mock.
 */
@Suppress("CloseTarget", "CloseAction")
class OemSmartspacerRepositoryTests: BaseTest<OemSmartspacerRepository>() {

    companion object {
        private fun getMockTargets(context: Context, makeUnique: Boolean): List<TargetHolder> {
            val packageName = "com.example.one"
            return listOf(
                TargetHolder(
                    getMockTarget(context, packageName),
                    listOf(getMockSmartspaceTarget(packageName, makeUnique, 0))
                ),
                TargetHolder(
                    getMockTarget(context, packageName),
                    listOf(getMockSmartspaceTarget(packageName, makeUnique, 1))
                ),
                TargetHolder(
                    getMockTarget(context, packageName),
                    listOf(getMockSmartspaceTarget(packageName, makeUnique, 2))
                )
            )
        }

        private fun getMockTarget(context: Context, packageName: String): Target {
            return Target(context, randomString(), randomString(), sourcePackage = packageName)
        }

        private fun getMockSmartspaceTarget(
            packageName: String,
            makeUnique: Boolean,
            id: Int
        ): SmartspaceTarget {
             return SmartspaceTarget(
                 if(makeUnique) "smartspacer_${packageName}_$id" else id.toString(),
                 componentName = ComponentName(randomString(), randomString()),
                 featureType = SmartspaceTarget.FEATURE_UNDEFINED
             )
        }

        private fun getMockActions(context: Context, makeUnique: Boolean): List<ActionHolder> {
            val packageName = "com.example.one"
            return listOf(
                ActionHolder(
                    getMockAction(context, packageName),
                    listOf(getMockSmartspaceAction(packageName, makeUnique, 0))
                ),
                ActionHolder(
                    getMockAction(context, packageName),
                    listOf(getMockSmartspaceAction(packageName, makeUnique, 1))
                ),
                ActionHolder(
                    getMockAction(context, packageName),
                    listOf(getMockSmartspaceAction(packageName, makeUnique, 2))
                )
            )
        }

        private fun getMockAction(context: Context, packageName: String): Action {
            return Action(context, randomString(), randomString(), sourcePackage = packageName)
        }

        private fun getMockSmartspaceAction(
            packageName: String,
            makeUnique: Boolean,
            id: Int
        ): SmartspaceAction {
             return SmartspaceAction(
                 if(makeUnique) "smartspacer_${packageName}_$id" else id.toString(),
                 title = randomString()
             )
        }

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun createMockContentProviderClient() = mock<ContentProviderClient> {
            every { call("get_targets_config", any(), any()) } answers {
                SmartspacerTargetProvider.Config(
                    randomString(),
                    randomString(),
                    DUMMY_ICON
                ).toBundle()
            }
            every { call("get_actions_config", any(), any()) } answers {
                SmartspacerComplicationProvider.Config(
                    randomString(),
                    randomString(),
                    DUMMY_ICON
                ).toBundle()
            }
        }
    }

    private val smartspaceRepositoryMock = mock<SmartspaceRepository>()
    private val grantRepositoryMock = mock<GrantRepository>()
    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()
    private val packageRepositoryMock = mock<PackageRepository>()
    private val contentProviderClient = mock<ContentProviderClient>()

    private val onPackageChanged = MutableSharedFlow<String>()

    override val sut by lazy {
        createSut()
    }

    override fun Context.context() {
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } answers {
            createMockContentProviderClient()
        }
        every {
            contentResolver.acquireUnstableContentProviderClient(any<String>())
        } answers {
            createMockContentProviderClient()
        }
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            createMockContentProviderClient()
        }
    }

    override fun setup() {
        super.setup()
        every { packageRepositoryMock.onPackageChanged } returns onPackageChanged
    }

    @Test
    fun testToken() {
        val sutToken = sut.token
        val otherSutToken = createSut().token
        //Token between two instances should not match
        assertFalse(sutToken == otherSutToken)
    }

    @Test
    fun testGetCompatibleApps() = runTest {
        sut.getCompatibleApps().test {
            val packages = awaitItem().map { it.packageName }
            assertTrue(packages.contains("amirz.rootless.nexuslauncher"))
        }
    }

    @Test
    fun testGetSmartspaceTarget() = runTest {
        val uniqueTargets = getMockTargets(contextMock, false)
        val rawTargets = getMockTargets(contextMock, true)
        every { smartspaceRepositoryMock.targets } returns MutableStateFlow(uniqueTargets)
        val mock = rawTargets.first().targets!!.first()
        val id = rawTargets.first().targets!!.first().smartspaceTargetId
        val actual = sut.getSmartspaceTarget(id)
        val uniqueId = actual!!.getUniqueId(rawTargets.first().parent)
        assertTrue(uniqueId == mock.smartspaceTargetId)
    }

    @Test
    fun textGetSmartspaceAction() = runTest {
        val uniqueActions = getMockActions(contextMock, false)
        val rawActions = getMockActions(contextMock, true)
        every { smartspaceRepositoryMock.actions } returns MutableStateFlow(uniqueActions)
        val mock = rawActions.first().actions!!.first()
        val id = rawActions.first().actions!!.first().id
        val actual = sut.getSmartspaceAction(id)
        val uniqueId = actual!!.getUniqueId(rawActions.first().parent)
        assertTrue(uniqueId == mock.id)
    }

    private fun createSut(): OemSmartspacerRepositoryImpl {
        return OemSmartspacerRepositoryImpl(
            smartspaceRepositoryMock,
            grantRepositoryMock,
            settingsRepositoryMock,
            actualContext, //Required for package queries
            packageRepositoryMock,
            scope
        )
    }

}