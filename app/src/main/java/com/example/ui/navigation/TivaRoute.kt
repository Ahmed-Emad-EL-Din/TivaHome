package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class TivaRoute(
  val route: String,
  val title: String,
  val subtitle: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  DASHBOARD(
    route = "dashboard",
    title = "Home Dashboard",
    subtitle = "Relays, door & live thermal monitor",
    selectedIcon = Icons.Rounded.Dashboard,
    unselectedIcon = Icons.Outlined.Dashboard,
  ),
  DOOR_LOG(
    route = "door_log",
    title = "Door Activity Log",
    subtitle = "Timestamped open & close history",
    selectedIcon = Icons.Rounded.DoorFront,
    unselectedIcon = Icons.Outlined.DoorFront,
  ),
  BLUETOOTH(
    route = "bluetooth",
    title = "Bluetooth & Connections",
    subtitle = "Pair & manage HC-05 SPP link",
    selectedIcon = Icons.Rounded.Bluetooth,
    unselectedIcon = Icons.Outlined.Bluetooth,
  ),
  TERMINAL(
    route = "terminal",
    title = "Serial Terminal",
    subtitle = "Live 9600-baud UART packet log",
    selectedIcon = Icons.Rounded.Terminal,
    unselectedIcon = Icons.Outlined.Terminal,
  ),
  PIN_MAP(
    route = "pin_map",
    title = "Pin Map & Protocol",
    subtitle = "TM4C123GH6PM wiring & commands",
    selectedIcon = Icons.Rounded.Memory,
    unselectedIcon = Icons.Outlined.Memory,
  ),
  SETTINGS(
    route = "settings",
    title = "Settings",
    subtitle = "Limits, units, alerts & theme",
    selectedIcon = Icons.Rounded.Settings,
    unselectedIcon = Icons.Outlined.Settings,
  );

  companion object {
    fun fromRoute(route: String?): TivaRoute {
      return entries.firstOrNull { it.route == route } ?: DASHBOARD
    }
  }
}
