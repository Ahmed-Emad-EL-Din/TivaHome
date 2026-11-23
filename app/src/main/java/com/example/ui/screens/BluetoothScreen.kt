package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.BluetoothDeviceItem
import com.example.bluetooth.BtConnectionState
import com.example.ui.theme.TivaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BluetoothScreen(
  isBluetoothSupported: Boolean,
  isBluetoothEnabled: Boolean,
  hasPermissions: Boolean,
  requiredPermissions: Array<String>,
  connectionState: BtConnectionState,
  pairedDevices: List<BluetoothDeviceItem>,
  discoveredDevices: List<BluetoothDeviceItem>,
  isScanning: Boolean,
  lastPacketMillis: Long?,
  onPermissionsResult: () -> Unit,
  onRefreshState: () -> Unit,
  onStartScan: () -> Unit,
  onStopScan: () -> Unit,
  onConnectDevice: (BluetoothDeviceItem) -> Unit,
  onDisconnect: () -> Unit,
  onSyncStatus: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val ext = TivaTheme.extendedColors
  val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      onPermissionsResult()
    }

  val enableBtLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      onRefreshState()
    }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
  ) {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
    ) {
      // 1. Hardware / Permission Gate Cards
      if (!isBluetoothSupported) {
        item {
          StatusGateCard(
            icon = Icons.Rounded.BluetoothDisabled,
            title = "Bluetooth Unavailable",
            body =
              "This device or emulator does not expose a Classic Bluetooth adapter. Install on a physical Android phone to pair with the HC-05 module.",
            accent = ext.alarmCrimson,
            buttonLabel = null,
            onButtonClick = {},
          )
        }
      } else if (!hasPermissions) {
        item {
          StatusGateCard(
            icon = Icons.Rounded.Security,
            title = "Bluetooth Permission Required",
            body =
              "Grant Bluetooth Connect & Scan permissions so TivaHome can discover and communicate with your HC-05 module at 9600 baud.",
            accent = ext.warningAmber,
            buttonLabel = "Grant Permissions",
            onButtonClick = { permissionLauncher.launch(requiredPermissions) },
          )
        }
      } else if (!isBluetoothEnabled) {
        item {
          StatusGateCard(
            icon = Icons.Rounded.BluetoothDisabled,
            title = "Bluetooth Is Turned Off",
            body = "Turn on Bluetooth to view paired HC-05 modules and connect to your TM4C123 board.",
            accent = ext.warningAmber,
            buttonLabel = "Turn On Bluetooth",
            onButtonClick = {
              runCatching {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
              }
            },
          )
        }
      }

      // 2. Active Connection Diagnostics Card
      if (connectionState is BtConnectionState.Connected) {
        item {
          Surface(
            color = ext.safeContainer.copy(alpha = 0.45f),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.25.dp, ext.safeEmerald.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().testTag("active_connection_card"),
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.BluetoothConnected,
                  contentDescription = null,
                  tint = ext.safeEmerald,
                  modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = connectionState.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${connectionState.address} • ${connectionState.transportLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              val rxText =
                lastPacketMillis?.let { "Last RX ${timeFormat.format(Date(it))}" }
                  ?: "No frames received yet"
              Text(
                text = "Serial Profile: SPP 9600 baud (8N1) • $rxText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                FilledTonalButton(
                  onClick = onSyncStatus,
                  modifier = Modifier.weight(1f).testTag("bt_sync_status_button"),
                ) {
                  Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.action_sync_status))
                }
                OutlinedButton(
                  onClick = onDisconnect,
                  modifier = Modifier.weight(1f).testTag("bt_disconnect_button"),
                ) {
                  Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.action_disconnect))
                }
              }
            }
          }
        }
      }

      // 3. Scan & System Bluetooth Controls Row
      if (isBluetoothSupported && hasPermissions && isBluetoothEnabled) {
        item {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = { if (isScanning) onStopScan() else onStartScan() },
              modifier = Modifier.weight(1f).testTag("bt_scan_toggle_button"),
            ) {
              if (isScanning) {
                CircularProgressIndicator(
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_stop_scan))
              } else {
                Icon(
                  Icons.Rounded.BluetoothSearching,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_scan_devices))
              }
            }

            OutlinedButton(
              onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
              },
              modifier = Modifier.testTag("bt_system_settings_button"),
            ) {
              Icon(
                Icons.Rounded.SettingsBluetooth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pair in OS")
            }
          }
        }

        // 4. Paired Devices Header
        item {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          ) {
            Text(
              text = "PAIRED DEVICES (${pairedDevices.size})",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "PIN usually 1234 or 0000",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (pairedDevices.isEmpty()) {
          item {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = MaterialTheme.shapes.medium,
              border = BorderStroke(1.dp, ext.cardSubtleBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text =
                  "No paired Bluetooth devices found. Tap 'Scan Nearby' or 'Pair in OS' to pair your HC-05 module first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        } else {
          items(items = pairedDevices, key = { "paired_${it.address}" }) { device ->
            BluetoothDeviceRow(
              device = device,
              connectionState = connectionState,
              onClick = { onConnectDevice(device) },
            )
          }
        }

        // 5. Discovered Devices Section
        if (discoveredDevices.isNotEmpty() || isScanning) {
          item {
            Text(
              text = "NEARBY DISCOVERED (${discoveredDevices.size})",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 6.dp),
            )
          }
          items(items = discoveredDevices, key = { "disc_${it.address}" }) { device ->
            BluetoothDeviceRow(
              device = device,
              connectionState = connectionState,
              onClick = { onConnectDevice(device) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StatusGateCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  body: String,
  accent: androidx.compose.ui.graphics.Color,
  buttonLabel: String?,
  onButtonClick: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.25.dp, accent.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(42.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        ) {
          Icon(icon, contentDescription = null, tint = accent)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (buttonLabel != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onButtonClick, modifier = Modifier.testTag("bt_gate_action_button")) {
          Text(buttonLabel)
        }
      }
    }
  }
}

@Composable
private fun BluetoothDeviceRow(
  device: BluetoothDeviceItem,
  connectionState: BtConnectionState,
  onClick: () -> Unit,
) {
  val ext = TivaTheme.extendedColors
  val isThisConnected =
    connectionState is BtConnectionState.Connected && connectionState.address == device.address
  val isThisConnecting =
    connectionState is BtConnectionState.Connecting && connectionState.address == device.address

  Surface(
    color =
      if (isThisConnected) ext.safeContainer.copy(alpha = 0.35f)
      else MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    border =
      BorderStroke(
        1.dp,
        if (isThisConnected) ext.safeEmerald.copy(alpha = 0.6f) else ext.cardSubtleBorder,
      ),
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(enabled = !isThisConnecting) { onClick() }
        .testTag("bt_device_${device.address}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(14.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(42.dp)
            .background(
              if (device.isLikelyHcModule) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surfaceVariant,
              RoundedCornerShape(12.dp),
            ),
      ) {
        Icon(
          imageVector =
            if (isThisConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
          contentDescription = null,
          tint =
            if (device.isLikelyHcModule) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = device.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (device.isLikelyHcModule) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "HC-05 / MCU",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        val rssiNote = device.rssi?.let { " • $it dBm" } ?: ""
        Text(
          text = "${device.address}$rssiNote",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (isThisConnecting) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
      } else {
        Text(
          text = if (isThisConnected) "Active" else "Connect",
          style = MaterialTheme.typography.labelLarge,
          color = if (isThisConnected) ext.safeEmerald else MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
