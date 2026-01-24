package de.thake.betreuung.ui

import androidx.compose.ui.test.*
import de.thake.betreuung.model.Betreuter
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class BetreutenScreenTest {

    @Test
    fun testAddBetreuter() = runComposeUiTest {
        // Setup AppState with dummy password so it thinks it's unlocked/loaded
        val appState = AppStateModel()
        appState.password.value = "test".toCharArray()

        // Initial empty state
        appState.betreutenList.clear()

        setContent { BetreutenScreen(appState) }

        // Check if "Neuer Betreuter" button exists
        onNodeWithText("Neuer Betreuter").assertExists()

        // Click Add
        onNodeWithText("Neuer Betreuter").performClick()

        // Check if "Neu" (default name) appears in the list
        onNodeWithText("Neu, ").assertExists()

        // Check if Detail form appears (e.g., "Nachname" field)
        onNodeWithText("Nachname").assertExists()

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

        // Details should show up
        onNodeWithText("Details bearbeiten").assertExists()

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

        // The edit form should be scrollable.
        // We look for a container that has a scroll action and contains the "Details bearbeiten"
        // text or similar.
        // Currently, it's just a Column in a Box, so this should fail.
        onNode(hasScrollAction().and(hasAnyDescendant(hasText("Details bearbeiten"))))
                .assertExists()
    }
}
