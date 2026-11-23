package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.SensorDoor
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DoorEventEntity
import com.example.ui.theme.TivaTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class DoorLogFilter(val label: String) {
  ALL("All"),
  OPEN("Opened"),
  CLOSED("Closed"),
}

@Composable
fun DoorLogScreen(
  events: List<DoorEventEntity>,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val ext = TivaTheme.extendedColors
  var filter by remember { mutableStateOf(DoorLogFilter.ALL) }
  var showClearDialog by remember { mutableStateOf(false) }

  val openCount = remember(events) { events.count { it.state == "OPEN" } }
  val closedCount = remember(events) { events.count { it.state == "CLOSED" } }

  val filteredEvents =
    remember(events, filter) {
      when (filter) {
        DoorLogFilter.ALL -> events
        DoorLogFilter.OPEN -> events.filter { it.state == "OPEN" }
        DoorLogFilter.CLOSED -> events.filter { it.state == "CLOSED" }
      }
    }

  val csvFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
  val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
  val dayFormatter = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }

  val groupedEvents =
    remember(filteredEvents) {
      filteredEvents.groupBy { event -> formatDayHeader(event.timestampMillis, dayFormatter) }
    }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
      text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            onClearHistory()
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
    modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp)) {
      Spacer(modifier = Modifier.height(10.dp))

      // Summary Row
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        SummaryStatPill(
          label = "Total Events",
          value = events.size.toString(),
          accent = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        SummaryStatPill(
          label = "Opened",
          value = openCount.toString(),
          accent = ext.alarmCrimson,
          modifier = Modifier.weight(1f),
        )
        SummaryStatPill(
          label = "Closed",
          value = closedCount.toString(),
          accent = ext.safeEmerald,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Filter & Action Toolbar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          DoorLogFilter.entries.forEach { entry ->
            FilterChip(
              selected = filter == entry,
              onClick = { filter = entry },
              label = { Text(entry.label) },
              modifier = Modifier.testTag("door_filter_${entry.name.lowercase()}"),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = {
              val csvHeader = "id,state,timestamp,device_name,device_address\n"
              val rows =
                events.joinToString("\n") { ev ->
                  val ts = csvFormatter.format(Date(ev.timestampMillis))
                  "${ev.id},${ev.state},$ts,${ev.deviceName.orEmpty()},${ev.deviceAddress.orEmpty()}"
                }
              val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, "TivaHome Door Activity Log")
                  putExtra(Intent.EXTRA_TEXT, csvHeader + rows)
                }
              context.startActivity(Intent.createChooser(shareIntent, "Export Door Log"))
            },
            enabled = events.isNotEmpty(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("door_log_export_button"),
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export")
          }

          OutlinedButton(
            onClick = { showClearDialog = true },
            enabled = events.isNotEmpty(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("door_log_clear_button"),
          ) {
            Icon(
              Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.door_log_clear),
              tint = ext.alarmCrimson,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (filteredEvents.isEmpty()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(72.dp)
                  .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            ) {
              Icon(
                imageVector = Icons.Rounded.DoorFront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp),
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = stringResource(R.string.door_log_empty_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = stringResource(R.string.door_log_empty_body),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.widthIn(max = 340.dp),
            )
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(bottom = 24.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          groupedEvents.forEach { (dateLabel, dayItems) ->
            item(key = "header_$dateLabel") {
              Text(
                text = dateLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
              )
            }

            items(items = dayItems, key = { it.id }) { event ->
              val isOpen = event.state == "OPEN"
              val accent = if (isOpen) ext.alarmCrimson else ext.safeEmerald
              val container = if (isOpen) ext.alarmContainer else ext.safeContainer
              val dateObj = Date(event.timestampMillis)

              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, ext.cardSubtleBorder),
                modifier = Modifier.fillMaxWidth().testTag("door_event_row_${event.id}"),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                      Modifier.size(40.dp).background(container, RoundedCornerShape(12.dp)),
                  ) {
                    Icon(
                      imageVector = if (isOpen) Icons.Rounded.SensorDoor else Icons.Rounded.DoorFront,
                      contentDescription = event.state,
                      tint = accent,
                      modifier = Modifier.size(22.dp),
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = if (isOpen) "Door Opened" else "Door Closed",
                      style = MaterialTheme.typography.titleSmall,
                      color = accent,
                      fontWeight = FontWeight.Bold,
                    )
                    val sourceNote =
                      if (!event.deviceName.isNullOrBlank()) " • ${event.deviceName}" else ""
                    Text(
                      text = "${formatAgo(event.timestampMillis)}$sourceNote",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = timeFormatter.format(dateObj),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
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

private fun formatDayHeader(timestampMillis: Long, formatter: SimpleDateFormat): String {
  val nowCal = Calendar.getInstance()
  val eventCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
  val isSameYear = nowCal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR)
  val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - eventCal.get(Calendar.DAY_OF_YEAR)
  return when {
    isSameYear && dayDiff == 0 -> "Today"
    isSameYear && dayDiff == 1 -> "Yesterday"
    else -> formatter.format(Date(timestampMillis))
  }
}

private fun formatAgo(timestampMillis: Long): String {
  val diffSec = ((System.currentTimeMillis() - timestampMillis) / 1000L).coerceAtLeast(0L)
  return when {
    diffSec < 10 -> "Just now"
    diffSec < 60 -> "${diffSec}s ago"
    diffSec < 3600 -> "${diffSec / 60}m ago"
    diffSec < 86400 -> "${diffSec / 3600}h ago"
    else -> "${diffSec / 86400}d ago"
  }
}

@Composable
private fun SummaryStatPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    modifier = modifier,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = accent,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
