package com.example.data.settings

enum class ThemeMode(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark"),
}

data class AppSettings(
  val tempLimitCelsius: Int = 30,
  val useFahrenheit: Boolean = false,
  val autoReconnect: Boolean = true,
  val insecureRfcommFallback: Boolean = true,
  val alarmSoundEnabled: Boolean = true,
  val alarmVibrationEnabled: Boolean = true,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val lastDeviceAddress: String? = null,
  val lastDeviceName: String? = null,
)
