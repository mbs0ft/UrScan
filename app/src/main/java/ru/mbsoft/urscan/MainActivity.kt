package ru.mbsoft.urscan

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ru.mbsoft.urscan.data.FileManager
import ru.mbsoft.urscan.data.SettingsRepository
import ru.mbsoft.urscan.scanner.SoundManager
import ru.mbsoft.urscan.ui.home.HomeScreen
import ru.mbsoft.urscan.ui.navigation.Screen
import ru.mbsoft.urscan.ui.scanner.ScannerScreen
import ru.mbsoft.urscan.ui.settings.SettingsScreen
import ru.mbsoft.urscan.ui.theme.UrScanTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var fileManager: FileManager
    private lateinit var soundManager: SoundManager

    override fun attachBaseContext(newBase: Context) {
        val repo = SettingsRepository(newBase)
        super.attachBaseContext(repo.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepo = SettingsRepository(this)
        fileManager = FileManager(this)
        soundManager = SoundManager(this)

        setContent {
            UrScanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                    BackHandler(enabled = currentScreen !is Screen.Home) {
                        currentScreen = Screen.Home
                    }

                    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                        when (screen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    fileManager = fileManager,
                                    onOpenScanner = { fileName, existingFile ->
                                        currentScreen = Screen.Scanner(fileName, existingFile)
                                    },
                                    onOpenSettings = {
                                        currentScreen = Screen.Settings
                                    }
                                )
                            }
                            is Screen.Scanner -> {
                                ScannerScreen(
                                    fileName = screen.fileName,
                                    existingFile = screen.existingFile,
                                    fileManager = fileManager,
                                    settingsRepo = settingsRepo,
                                    soundManager = soundManager,
                                    onBack = { currentScreen = Screen.Home }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    settingsRepo = settingsRepo,
                                    onBack = { currentScreen = Screen.Home },
                                    onLanguageChanged = {
                                        recreate()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
