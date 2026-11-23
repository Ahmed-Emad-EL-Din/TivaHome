package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bluetooth.BtConnectionState
import com.example.data.local.DoorState
import com.example.protocol.TivaProtocol
import com.example.ui.theme.TivaTheme
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveRoomHeroCard(
  connectionState: BtConnectionState,
  lampOn: Boolean,
  plugOn: Boolean,
  doorState: DoorState,
  temperatureCelsius: Float?,
  useFahrenheit: Boolean,
  isAlarmActive: Boolean,
  onSetAllAppliances: (Boolean) -> Unit,
  onOpenQuickConnect: () -> Unit,
  onSyncStatus: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  val haptic = LocalHapticFeedback.current
  val isConnected = connectionState is BtConnectionState.Connected

  val lampGlowAlpha by
    animateFloatAsState(
      targetValue = if (lampOn) 1f else 0f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "lamp_glow",
    )
  val doorAngleFrac by
    animateFloatAsState(
      targetValue = if (doorState == DoorState.OPEN) 1f else 0f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "door_swing",
    )
  val plugGlowAlpha by
    animateFloatAsState(
      targetValue = if (plugOn) 1f else 0.15f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "plug_glow",
    )

  val heroBorder by
    animateColorAsState(
      targetValue =
        when {
          isAlarmActive -> ext.alarmCrimson.copy(alpha = 0.65f)
          lampOn -> ext.lampActive.copy(alpha = 0.45f)
          isConnected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
          else -> ext.cardSubtleBorder
        },
      label = "hero_border",
    )

  val lampColor = ext.lampActive
  val plugColor = ext.plugActive
  val doorColor =
    when (doorState) {
      DoorState.OPEN -> ext.alarmCrimson
      DoorState.CLOSED -> ext.safeEmerald
      DoorState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
  val frameColor = MaterialTheme.colorScheme.outline
  val primaryColor = MaterialTheme.colorScheme.primary
  val surfaceTint = MaterialTheme.colorScheme.surfaceVariant

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.extraLarge,
    border = BorderStroke(1.25.dp, heroBorder),
    tonalElevation = 2.dp,
    modifier = modifier.fillMaxWidth().testTag("live_room_hero_card"),
  ) {
    Column(
      modifier =
        Modifier.background(
            Brush.linearGradient(
              colors =
                listOf(
                  if (lampOn) ext.lampContainer.copy(alpha = 0.25f)
                  else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f),
                  MaterialTheme.colorScheme.surface,
                )
            )
          )
          .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        // Interactive Architectural Room Scene Canvas
        Canvas(
          modifier =
            Modifier.size(width = 112.dp, height = 88.dp)
              .padding(end = 4.dp)
        ) {
          val w = size.width
          val h = size.height

          // Ambient warm lamp ceiling cone when lampOn
          if (lampGlowAlpha > 0.01f) {
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      lampColor.copy(alpha = 0.42f * lampGlowAlpha),
                      lampColor.copy(alpha = 0.0f),
                    ),
                  center = Offset(w * 0.46f, h * 0.36f),
                  radius = w * 0.46f,
                ),
              center = Offset(w * 0.46f, h * 0.36f),
              radius = w * 0.46f,
            )
          }

          // House Roof & Walls
          val housePath =
            Path().apply {
              moveTo(w * 0.10f, h * 0.42f)
              lineTo(w * 0.48f, h * 0.10f)
              lineTo(w * 0.86f, h * 0.42f)
              moveTo(w * 0.18f, h * 0.36f)
              lineTo(w * 0.18f, h * 0.88f)
              lineTo(w * 0.78f, h * 0.88f)
              lineTo(w * 0.78f, h * 0.36f)
            }
          drawPath(
            path = housePath,
            color = frameColor,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
          )

          // Hanging Ceiling Lamp
          drawLine(
            color = frameColor,
            start = Offset(w * 0.46f, h * 0.12f),
            end = Offset(w * 0.46f, h * 0.30f),
            strokeWidth = 2.dp.toPx(),
          )
          drawCircle(
            color = if (lampOn) lampColor else surfaceTint,
            radius = 7.5.dp.toPx(),
            center = Offset(w * 0.46f, h * 0.36f),
          )
          drawCircle(
            color = if (lampOn) lampColor else frameColor,
            radius = 7.5.dp.toPx(),
            center = Offset(w * 0.46f, h * 0.36f),
            style = Stroke(width = 1.8.dp.toPx()),
          )

          // Wall Power Plug Node (left wall)
          drawRoundRect(
            color = plugColor.copy(alpha = plugGlowAlpha),
            topLeft = Offset(w * 0.24f, h * 0.60f),
            size = Size(13.dp.toPx(), 13.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx()),
          )
          if (plugOn) {
            drawCircle(
              color = plugColor.copy(alpha = 0.26f),
              radius = 10.dp.toPx(),
              center = Offset(w * 0.24f + 6.5.dp.toPx(), h * 0.60f + 6.5.dp.toPx()),
            )
          }

          // Magnetic Door (right side, pivots inward when OPEN)
          val doorHingeX = w * 0.66f
          val doorBottomY = h * 0.88f
          val doorHeight = h * 0.34f
          val doorWidth = w * 0.11f
          val swingOffset = doorAngleFrac * (doorWidth * 0.85f)
          drawLine(
            color = doorColor,
            start = Offset(doorHingeX, doorBottomY),
            end = Offset(doorHingeX - swingOffset, doorBottomY - doorHeight),
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawCircle(
            color = doorColor,
            radius = 3.dp.toPx(),
            center = Offset(doorHingeX, doorBottomY - doorHeight * 0.5f),
          )

          // Bluetooth Link Signal Dot (top right corner)
          drawCircle(
            color = if (isConnected) primaryColor else frameColor.copy(alpha = 0.5f),
            radius = 3.5.dp.toPx(),
            center = Offset(w * 0.88f, h * 0.16f),
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              color =
                if (isConnected) ext.safeContainer
                else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text =
                  when (connectionState) {
                    is BtConnectionState.Connected -> "LIVE • ${connectionState.deviceName}"
                    is BtConnectionState.Connecting -> "CONNECTING…"
                    is BtConnectionState.Error -> "LINK ERROR"
                    BtConnectionState.Disconnected -> "OFFLINE"
                  },
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (isConnected) ext.safeEmerald
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          val activeCount = (if (lampOn) 1 else 0) + (if (plugOn) 1 else 0)
          val headline =
            when {
              !isConnected -> "Prototype Disconnected"
              isAlarmActive -> "Thermal Alert Active"
              activeCount == 2 -> "All Appliances Active"
              activeCount == 1 -> if (lampOn) "220V Lamp Lit" else "Power Plug Energized"
              else -> "All Relays Standby"
            }
          Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          val tempSummary =
            temperatureCelsius?.let {
              String.format(
                Locale.US,
                "%.1f%s",
                TivaProtocol.celsiusToDisplay(it, useFahrenheit),
                TivaProtocol.unitSymbol(useFahrenheit),
              )
            } ?: "--.-"
          val doorLabel =
            when (doorState) {
              DoorState.OPEN -> "Door Open"
              DoorState.CLOSED -> "Door Closed"
              DoorState.UNKNOWN -> "Door --"
            }
          Text(
            text = "$tempSummary • $doorLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Quick Scene & Connection Bar
      if (isConnected) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          FilledTonalButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onSetAllAppliances(true)
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("master_all_on_button"),
          ) {
            Icon(Icons.Rounded.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("All On", maxLines = 1)
          }

          OutlinedButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onSetAllAppliances(false)
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("master_all_off_button"),
          ) {
            Icon(Icons.Rounded.FlashOff, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("All Off", maxLines = 1)
          }

          FilledTonalButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSyncStatus()
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            modifier = Modifier.testTag("sync_status_button"),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Sync STATUS?", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sync", maxLines = 1)
          }
        }
      } else {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Button(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onOpenQuickConnect()
            },
            modifier = Modifier.weight(1f).testTag("quick_connect_button"),
          ) {
            Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quick Connect HC-05")
          }
        }
      }
    }
  }
}

