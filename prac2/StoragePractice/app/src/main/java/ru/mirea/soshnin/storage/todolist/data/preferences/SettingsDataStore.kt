package ru.mirea.soshnin.storage.todolist.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "todo_settings")

class SettingsDataStore(private val context: Context) {
    private val completedColorKey = booleanPreferencesKey("completed_color")
    private val importedKey = booleanPreferencesKey("json_imported")

    // переключатель "Цвет завершенных"
    val completedColor: Flow<Boolean> =
        context.dataStore.data.map { it[completedColorKey] ?: true }

    suspend fun setCompletedColor(enabled: Boolean) {
        context.dataStore.edit { it[completedColorKey] = enabled }
    }

    suspend fun isImported(): Boolean =
        context.dataStore.data.first()[importedKey] ?: false

    suspend fun setImported() {
        context.dataStore.edit { it[importedKey] = true }
    }
}
