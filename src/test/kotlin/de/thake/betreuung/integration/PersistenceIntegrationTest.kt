package de.thake.betreuung.integration

import de.thake.betreuung.logic.DataManager
import de.thake.betreuung.model.Betreuter
import java.io.File
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PersistenceIntegrationTest {

    @org.junit.jupiter.api.io.TempDir lateinit var tempDir: File

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        DataManager.rootDir = tempDir
    }

    @Test
    fun testEncryptedStorage() {
        val password = "securePassword123".toCharArray()
        val wrongPassword = "wrong".toCharArray()

        val originalList =
                listOf(
                        Betreuter(
                                UUID.randomUUID().toString(),
                                "Test",
                                "User",
                                "01.01.2000",
                                "123",
                                emptyList(),
                                "City"
                        )
                )

        // Save
        DataManager.saveBetreuten(originalList, password)

        // Load with correct password
        val loadedList = DataManager.loadBetreuten(password)
        assertEquals(1, loadedList.size)
        assertEquals("Test", loadedList[0].nachname)

        // Load with wrong password should fail
        assertThrows(RuntimeException::class.java) { DataManager.loadBetreuten(wrongPassword) }
    }
}
