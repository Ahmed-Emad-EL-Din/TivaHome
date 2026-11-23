package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SensorDoor
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.BtConnectionState
import com.example.data.local.DoorEventEntity
import com.example.data.local.DoorState
import com.example.data.settings.AppSettings
import com.example.protocol.TivaProtocol
import com.example.ui.components.LiveRoomHeroCard
import com.example.ui.components.RadialTemperatureGauge
import com.example.ui.components.TemperatureSparklineChart
import com.example.ui.theme.TivaTheme
import com.example.ui.viewmodel.TelemetryUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
  telemetry: TelemetryUiState,
  settings: AppSettings,
  connectionState: BtConnectionState,
  latestDoorEvent: DoorEventEntity?,
  lastPacketMillis: Long?,
  isAlarmActive: Boolean,
  onToggleLamp: (Boolean) -> Unit,
  onTogglePlug: (Boolean) -> Unit,
  onSetAllAppliances: (Boolean) -> Unit,
  onApplyLimit: (Int) -> Unit,
  onSyncStatus: () -> Unit,
  onOpenQuickConnect: () -> Unit,
  onOpenDoorLogScreen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isConnected = connectionState is BtConnectionState.Connected

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWide = maxWidth >= 680.dp

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().widthIn(max = 920.dp),
      ) {
        // 1. Live Room Hero Banner + Master Controls + Quick Connect
        LiveRoomHeroCard(
          connectionState = connectionState,
          lampOn = telemetry.lampOn,
          plugOn = telemetry.plugOn,
          doorState = telemetry.doorState,
          temperatureCelsius = telemetry.temperatureCelsius,
          useFahrenheit = settings.useFahrenheit,
          isAlarmActive = isAlarmActive,
          onSetAllAppliances = onSetAllAppliances,
          onOpenQuickConnect = onOpenQuickConnect,
          onSyncStatus = onSyncStatus,
        )

        // 2. Appliance Relay Controls (Lamp + Plug)
        Row(
          horizontalArrangement = Arrangement.spacedBy(if (isWide) 14.dp else 12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          ApplianceRelayCard(
            title = stringResource(R.string.appliance_lamp_title),
            subtitle = if (isWide) stringResource(R.string.appliance_lamp_subtitle) else "PA2 • Parallel Switch",
            isOn = telemetry.lampOn,
            isPending = telemetry.lampPending,
            enabled = isConnected,
            activeColor = TivaTheme.extendedColors.lampActive,
            activeContainer = TivaTheme.extendedColors.lampContainer,
            icon = Icons.Rounded.Lightbulb,
            footnote = if (isWide) "Manual wall switch active" else "Wall switch active",
            testTag = "lamp_toggle_card",
            switchTag = "lamp_switch",
            onToggle = onToggleLamp,
            modifier = Modifier.weight(1f),
          )
          ApplianceRelayCard(
            title = stringResource(R.string.appliance_plug_title),
            subtitle = if (isWide) stringResource(R.string.appliance_plug_subtitle) else "PA3 • Mains Relay",
            isOn = telemetry.plugOn,
            isPending = telemetry.plugPending,
            enabled = isConnected,
            activeColor = TivaTheme.extendedColors.plugActive,
            activeContainer = TivaTheme.extendedColors.plugContainer,
            icon = Icons.Rounded.Power,
            footnote = if (telemetry.plugOn) "220V energized" else "Relay open — safe",
            testTag = "plug_toggle_card",
            switchTag = "plug_switch",
            onToggle = onTogglePlug,
            modifier = Modifier.weight(1f),
          )
        }

        // 3. Door Sensor + Room Temperature Telemetry
        if (isWide) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            DoorStatusCard(
              doorState = telemetry.doorState,
              latestEvent = latestDoorEvent,
              onOpenDoorLog = onOpenDoorLogScreen,
              modifier = Modifier.weight(0.95f),
            )
            TemperatureTelemetryCard(
              telemetry = telemetry,
              settings = settings,
              isConnected = isConnected,
              isAlarmActive = isAlarmActive,
              lastPacketMillis = lastPacketMillis,
              onApplyLimit = onApplyLimit,
              modifier = Modifier.weight(1.35f),
            )
          }
        } else {
          DoorStatusCard(
            doorState = telemetry.doorState,
            latestEvent = latestDoorEvent,
            onOpenDoorLog = onOpenDoorLogScreen,
            modifier = Modifier.fillMaxWidth(),
          )
          TemperatureTelemetryCard(
            telemetry = telemetry,
            settings = settings,
            isConnected = isConnected,
            isAlarmActive = isAlarmActive,
            lastPacketMillis = lastPacketMillis,
            onApplyLimit = onApplyLimit,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun ApplianceRelayCard(
  title: String,
  subtitle: String,
  isOn: Boolean,
  isPending: Boolean,
  enabled: Boolean,
  activeColor: Color,
  activeContainer: Color,
  icon: ImageVector,
  footnote: String,
  testTag: String,
  switchTag: String,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  val haptic = LocalHapticFeedback.current

  val containerColor by
    animateColorAsState(
      targetValue = if (isOn) activeContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "relay_bg",
    )
  val borderColor by
    animateColorAsState(
      targetValue = if (isOn) activeColor.copy(alpha = 0.7f) else ext.cardSubtleBorder,
      label = "relay_border",
    )
  val iconScale by
    animateFloatAsState(
      targetValue = if (isOn) 1.08f else 1.0f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "relay_icon_scale",
    )

  Surface(
    color = containerColor,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.25.dp, borderColor),
    tonalElevation = if (isOn) 3.dp else 1.dp,
    modifier =
      modifier
        .clip(MaterialTheme.shapes.large)
        .clickable(enabled = enabled) {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onToggle(!isOn)
        }
        .testTag(testTag),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(44.dp)
              .scale(iconScale)
              .background(
                if (isOn) activeColor.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(14.dp),
              ),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isOn) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
          )
        }

        if (isPending) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = activeColor,
            modifier = Modifier.size(22.dp),
          )
        } else {
          Switch(
            checked = isOn,
            onCheckedChange = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onToggle(it)
            },
            enabled = enabled,
            modifier = Modifier.testTag(switchTag),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          color =
            if (isOn) activeColor.copy(alpha = 0.20f)
            else MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(6.dp),
        ) {
          Text(
            text =
              when {
                isPending -> "SYNCING"
                isOn -> "ACTIVE"
                else -> "OFF"
              },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isOn) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
        Text(
          text = footnote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun DoorStatusCard(
  doorState: DoorState,
  latestEvent: DoorEventEntity?,
  onOpenDoorLog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

  val (badgeBg, accentColor, label, icon) =
    when (doorState) {
      DoorState.OPEN ->
        DoorVisual(ext.alarmContainer, ext.alarmCrimson, "OPEN", Icons.Rounded.SensorDoor)
      DoorState.CLOSED ->
        DoorVisual(ext.safeContainer, ext.safeEmerald, "CLOSED", Icons.Rounded.DoorFront)
      DoorState.UNKNOWN ->
        DoorVisual(
          MaterialTheme.colorScheme.surfaceVariant,
          MaterialTheme.colorScheme.onSurfaceVariant,
          "AWAITING",
          Icons.Rounded.DoorFront,
        )
    }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border =
      BorderStroke(
        1.2.dp,
        if (doorState == DoorState.OPEN) ext.alarmCrimson.copy(alpha = 0.5f)
        else ext.cardSubtleBorder,
      ),
    tonalElevation = 1.dp,
    modifier =
      modifier
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onOpenDoorLog)
        .testTag("door_status_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).background(badgeBg, RoundedCornerShape(14.dp)),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = stringResource(R.string.door_sensor_title),
              tint = accentColor,
              modifier = Modifier.size(24.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = stringResource(R.string.door_sensor_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = stringResource(R.string.door_sensor_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          color = badgeBg,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        val lastEventDesc =
          if (latestEvent != null) {
            val rel = formatRelativeTime(latestEvent.timestampMillis)
            "Last ${latestEvent.state.lowercase()} $rel (${timeFormat.format(Date(latestEvent.timestampMillis))})"
          } else {
            "No open/close events recorded yet"
          }
        Text(
          text = lastEventDesc,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "View Log →",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun TemperatureTelemetryCard(
  telemetry: TelemetryUiState,
  settings: AppSettings,
  isConnected: Boolean,
  isAlarmActive: Boolean,
  lastPacketMillis: Long?,
  onApplyLimit: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  val haptic = LocalHapticFeedback.current
  var draftLimitCelsius by remember { mutableIntStateOf(settings.tempLimitCelsius) }
  LaunchedEffect(settings.tempLimitCelsius) { draftLimitCelsius = settings.tempLimitCelsius }

  val unit = TivaProtocol.unitSymbol(settings.useFahrenheit)
  val borderColor by
    animateColorAsState(
      targetValue = if (isAlarmActive) ext.alarmCrimson.copy(alpha = 0.7f) else ext.cardSubtleBorder,
      label = "temp_border",
    )

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.25.dp, borderColor),
    tonalElevation = 1.dp,
    modifier = modifier.fillMaxWidth().testTag("temperature_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(42.dp)
                .background(
                  if (isAlarmActive) ext.alarmContainer else MaterialTheme.colorScheme.primaryContainer,
                  RoundedCornerShape(12.dp),
                ),
          ) {
            Icon(
              imageVector = Icons.Rounded.Thermostat,
              contentDescription = stringResource(R.string.temp_card_title),
              tint =
                if (isAlarmActive) ext.alarmCrimson
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = stringResource(R.string.temp_card_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = stringResource(R.string.temp_card_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (isConnected && lastPacketMillis != null) {
          Surface(
            color = ext.safeContainer.copy(alpha = 0.6f),
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "1 Hz LIVE",
              style = MaterialTheme.typography.labelSmall,
              color = ext.safeEmerald,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Radial Gauge + 60s Sparkline Chart
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 340.dp) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            RadialTemperatureGauge(
              temperatureCelsius = telemetry.temperatureCelsius,
              limitCelsius = draftLimitCelsius,
              useFahrenheit = settings.useFahrenheit,
              isAlarm = isAlarmActive,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "60-SECOND TREND",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
              )
              TemperatureSparklineChart(
                samples = telemetry.temperatureHistory,
                limitCelsius = draftLimitCelsius,
                useFahrenheit = settings.useFahrenheit,
                isAlarm = isAlarmActive,
              )
            }
          }
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            RadialTemperatureGauge(
              temperatureCelsius = telemetry.temperatureCelsius,
              limitCelsius = draftLimitCelsius,
              useFahrenheit = settings.useFahrenheit,
              isAlarm = isAlarmActive,
            )
            TemperatureSparklineChart(
              samples = telemetry.temperatureHistory,
              limitCelsius = draftLimitCelsius,
              useFahrenheit = settings.useFahrenheit,
              isAlarm = isAlarmActive,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Limit control row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.temp_limit_label),
            style = MaterialTheme.typography.titleSmall,
          )
          val displayDraft = TivaProtocol.celsiusToDisplayInt(draftLimitCelsius, settings.useFahrenheit)
          val extraNote = if (settings.useFahrenheit) " (Board: ${draftLimitCelsius}°C)" else ""
          Text(
            text = "Alert above $displayDraft$unit$extraNote",
            style = MaterialTheme.typography.bodySmall,
            color = ext.warningAmber,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilledTonalIconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              draftLimitCelsius = (draftLimitCelsius - 1).coerceIn(10, 80)
            },
            modifier = Modifier.size(48.dp).testTag("limit_decrement_button"),
          ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease limit")
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = "${draftLimitCelsius}°C",
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
          FilledTonalIconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              draftLimitCelsius = (draftLimitCelsius + 1).coerceIn(10, 80)
            },
            modifier = Modifier.size(48.dp).testTag("limit_increment_button"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase limit")
          }
        }
      }

      Slider(
        value = draftLimitCelsius.toFloat(),
        onValueChange = { draftLimitCelsius = it.roundToInt().coerceIn(10, 80) },
        valueRange = 15f..60f,
        steps = 44,
        modifier = Modifier.fillMaxWidth().testTag("temp_limit_slider"),
      )

      Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        val isDirty = draftLimitCelsius != settings.tempLimitCelsius
        Button(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onApplyLimit(draftLimitCelsius)
          },
          enabled = isDirty || isConnected,
          modifier = Modifier.testTag("apply_limit_button"),
        ) {
          Text(
            text =
              if (isConnected) "Send LIMIT:$draftLimitCelsius"
              else stringResource(R.string.temp_limit_apply),
          )
        }
      }
    }
  }
}

private fun formatRelativeTime(timestampMillis: Long): String {
  val diffSec = ((System.currentTimeMillis() - timestampMillis) / 1000L).coerceAtLeast(0L)
  return when {
    diffSec < 10 -> "just now"
    diffSec < 60 -> "${diffSec}s ago"
    diffSec < 3600 -> "${diffSec / 60}m ago"
    diffSec < 86400 -> "${diffSec / 3600}h ago"
    else -> "${diffSec / 86400}d ago"
  }
}

private data class DoorVisual(
  val bg: Color,
  val accent: Color,
  val label: String,
  val icon: ImageVector,
)
