package de.thake.betreuung.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import java.io.File
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {

    @Test
    fun testLoginScreenStructure() = runComposeUiTest {
        val appState = AppStateModel()

        // Ensure clean state for test if possible
        val fileExists = File("betreuten.dat").exists()
        val expectedButtonText = if (fileExists) "Öffnen" else "Erstellen"

        setContent { LoginScreen(appState) }

        onNodeWithText("Passwort").assertExists()
        onNodeWithText(expectedButtonText).assertExists()
    }

    @Test
    fun testLoginOnEnter() = runComposeUiTest {
        val appState = AppStateModel()
        val file = File("betreuten.dat")
        if (file.exists()) file.delete()

        setContent { LoginScreen(appState) }

        onNodeWithText("Passwort").performTextInput("secure123")
        onNodeWithText("Passwort").performImeAction()

        // Should navigate to BETREUTEN_LIST
        assert(appState.currentScreen.value == Screen.BETREUTEN_LIST) {
            "Expected screen to be BETREUTEN_LIST but was ${appState.currentScreen.value}"
        }

        if (file.exists()) file.delete()
    }
}
