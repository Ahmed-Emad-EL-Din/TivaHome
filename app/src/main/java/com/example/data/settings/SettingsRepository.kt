package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tivahome_settings")

class SettingsRepository(private val context: Context) {

  private object Keys {
    val TEMP_LIMIT_CELSIUS = intPreferencesKey("temp_limit_celsius")
    val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
    val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    val INSECURE_FALLBACK = booleanPreferencesKey("insecure_rfcomm_fallback")
    val ALARM_SOUND = booleanPreferencesKey("alarm_sound_enabled")
    val ALARM_VIBRATION = booleanPreferencesKey("alarm_vibration_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")
    val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
  }

  val settingsFlow: Flow<AppSettings> =
    context.dataStore.data.map { prefs ->
      val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
      val themeMode =
        ThemeMode.entries.firstOrNull { it.name == themeName } ?: ThemeMode.SYSTEM
      AppSettings(
        tempLimitCelsius = (prefs[Keys.TEMP_LIMIT_CELSIUS] ?: 30).coerceIn(10, 80),
        useFahrenheit = prefs[Keys.USE_FAHRENHEIT] ?: false,
        autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: true,
        insecureRfcommFallback = prefs[Keys.INSECURE_FALLBACK] ?: true,
        alarmSoundEnabled = prefs[Keys.ALARM_SOUND] ?: true,
        alarmVibrationEnabled = prefs[Keys.ALARM_VIBRATION] ?: true,
        themeMode = themeMode,
        lastDeviceAddress = prefs[Keys.LAST_DEVICE_ADDRESS],
        lastDeviceName = prefs[Keys.LAST_DEVICE_NAME],
      )
    }

  suspend fun setTempLimitCelsius(limit: Int) {
    context.dataStore.edit { it[Keys.TEMP_LIMIT_CELSIUS] = limit.coerceIn(10, 80) }
  }

  suspend fun setUseFahrenheit(enabled: Boolean) {
    context.dataStore.edit { it[Keys.USE_FAHRENHEIT] = enabled }
  }

  suspend fun setAutoReconnect(enabled: Boolean) {
    context.dataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
  }

  suspend fun setInsecureRfcommFallback(enabled: Boolean) {
    context.dataStore.edit { it[Keys.INSECURE_FALLBACK] = enabled }
  }

  suspend fun setAlarmSoundEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.ALARM_SOUND] = enabled }
  }

  suspend fun setAlarmVibrationEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.ALARM_VIBRATION] = enabled }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun saveLastDevice(address: String, name: String?) {
    context.dataStore.edit {
      it[Keys.LAST_DEVICE_ADDRESS] = address
      if (!name.isNullOrBlank()) {
        it[Keys.LAST_DEVICE_NAME] = name
      }
    }
  }
}
