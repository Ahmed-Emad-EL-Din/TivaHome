package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.BtConnectionState
import com.example.bluetooth.SerialDirection
import com.example.bluetooth.SerialLogEntry
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TivaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TerminalFilter(val label: String) {
  ALL("ALL"),
  RX("RX"),
  TX("TX"),
  ACK("ACK"),
  SYS("SYS"),
}

@Composable
fun SerialTerminalScreen(
  logs: List<SerialLogEntry>,
  connectionState: BtConnectionState,
  onSendCommand: (String) -> Unit,
  onClearLogs: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val haptic = LocalHapticFeedback.current
  val ext = TivaTheme.extendedColors
  var filter by remember { mutableStateOf(TerminalFilter.ALL) }
  var autoScroll by remember { mutableStateOf(true) }
  var commandInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

  val rxCount = remember(logs) { logs.count { it.direction == SerialDirection.RX } }
  val txCount = remember(logs) { logs.count { it.direction == SerialDirection.TX } }
  val ackCount = remember(logs) { logs.count { it.direction == SerialDirection.ACK } }

  val filteredLogs =
    remember(logs, filter) {
      when (filter) {
        TerminalFilter.ALL -> logs
        TerminalFilter.RX -> logs.filter { it.direction == SerialDirection.RX }
        TerminalFilter.TX -> logs.filter { it.direction == SerialDirection.TX }
        TerminalFilter.ACK -> logs.filter { it.direction == SerialDirection.ACK }
        TerminalFilter.SYS ->
          logs.filter {
            it.direction == SerialDirection.SYS || it.direction == SerialDirection.ERR
          }
      }
    }

  LaunchedEffect(filteredLogs.size, autoScroll) {
    if (autoScroll && filteredLogs.isNotEmpty()) {
      listState.animateScrollToItem(filteredLogs.lastIndex)
    }
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 900.dp)) {
      // Packet Counters Strip
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
      ) {
        PacketCounterBadge("RX $rxCount", ext.terminalRx)
        PacketCounterBadge("TX $txCount", ext.terminalTx)
        PacketCounterBadge("ACK $ackCount", ext.terminalAck)
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "Tap line to copy",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Filter & Controls Bar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        ) {
          TerminalFilter.entries.forEach { f ->
            FilterChip(
              selected = filter == f,
              onClick = { filter = f },
              label = { Text(f.label) },
              modifier = Modifier.testTag("terminal_filter_${f.name.lowercase()}"),
            )
          }
        }
        IconButton(
          onClick = { autoScroll = !autoScroll },
          modifier = Modifier.size(48.dp).testTag("terminal_autoscroll_button"),
        ) {
          Icon(
            imageVector = if (autoScroll) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (autoScroll) "Pause auto-scroll" else "Resume auto-scroll",
          )
        }
        IconButton(
          onClick = onClearLogs,
          modifier = Modifier.size(48.dp).testTag("terminal_clear_button"),
        ) {
          Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear terminal")
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Console Output Box
      Surface(
        color = ext.terminalBackground,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.weight(1f).fillMaxWidth(),
      ) {
        if (filteredLogs.isEmpty()) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(
              text = stringResource(R.string.terminal_empty),
              style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
              color = ext.terminalSystem,
            )
          }
        } else {
          LazyColumn(
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize().testTag("terminal_log_list"),
          ) {
            items(items = filteredLogs, key = { it.id }) { entry ->
              val (tagColor, tagLabel) =
                when (entry.direction) {
                  SerialDirection.RX -> ext.terminalRx to "RX"
                  SerialDirection.TX -> ext.terminalTx to "TX"
                  SerialDirection.ACK -> ext.terminalAck to "ACK"
                  SerialDirection.SYS -> ext.terminalSystem to "SYS"
                  SerialDirection.ERR -> ext.alarmCrimson to "ERR"
                }
              Row(
                verticalAlignment = Alignment.Top,
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      clipboard.setText(AnnotatedString(entry.text))
                      Toast.makeText(context, "Copied: ${entry.text}", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 2.dp),
              ) {
                Text(
                  text = timeFormat.format(Date(entry.timestampMillis)),
                  style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                  color = Color(0xFF64748B),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "[$tagLabel]",
                  style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                  color = tagColor,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = entry.text,
                  style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                  color = Color(0xFFE2E8F0),
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Quick ASCII Command Chips
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      ) {
        listOf("STATUS?", "LAMP:1", "LAMP:0", "PLUG:1", "PLUG:0", "LIMIT:30").forEach { cmd ->
          AssistChip(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSendCommand(cmd)
            },
            enabled = connectionState is BtConnectionState.Connected,
            label = {
              Text(
                text = cmd,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
              )
            },
            modifier = Modifier.testTag("terminal_chip_$cmd"),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Raw ASCII Input Row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedTextField(
          value = commandInput,
          onValueChange = { commandInput = it },
          placeholder = { Text(stringResource(R.string.terminal_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions =
            KeyboardActions(
              onSend = {
                if (commandInput.isNotBlank()) {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  onSendCommand(commandInput)
                  commandInput = ""
                }
              }
            ),
          modifier = Modifier.weight(1f).testTag("terminal_command_input"),
        )
        FilledIconButton(
          onClick = {
            if (commandInput.isNotBlank()) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSendCommand(commandInput)
              commandInput = ""
            }
          },
          enabled = commandInput.isNotBlank() && connectionState is BtConnectionState.Connected,
          modifier = Modifier.size(52.dp).testTag("terminal_send_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = stringResource(R.string.terminal_send),
          )
        }
      }
    }
  }
}

@Composable
private fun PacketCounterBadge(label: String, color: Color) {
  Surface(
    color = color.copy(alpha = 0.14f),
    shape = RoundedCornerShape(6.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}
