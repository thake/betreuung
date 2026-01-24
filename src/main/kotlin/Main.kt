import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text // Temp
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.thake.betreuung.ui.AppStateModel
import de.thake.betreuung.ui.LoginScreen
import de.thake.betreuung.ui.MainLayout
import de.thake.betreuung.ui.Screen

fun main() = application {
    val appState = remember { AppStateModel() }
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

    Window(
            onCloseRequest = ::exitApplication,
            title = "Betreuung XML Converter",
            state = windowState
    ) {
        MaterialTheme {
            when (appState.currentScreen.value) {
                Screen.LOGIN -> LoginScreen(appState)
                else ->
                        MainLayout(appState) {
                            when (appState.currentScreen.value) {
                                Screen.BETREUTEN_LIST ->
                                        de.thake.betreuung.ui.BetreutenScreen(appState)
                                Screen.MAPPING_LIST -> de.thake.betreuung.ui.MappingScreen(appState)
                                Screen.WORKFLOW -> de.thake.betreuung.ui.WorkScreen(appState)
                                else -> Text("Error")
                            }
                        }
            }
        }
    }
}
