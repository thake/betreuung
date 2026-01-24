package de.thake.betreuung.logic

import de.thake.betreuung.model.Betreuter
import de.thake.betreuung.model.MappingProfile
import de.thake.betreuung.util.SecurityUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DataManager {
    private val logger = KotlinLogging.logger {}
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    var rootDir: File = File(".")
    private val betreuterFile
        get() = File(rootDir, "betreuten.dat")
    private val mappingsFile
        get() = File(rootDir, "mappings.json")

    fun saveBetreuten(betreute: List<Betreuter>, password: CharArray) {
        val jsonString = json.encodeToString(betreute)
        val encrypted = SecurityUtils.encrypt(jsonString, password)
        betreuterFile.writeText(encrypted)
    }

    fun loadBetreuten(password: CharArray): List<Betreuter> {
        if (!betreuterFile.exists()) return emptyList()
        try {
            val encrypted = betreuterFile.readText()
            val decrypted = SecurityUtils.decrypt(encrypted, password)
            return json.decodeFromString(decrypted)
        } catch (e: Exception) {
            logger.error(e) { "Failed to load Betreuten" }
            throw RuntimeException("Failed to load Betreuten. Wrong password or corrupted file.", e)
            // In a real app we might want specific error types
        }
    }

    fun hasBetreutenFile(): Boolean = betreuterFile.exists()

    fun saveMappings(mappings: List<MappingProfile>) {
        val jsonString = json.encodeToString(mappings)
        mappingsFile.writeText(jsonString)
    }

    fun loadMappings(): List<MappingProfile> {
        if (!mappingsFile.exists()) return emptyList()
        return try {
            json.decodeFromString(mappingsFile.readText())
        } catch (e: Exception) {
            logger.error(e) { "Failed to load mappings" }
            emptyList()
        }
    }
}
