package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReaderViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "bookshelf_theme_prefs")

class ThemePreferencesStore(private val context: Context) {

    private val appThemeKey = stringPreferencesKey("app_theme")
    private val readerThemeKey = stringPreferencesKey("reader_theme")
    private val languageTagKey = stringPreferencesKey("preferred_language_tag")
    private val readerViewModeKey = stringPreferencesKey("reader_view_mode")
    private val pendingProfileNameKey = stringPreferencesKey("pending_profile_name")

    val appThemeFlow: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        AppThemeMode.fromRaw(prefs[appThemeKey])
    }

    val readerThemeFlow: Flow<ReaderThemeMode> = context.themeDataStore.data.map { prefs ->
        ReaderThemeMode.fromRaw(prefs[readerThemeKey])
    }

    val languageTagFlow: Flow<String> = context.themeDataStore.data.map { prefs ->
        prefs[languageTagKey] ?: "en"
    }

    val readerViewModeFlow: Flow<ReaderViewMode> = context.themeDataStore.data.map { prefs ->
        ReaderViewMode.fromRaw(prefs[readerViewModeKey])
    }

    val pendingProfileNameFlow: Flow<String?> = context.themeDataStore.data.map { prefs ->
        prefs[pendingProfileNameKey]?.trim()?.takeIf { it.isNotEmpty() }
    }

    suspend fun setAppTheme(mode: AppThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[appThemeKey] = mode.name
        }
    }

    suspend fun setReaderTheme(mode: ReaderThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[readerThemeKey] = mode.name
        }
    }

    suspend fun setPreferredLanguageTag(tag: String) {
        context.themeDataStore.edit { prefs ->
            prefs[languageTagKey] = tag.ifBlank { "en" }
        }
    }

    suspend fun setReaderViewMode(mode: ReaderViewMode) {
        context.themeDataStore.edit { prefs ->
            prefs[readerViewModeKey] = mode.name
        }
    }

    suspend fun setPendingProfileName(name: String?) {
        context.themeDataStore.edit { prefs ->
            val clean = name?.trim()?.take(80)
            if (clean.isNullOrEmpty()) {
                prefs.remove(pendingProfileNameKey)
            } else {
                prefs[pendingProfileNameKey] = clean
            }
        }
    }
}