@Composable
fun RadialTemperatureGauge(
  temperatureCelsius: Float?,
  limitCelsius: Int,
  useFahrenheit: Boolean,
  isAlarm: Boolean,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  val minC = 10f
  val maxC = 60f

  val targetFraction =
    temperatureCelsius?.let { ((it - minC) / (maxC - minC)).coerceIn(0f, 1f) } ?: 0f
  val animatedFraction by
    animateFloatAsState(
      targetValue = targetFraction,
      animationSpec = spring(stiffness = Spring.StiffnessLow),
      label = "temp_gauge_sweep",
    )

  val limitFraction = ((limitCelsius - minC) / (maxC - minC)).coerceIn(0f, 1f)

  val activeArcColor by
    animateColorAsState(
      targetValue =
        when {
          temperatureCelsius == null -> MaterialTheme.colorScheme.outline
          isAlarm -> ext.alarmCrimson
          temperatureCelsius >= limitCelsius - 3 -> ext.warningAmber
          else -> MaterialTheme.colorScheme.primary
        },
      label = "gauge_color",
    )
  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  val limitTickColor = ext.warningAmber

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(136.dp),
  ) {
    Canvas(modifier = Modifier.size(132.dp)) {
      val strokePx = 11.dp.toPx()
      val inset = strokePx / 2f + 4.dp.toPx()
      val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
      val topLeft = Offset(inset, inset)
      val startAngle = 150f
      val maxSweep = 240f

      // Background track arc
      drawArc(
        color = trackColor,
        startAngle = startAngle,
        sweepAngle = maxSweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )

      // Active value arc
      if (temperatureCelsius != null && animatedFraction > 0.005f) {
        drawArc(
          color = activeArcColor,
          startAngle = startAngle,
          sweepAngle = maxSweep * animatedFraction,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
      }

      // Limit threshold tick mark along the outer arc
      val limitAngleRad = ((startAngle + maxSweep * limitFraction) * PI / 180.0)
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = arcSize.width / 2f
      val innerTick =
        Offset(
          center.x + (radius - strokePx * 0.7f) * cos(limitAngleRad).toFloat(),
          center.y + (radius - strokePx * 0.7f) * sin(limitAngleRad).toFloat(),
        )
      val outerTick =
        Offset(
          center.x + (radius + strokePx * 0.7f) * cos(limitAngleRad).toFloat(),
          center.y + (radius + strokePx * 0.7f) * sin(limitAngleRad).toFloat(),
        )
      drawLine(
        color = limitTickColor,
        start = innerTick,
        end = outerTick,
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      val displayValue =
        temperatureCelsius?.let {
          String.format(
            Locale.US,
            "%.1f",
            TivaProtocol.celsiusToDisplay(it, useFahrenheit),
          )
        } ?: "--.-"
      Text(
        text = displayValue,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = if (isAlarm) ext.alarmCrimson else MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = TivaProtocol.unitSymbol(useFahrenheit),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
