package de.thake.betreuung.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import de.thake.betreuung.model.Betreuter
import de.thake.betreuung.model.MappingProfile

enum class Screen {
    LOGIN,
    BETREUTEN_LIST,
    MAPPING_LIST,
    WORKFLOW
}

class AppStateModel {
    var currentScreen = mutableStateOf(Screen.LOGIN)
    var password = mutableStateOf(CharArray(0))
    
    // Data
    var betreutenList = mutableStateListOf<Betreuter>()
    var mappingsList = mutableStateListOf<MappingProfile>()
    
    // Error/Status
    var globalError = mutableStateOf<String?>(null)
    
    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
    }
}
