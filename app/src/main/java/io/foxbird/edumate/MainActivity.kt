package io.foxbird.edumate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edumate.data.preferences.UserPreferencesManager
import io.foxbird.edumate.feature.common.navigation.EduMateAppShell
import io.foxbird.edumate.ui.theme.EduMateLiteTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val prefsManager: UserPreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by prefsManager.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = io.foxbird.edumate.data.preferences.AppPreferences()
            )
            EduMateLiteTheme(themeMode = prefs.themeMode) {
                EduMateAppShell()
            }
        }
    }
}
