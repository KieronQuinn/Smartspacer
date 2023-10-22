package com.kieronquinn.app.smartspacer.ui.screens.complications.requirements

import com.kieronquinn.app.smartspacer.model.database.Action
import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("CloseAction", "CloseRequirement")
class ComplicationsRequirementsViewModelTests: BaseTest<ComplicationsRequirementsViewModel>() {

    private val mockAction = Action(
        randomString(),
        randomString(),
        randomInt(),
        randomString()
    )

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getActionById(any()) } returns flowOf(mockAction)
    }

    override val sut by lazy {
        ComplicationsRequirementsViewModelImpl(
            databaseRepositoryMock,
            scope,
            Dispatchers.Main
        )
    }

    @Test
    fun testCurrentPage() = runTest {
        assertTrue(sut.getCurrentPage() == 0)
        sut.setCurrentPage(1)
        assertTrue(sut.getCurrentPage() == 1)
    }

    @Test
    fun testAddRequirementAny() = runTest {
        val complicationId = randomString()
        val pageType = PageType.ANY
        val authority = randomString()
        val id = randomString()
        val packageName = randomString()
        sut.addRequirement(
            complicationId,
            pageType,
            authority,
            id,
            packageName
        )
        coVerify {
            databaseRepositoryMock.addRequirement(Requirement(id, authority, packageName, false))
        }
        coVerify {
            val action = mockAction.copy(anyRequirements = setOf(id))
            databaseRepositoryMock.updateAction(action)
        }
    }

    @Test
    fun testAddRequirementAll() = runTest {
        val complicationId = randomString()
        val pageType = PageType.ALL
        val authority = randomString()
        val id = randomString()
        val packageName = randomString()
        sut.addRequirement(
            complicationId,
            pageType,
            authority,
            id,
            packageName
        )
        coVerify {
            databaseRepositoryMock.addRequirement(Requirement(id, authority, packageName, false))
        }
        coVerify {
            val action = mockAction.copy(allRequirements = setOf(id))
            databaseRepositoryMock.updateAction(action)
        }
    }

}