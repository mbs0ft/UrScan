package ru.mbsoft.urscan.ui.navigation

import java.io.File

sealed class Screen {
    object Home : Screen()
    data class Scanner(val fileName: String, val existingFile: File? = null) : Screen()
    object Settings : Screen()
}
