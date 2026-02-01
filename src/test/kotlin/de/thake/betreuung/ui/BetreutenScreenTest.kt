package de.thake.betreuung.ui

import androidx.compose.ui.test.*
import de.thake.betreuung.logic.DataManager
import de.thake.betreuung.model.Betreuter
import java.io.File
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@OptIn(ExperimentalTestApi::class)
class BetreutenScreenTest {

        @Test
        fun testAddBetreuter(@TempDir tempDir: File) = runComposeUiTest {
                DataManager.rootDir = tempDir
                // Setup AppState with dummy password so it thinks it's unlocked/loaded
                val appState = AppStateModel()
                appState.password.value = "test".toCharArray()

                // Initial empty state
                appState.betreutenList.clear()

                setContent { BetreutenScreen(appState) }

                // Check if "Neuer Betreuter" FAB exists (by content description)
                onNodeWithContentDescription("Neuer Betreuter").assertExists()

                // Click Add (FAB)
                onNodeWithContentDescription("Neuer Betreuter").performClick()

                // Check if Dialog title exists
                onNodeWithText("Neuer Betreuter").assertExists()

                // Fill in Name
                onNodeWithText("Nachname").performTextInput("Neu")

                // Save
                onNodeWithText("Speichern").performClick()

                // Check if "Neu, " appears in the list
                onNodeWithText("Neu, ").assertExists()

                // Check data state directly to verify the Add action worked on the model
                assert(appState.betreutenList.size == 1)
                assert(appState.betreutenList[0].nachname == "Neu")
        }

        @Test
        fun testSelectBetreuter() = runComposeUiTest {
                val appState = AppStateModel()
                val b =
                        Betreuter(
                                UUID.randomUUID().toString(),
                                "Müller",
                                "Hans",
                                "01.01.1990",
                                "AZ1",
                                emptyList(),
                                "Berlin"
                        )
                appState.betreutenList.add(b)

                setContent { BetreutenScreen(appState) }

                // Click on the list item
                onNodeWithText("Müller, Hans").performClick()

                // Dialog should show up with title
                onNodeWithText("Betreuter bearbeiten").assertExists()

                // Verify value in text field
                onNodeWithText("Müller").assertExists()
        }

        @Test
        fun testEditFormIsScrollable() = runComposeUiTest {
                val appState = AppStateModel()
                val b =
                        Betreuter(
                                UUID.randomUUID().toString(),
                                "Müller",
                                "Hans",
                                "01.01.1990",
                                "AZ1",
                                emptyList(),
                                "Berlin"
                        )
                appState.betreutenList.add(b)

                setContent { BetreutenScreen(appState) }

                // Select the item
                onNodeWithText("Müller, Hans").performClick()

                // The edit form should be scrollable (LazyColumn).
                onNodeWithTag("edit_list").assertExists()
        }
}
