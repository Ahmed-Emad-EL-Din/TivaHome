package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bluetooth.BluetoothDeviceItem
import com.example.bluetooth.BtConnectionState
import com.example.ui.theme.TivaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickConnectBottomSheet(
  visible: Boolean,
  isBluetoothSupported: Boolean,
  isBluetoothEnabled: Boolean,
  hasPermissions: Boolean,
  requiredPermissions: Array<String>,
  connectionState: BtConnectionState,
  pairedDevices: List<BluetoothDeviceItem>,
  discoveredDevices: List<BluetoothDeviceItem>,
  isScanning: Boolean,
  onDismiss: () -> Unit,
  onPermissionsResult: () -> Unit,
  onRefreshState: () -> Unit,
  onStartScan: () -> Unit,
  onStopScan: () -> Unit,
  onConnectDevice: (BluetoothDeviceItem) -> Unit,
  onDisconnect: () -> Unit,
  onSyncStatus: () -> Unit,
  onOpenFullBluetoothScreen: () -> Unit,
) {
  if (!visible) return

  val context = LocalContext.current
  val ext = TivaTheme.extendedColors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      onPermissionsResult()
    }
  val enableBtLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      onRefreshState()
    }

  // Auto-dismiss smoothly once a new connection succeeds while the sheet was used to connect
  LaunchedEffect(connectionState) {
    if (connectionState is BtConnectionState.Connecting) {
      // keep open while connecting
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("quick_connect_sheet"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "HC-05 Quick Connect",
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = "Classic Bluetooth SPP • 9600 baud",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        TextButton(
          onClick = {
            onDismiss()
            onOpenFullBluetoothScreen()
          },
          modifier = Modifier.testTag("sheet_open_full_bt_button"),
        ) {
          Text("All Tools")
          Spacer(modifier = Modifier.width(4.dp))
          Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      when {
        !isBluetoothSupported -> {
          Surface(
            color = ext.alarmContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Classic Bluetooth hardware is unavailable on this device/emulator.",
              style = MaterialTheme.typography.bodyMedium,
              color = ext.alarmCrimson,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
        !hasPermissions -> {
          Surface(
            color = ext.warningContainer.copy(alpha = 0.7f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Bluetooth permissions are needed to connect to HC-05.",
                style = MaterialTheme.typography.bodyMedium,
              )
              Spacer(modifier = Modifier.height(10.dp))
              Button(
                onClick = { permissionLauncher.launch(requiredPermissions) },
                modifier = Modifier.testTag("sheet_grant_permissions_button"),
              ) {
                Text("Grant Bluetooth Permissions")
              }
            }
          }
        }
        !isBluetoothEnabled -> {
          Surface(
            color = ext.warningContainer.copy(alpha = 0.7f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Bluetooth is currently turned off.",
                style = MaterialTheme.typography.bodyMedium,
              )
              Spacer(modifier = Modifier.height(10.dp))
              Button(
                onClick = {
                  runCatching {
                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                  }
                },
                modifier = Modifier.testTag("sheet_enable_bt_button"),
              ) {
                Text("Turn On Bluetooth")
              }
            }
          }
        }
        else -> {
          if (connectionState is BtConnectionState.Connected) {
            Surface(
              color = ext.safeContainer.copy(alpha = 0.55f),
              shape = MaterialTheme.shapes.medium,
              border = BorderStroke(1.dp, ext.safeEmerald.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
              ) {
                Icon(
                  Icons.Rounded.BluetoothConnected,
                  contentDescription = null,
                  tint = ext.safeEmerald,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = connectionState.deviceName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = connectionState.transportLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                FilledTonalButton(
                  onClick = onSyncStatus,
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                  onClick = onDisconnect,
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Icon(Icons.Rounded.LinkOff, contentDescription = "Disconnect", modifier = Modifier.size(16.dp))
                }
              }
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            FilledTonalButton(
              onClick = { if (isScanning) onStopScan() else onStartScan() },
              modifier = Modifier.weight(1f).testTag("sheet_scan_button"),
            ) {
              if (isScanning) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Scan")
              } else {
                Icon(Icons.Rounded.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Nearby")
              }
            }
            OutlinedButton(
              onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
              },
            ) {
              Icon(Icons.Rounded.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("OS Pair")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          val allDevices =
            (pairedDevices + discoveredDevices.filter { d -> pairedDevices.none { it.address == d.address } })

          if (allDevices.isEmpty()) {
            Text(
              text = "No paired or discovered devices yet. Tap 'Scan Nearby' or 'OS Pair' to add your HC-05.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 12.dp),
            )
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
            ) {
              items(items = allDevices, key = { it.address }) { device ->
                val isConnected =
                  connectionState is BtConnectionState.Connected &&
                    connectionState.address == device.address
                val isConnecting =
                  connectionState is BtConnectionState.Connecting &&
                    connectionState.address == device.address

                Surface(
                  color =
                    if (isConnected) ext.safeContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  shape = MaterialTheme.shapes.medium,
                  border =
                    BorderStroke(
                      1.dp,
                      if (isConnected) ext.safeEmerald else ext.cardSubtleBorder,
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(MaterialTheme.shapes.medium)
                      .clickable(enabled = !isConnecting) {
                        onConnectDevice(device)
                        onDismiss()
                      }
                      .testTag("sheet_device_${device.address}"),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier =
                        Modifier.size(38.dp)
                          .background(
                            if (device.isLikelyHcModule) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(10.dp),
                          ),
                    ) {
                      Icon(
                        imageVector =
                          if (isConnected) Icons.Rounded.BluetoothConnected
                          else Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint =
                          if (device.isLikelyHcModule) MaterialTheme.colorScheme.onPrimaryContainer
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                      )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text =
                          device.address +
                            if (device.isPaired) " • Paired" else " • Discovered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    if (isConnecting) {
                      CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                      Text(
                        text = if (isConnected) "Active" else "Connect",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isConnected) ext.safeEmerald else MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
