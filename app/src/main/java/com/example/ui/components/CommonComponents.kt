package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.BtConnectionState
import com.example.protocol.TivaProtocol
import com.example.ui.navigation.TivaRoute
import com.example.ui.theme.TivaTheme
import com.example.ui.viewmodel.TempSample
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun ConnectionStatusPill(
  connectionState: BtConnectionState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  val (containerColor, contentColor, label, icon) =
    when (connectionState) {
      is BtConnectionState.Connected ->
        Quad(
          ext.safeContainer,
          ext.safeEmerald,
          connectionState.deviceName,
          Icons.Rounded.BluetoothConnected,
        )
      is BtConnectionState.Connecting ->
        Quad(
          ext.warningContainer,
          ext.warningAmber,
          "Connecting…",
          Icons.Rounded.BluetoothSearching,
        )
      is BtConnectionState.Error ->
        Quad(
          ext.alarmContainer,
          ext.alarmCrimson,
          "Retry Link",
          Icons.Rounded.BluetoothDisabled,
        )
      BtConnectionState.Disconnected ->
        Quad(
          MaterialTheme.colorScheme.surfaceVariant,
          MaterialTheme.colorScheme.onSurfaceVariant,
          "Disconnected",
          Icons.Rounded.BluetoothDisabled,
        )
    }

  val infiniteTransition = rememberInfiniteTransition(label = "bt_pulse")
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.4f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
      label = "pulse_alpha",
    )

  Surface(
    color = containerColor,
    shape = RoundedCornerShape(50),
    border = BorderStroke(1.dp, contentColor.copy(alpha = 0.35f)),
    modifier =
      modifier
        .clip(RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .testTag("connection_status_pill"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
      Box(
        modifier =
          Modifier.size(8.dp)
            .alpha(
              if (connectionState is BtConnectionState.Connected ||
                connectionState is BtConnectionState.Connecting
              )
                pulseAlpha
              else 1f
            )
            .background(contentColor, CircleShape)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ThermalAlertBanner(
  visible: Boolean,
  temperatureCelsius: Float?,
  limitCelsius: Int,
  useFahrenheit: Boolean,
  muted: Boolean,
  onMuteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  AnimatedVisibility(
    visible = visible,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut(),
    modifier = modifier,
  ) {
    Surface(
      color = ext.alarmContainer,
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.5.dp, ext.alarmCrimson.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(40.dp)
              .background(ext.alarmCrimson.copy(alpha = 0.16f), CircleShape),
        ) {
          Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = stringResource(R.string.alarm_banner_title),
            tint = ext.alarmCrimson,
            modifier = Modifier.size(22.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.alarm_banner_title),
            style = MaterialTheme.typography.titleSmall,
            color = ext.alarmCrimson,
            fontWeight = FontWeight.Bold,
          )
          val unit = TivaProtocol.unitSymbol(useFahrenheit)
          val tempStr =
            temperatureCelsius?.let {
              String.format(Locale.US, "%.1f%s", TivaProtocol.celsiusToDisplay(it, useFahrenheit), unit)
            } ?: "Board ALARM:1"
          val limitStr = "${TivaProtocol.celsiusToDisplayInt(limitCelsius, useFahrenheit)}$unit"
          Text(
            text =
              if (muted) "Muted • Reading $tempStr (Limit $limitStr)"
              else "Reading $tempStr exceeds limit $limitStr",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        if (!muted) {
          TextButton(
            onClick = onMuteClick,
            modifier = Modifier.testTag("mute_alarm_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsOff,
              contentDescription = null,
              tint = ext.alarmCrimson,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.alarm_banner_mute),
              color = ext.alarmCrimson,
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}

@Composable
fun TemperatureSparklineChart(
  samples: List<TempSample>,
  limitCelsius: Int,
  useFahrenheit: Boolean,
  isAlarm: Boolean,
  modifier: Modifier = Modifier,
) {
  val ext = TivaTheme.extendedColors
  val lineColor by
    animateColorAsState(
      targetValue = if (isAlarm) ext.alarmCrimson else MaterialTheme.colorScheme.primary,
      label = "sparkline_color",
    )
  val limitLineColor = ext.warningAmber
  val gridColor = MaterialTheme.colorScheme.outlineVariant

  if (samples.isEmpty()) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        modifier
          .fillMaxWidth()
          .height(92.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.small),
    ) {
      Text(
        text = "Waiting for 1 Hz LM35 telemetry (TEMP:xx.x)…",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  val values = samples.map { TivaProtocol.celsiusToDisplay(it.celsius, useFahrenheit) }
  val limitVal = TivaProtocol.celsiusToDisplay(limitCelsius.toFloat(), useFahrenheit)
  val rawMin = min(values.min(), limitVal)
  val rawMax = max(values.max(), limitVal)
  val span = max(rawMax - rawMin, 4f)
  val minY = rawMin - span * 0.15f
  val maxY = rawMax + span * 0.15f

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(104.dp)
        .clip(MaterialTheme.shapes.small)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        .padding(horizontal = 8.dp, vertical = 10.dp)
  ) {
    val w = size.width
    val h = size.height

    fun yFor(value: Float): Float {
      val norm = ((value - minY) / (maxY - minY)).coerceIn(0f, 1f)
      return h - (norm * h)
    }

    // Subtle horizontal grid lines
    listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
      drawLine(
        color = gridColor.copy(alpha = 0.5f),
        start = Offset(0f, h * frac),
        end = Offset(w, h * frac),
        strokeWidth = 1.dp.toPx(),
      )
    }

    // Limit threshold dashed line
    val limitY = yFor(limitVal)
    drawLine(
      color = limitLineColor.copy(alpha = 0.85f),
      start = Offset(0f, limitY),
      end = Offset(w, limitY),
      strokeWidth = 1.5.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
    )

    if (values.size == 1) {
      val y = yFor(values[0])
      drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(w / 2f, y))
      return@Canvas
    }

    val stepX = w / (values.size - 1).coerceAtLeast(1)
    val strokePath = Path()
    val fillPath = Path()

    values.forEachIndexed { idx, value ->
      val x = idx * stepX
      val y = yFor(value)
      if (idx == 0) {
        strokePath.moveTo(x, y)
        fillPath.moveTo(x, h)
        fillPath.lineTo(x, y)
      } else {
        val prevX = (idx - 1) * stepX
        val prevY = yFor(values[idx - 1])
        val midX = (prevX + x) / 2f
        strokePath.cubicTo(midX, prevY, midX, y, x, y)
        fillPath.cubicTo(midX, prevY, midX, y, x, y)
      }
      if (idx == values.lastIndex) {
        fillPath.lineTo(x, h)
        fillPath.close()
      }
    }

    drawPath(
      path = fillPath,
      brush =
        Brush.verticalGradient(
          colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.0f)),
          startY = 0f,
          endY = h,
        ),
    )

    drawPath(
      path = strokePath,
      color = lineColor,
      style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    val lastX = (values.size - 1) * stepX
    val lastY = yFor(values.last())
    drawCircle(color = lineColor.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = Offset(lastX, lastY))
    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
  }
}

@Composable
fun TivaDrawerContent(
  currentRoute: TivaRoute,
  connectionState: BtConnectionState,
  lampOn: Boolean,
  plugOn: Boolean,
  onSelectRoute: (TivaRoute) -> Unit,
) {
  val ext = TivaTheme.extendedColors
  ModalDrawerSheet(
    drawerContainerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.width(308.dp),
  ) {
    Column(
      modifier =
        Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
      // Brand Header
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(40.dp)
                  .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
            ) {
              Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = stringResource(R.string.drawer_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val isConnected = connectionState is BtConnectionState.Connected
            DrawerMiniBadge(
              label = if (isConnected) "LINK UP" else "OFFLINE",
              color = if (isConnected) ext.safeEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DrawerMiniBadge(
              label = if (lampOn) "LAMP ON" else "LAMP OFF",
              color = if (lampOn) ext.lampActive else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DrawerMiniBadge(
              label = if (plugOn) "PLUG ON" else "PLUG OFF",
              color = if (plugOn) ext.plugActive else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Text(
        text = "CONTROL & TELEMETRY",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      )

      listOf(TivaRoute.DASHBOARD, TivaRoute.DOOR_LOG, TivaRoute.BLUETOOTH).forEach { item ->
        val selected = currentRoute == item
        NavigationDrawerItem(
          selected = selected,
          onClick = { onSelectRoute(item) },
          icon = {
            Icon(
              imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
              contentDescription = item.title,
            )
          },
          label = {
            Column {
              Text(text = item.title, style = MaterialTheme.typography.titleSmall)
              Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          },
          colors =
            NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          modifier = Modifier.padding(vertical = 2.dp).testTag("drawer_item_${item.route}"),
        )
      }

      HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
      )

      Text(
        text = "ENGINEERING TOOLS & SETTINGS",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      )

      listOf(TivaRoute.TERMINAL, TivaRoute.PIN_MAP, TivaRoute.SETTINGS).forEach { item ->
        val selected = currentRoute == item
        NavigationDrawerItem(
          selected = selected,
          onClick = { onSelectRoute(item) },
          icon = {
            Icon(
              imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
              contentDescription = item.title,
            )
          },
          label = {
            Column {
              Text(text = item.title, style = MaterialTheme.typography.titleSmall)
              Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          },
          colors =
            NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          modifier = Modifier.padding(vertical = 2.dp).testTag("drawer_item_${item.route}"),
        )
      }
    }
  }
}

@Composable
private fun DrawerMiniBadge(label: String, color: Color) {
  Surface(
    color = color.copy(alpha = 0.14f),
    shape = RoundedCornerShape(6.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
