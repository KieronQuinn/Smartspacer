package com.kieronquinn.app.smartspacer.repositories

import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomDouble
import com.kieronquinn.app.smartspacer.utils.randomFloat
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomLong
import com.kieronquinn.app.smartspacer.utils.randomString
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SmartspacerSettingsRepositoryTests: BaseTest<SmartspacerSettingsRepository>() {

    override val sut by lazy {
        SmartspacerSettingsRepositoryImpl(actualContext)
    }

    @Test
    fun testBackupRestore() = runTest {
        //Randomise all values that will actually be backed up
        sut.setRandoms()
        //Create a backup of the settings
        val backup = sut.getBackup()
        //Backup should not be empty and should match the size of the backup fields list
        assertTrue(backup.isNotEmpty())
        assertTrue(backup.size == sut.getBackupFields().size)
        //Restore the backup to a new instance
        val restoreSut = SmartspacerSettingsRepositoryImpl(actualContext)
        restoreSut.restoreBackup(backup)
        //Create a backup of the new instance and verify it matches the original backup
        assertTrue(restoreSut.getBackup() == backup)
    }

    private suspend fun SmartspacerSettingsRepositoryImpl.setRandoms() {
        val backupFields = getBackupFields()
        backupFields.forEach {
            it as BaseSettingsRepository.SmartspacerSetting<Any>
            val random = when(it.type) {
                Boolean::class.java -> {
                    randomBoolean()
                }
                String::class.java -> {
                    randomString()
                }
                Long::class.java -> {
                    randomLong()
                }
                Double::class.java -> {
                    randomDouble()
                }
                Float::class.java -> {
                    randomFloat()
                }
                Integer.TYPE -> {
                    randomInt()
                }
                else -> {
                    it.type.enumConstants?.random()
                        ?: throw RuntimeException("Cannot handle type ${it.type.name} of ${it.key()}")
                }
            }
            it.set(random)
        }
    }

}