package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.settings.AppSettings
import com.example.data.settings.ThemeMode
import com.example.ui.theme.TivaTheme
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  settings: AppSettings,
  isConnected: Boolean,
  onApplyTempLimit: (Int) -> Unit,
  onToggleFahrenheit: (Boolean) -> Unit,
  onToggleAutoReconnect: (Boolean) -> Unit,
  onToggleInsecureFallback: (Boolean) -> Unit,
  onToggleAlarmSound: (Boolean) -> Unit,
  onToggleAlarmVibration: (Boolean) -> Unit,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onTestAlert: () -> Unit,
  onClearDoorHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  var draftLimit by remember { mutableIntStateOf(settings.tempLimitCelsius) }
  var showClearDialog by remember { mutableStateOf(false) }

  LaunchedEffect(settings.tempLimitCelsius) { draftLimit = settings.tempLimitCelsius }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
      text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            onClearDoorHistory()
            showClearDialog = false
          },
          colors = ButtonDefaults.textButtonColors(contentColor = ext.alarmCrimson),
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp),
    ) {
      // 1. Temperature & Units
      SettingsSectionCard(icon = Icons.Rounded.Thermostat, title = "Temperature & Threshold") {
        SettingSwitchRow(
          title = "Display in Fahrenheit (°F)",
          subtitle = "Board protocol LIMIT:<int> always sends whole °C",
          checked = settings.useFahrenheit,
          onCheckedChange = onToggleFahrenheit,
          testTag = "setting_fahrenheit_switch",
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Thermal Alert Limit", style = MaterialTheme.typography.titleSmall)
          Text(
            text = "${draftLimit}°C",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = draftLimit.toFloat(),
          onValueChange = { draftLimit = it.roundToInt().coerceIn(10, 80) },
          valueRange = 15f..60f,
          steps = 44,
        )
        Row(
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Button(
            onClick = { onApplyTempLimit(draftLimit) },
            enabled = draftLimit != settings.tempLimitCelsius || isConnected,
            modifier = Modifier.testTag("settings_apply_limit_button"),
          ) {
            Text(if (isConnected) "Apply & Sync LIMIT:$draftLimit" else "Save Limit")
          }
        }
      }

      // 2. Thermal Alert Feedback
      SettingsSectionCard(icon = Icons.Rounded.NotificationsActive, title = "Thermal Alert Feedback") {
        SettingSwitchRow(
          title = "Alert Chime Sound",
          subtitle = "Play audible alarm tone when room temp exceeds limit",
          checked = settings.alarmSoundEnabled,
          onCheckedChange = onToggleAlarmSound,
          testTag = "setting_sound_switch",
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
          title = "Haptic Vibration Pulse",
          subtitle = "Vibrate phone when high temperature alarm activates",
          checked = settings.alarmVibrationEnabled,
          onCheckedChange = onToggleAlarmVibration,
          testTag = "setting_vibration_switch",
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = onTestAlert,
          modifier = Modifier.testTag("settings_test_alert_button"),
        ) {
          Text("Test Sound & Vibration")
        }
      }

      // 3. Bluetooth Connection Behavior
      SettingsSectionCard(icon = Icons.Rounded.Bluetooth, title = "Bluetooth & HC-05 Link") {
        SettingSwitchRow(
          title = "Auto-Reconnect on Link Drop",
          subtitle = "Automatically retry SPP connection with exponential backoff",
          checked = settings.autoReconnect,
          onCheckedChange = onToggleAutoReconnect,
          testTag = "setting_autoreconnect_switch",
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
          title = "RFCOMM Compatibility Fallback",
          subtitle = "Allow insecure SPP & channel-1 fallback for HC-05 clones",
          checked = settings.insecureRfcommFallback,
          onCheckedChange = onToggleInsecureFallback,
          testTag = "setting_fallback_switch",
        )
      }

      // 4. Theme Appearance
      SettingsSectionCard(icon = Icons.Rounded.Palette, title = "Appearance") {
        Text(
          text = "Theme Mode",
          style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
              selected = settings.themeMode == mode,
              onClick = { onSelectThemeMode(mode) },
              shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
              modifier = Modifier.testTag("setting_theme_${mode.name.lowercase()}"),
            ) {
              Text(mode.label)
            }
          }
        }
      }

      // 5. Local Storage Management
      SettingsSectionCard(icon = Icons.Rounded.DeleteOutline, title = "Local Storage & Privacy") {
        Text(
          text =
            "Door opening and closing timestamps are stored locally on your device in Room. TivaHome runs 100% offline over Bluetooth SPP with no API keys, accounts, or cloud services.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = { showClearDialog = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.alarmCrimson),
          modifier = Modifier.testTag("settings_clear_door_log_button"),
        ) {
          Text("Clear Door Activity History")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SettingsSectionCard(
  icon: ImageVector,
  title: String,
  content: @Composable () -> Unit,
) {
  val ext = TivaTheme.extendedColors
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, ext.cardSubtleBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun SettingSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
