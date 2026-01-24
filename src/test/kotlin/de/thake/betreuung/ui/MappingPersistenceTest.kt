package de.thake.betreuung.ui

import de.thake.betreuung.logic.DataManager
import de.thake.betreuung.model.*
import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MappingPersistenceTest {

    @TempDir lateinit var tempDir: File

    @AfterEach
    fun tearDown() {
        DataManager.rootDir = File(".")
    }

    @Test
    fun `test persists defaultMappingId`() {
        DataManager.rootDir = tempDir
        val password = "test".toCharArray()

        // 1. Setup initial data
        val account = BankAccount(id = "acc1", iban = "DE123", bankName = "TestBank")
        val betreuter =
                Betreuter(
                        id = "b1",
                        nachname = "Doe",
                        vorname = "John",
                        geburtsdatum = "01.01.1980",
                        aktenzeichen = "AZ1",
                        accounts = listOf(account),
                        wohnort = "City"
                )
        val mapping = MappingProfile(id = "map1", name = "TestMap", columnMapping = emptyMap())

        // Simulate saving updated account with default mapping
        val updatedAccount = account.copy(defaultMappingId = mapping.id)
        val updatedBetreuter = betreuter.copy(accounts = listOf(updatedAccount))

        DataManager.saveBetreuten(listOf(updatedBetreuter), password)

        // 2. Load and verify
        val loaded = DataManager.loadBetreuten(password)
        assertEquals(1, loaded.size)
        val loadedAccount = loaded[0].accounts[0]
        assertEquals("map1", loadedAccount.defaultMappingId)
    }
}
