package io.foxbird.edumate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.foxbird.edumate.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val extractConcepts: Boolean = true,
    val developerMode: Boolean = false,
    val onboardingComplete: Boolean = false,
    /** ID of the last active inference model — restored on next launch. Empty = use default. */
    val activeInferenceModelId: String = ""
)

class UserPreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EXTRACT_CONCEPTS = booleanPreferencesKey("extract_concepts")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val ACTIVE_INFERENCE_MODEL_ID = stringPreferencesKey("active_inference_model_id")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            extractConcepts = prefs[Keys.EXTRACT_CONCEPTS] ?: true,
            developerMode = prefs[Keys.DEVELOPER_MODE] ?: false,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            activeInferenceModelId = prefs[Keys.ACTIVE_INFERENCE_MODEL_ID] ?: ""
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setExtractConcepts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EXTRACT_CONCEPTS] = enabled }
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEVELOPER_MODE] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setActiveInferenceModelId(id: String) {
        context.dataStore.edit { it[Keys.ACTIVE_INFERENCE_MODEL_ID] = id }
    }
}
