package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TivaTheme

private enum class PinCategory(val label: String) {
  ALL("All Pins"),
  RELAYS("Relays"),
  SENSORS("Sensors"),
  UART("Bluetooth UART"),
}

private data class PinRow(
  val component: String,
  val modulePin: String,
  val tivaPin: String,
  val category: PinCategory,
)

private data class ProtocolRow(val direction: String, val message: String, val meaning: String)

private val CSE322_PIN_MAP =
  listOf(
    PinRow("Lamp Relay Module", "IN", "PA2", PinCategory.RELAYS),
    PinRow("Plug Relay Module", "IN", "PA3", PinCategory.RELAYS),
    PinRow("Magnetic Door Switch", "One wire (other to GND)", "PA4", PinCategory.SENSORS),
    PinRow("LM35 Temp Sensor", "OUT", "PE3 (AIN0)", PinCategory.SENSORS),
    PinRow("HC-05 Bluetooth", "TXD", "PB0 (U1Rx)", PinCategory.UART),
    PinRow("HC-05 Bluetooth", "RXD", "PB1 (U1Tx)", PinCategory.UART),
  )

private val CSE322_PROTOCOL =
  listOf(
    ProtocolRow("App → Board", "LAMP:1 / LAMP:0", "Lamp relay on or off (PA2)"),
    ProtocolRow("App → Board", "PLUG:1 / PLUG:0", "Plug relay on or off (PA3)"),
    ProtocolRow("App → Board", "LIMIT:30", "Set thermal alert limit in °C (whole number)"),
    ProtocolRow("App → Board", "STATUS?", "Request lamp, plug, door, temp & alarm state"),
    ProtocolRow("Board → App", "TEMP:27.5", "LM35 room temperature in °C, sent every 1s"),
    ProtocolRow("Board → App", "DOOR:OPEN / DOOR:CLOSED", "Sent on every magnetic switch transition"),
    ProtocolRow("Board → App", "ALARM:1 / ALARM:0", "1 when temp > limit, 0 when <= limit"),
    ProtocolRow("Board → App", "ACK:LAMP:1", "Confirms command; echoes command received"),
  )

@Composable
fun PinMapScreen(modifier: Modifier = Modifier) {
  val ext = TivaTheme.extendedColors
  var selectedCategory by remember { mutableStateOf(PinCategory.ALL) }

  val filteredPins =
    remember(selectedCategory) {
      if (selectedCategory == PinCategory.ALL) CSE322_PIN_MAP
      else CSE322_PIN_MAP.filter { it.category == selectedCategory }
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
      // Hardware Power & Grounding Notes
      Surface(
        color = ext.warningContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ext.warningAmber.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Bolt,
              contentDescription = null,
              tint = ext.warningAmber,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Power & Common Ground Checklist",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text =
              "• Power the 2-channel 5V relay module, LM35, and HC-05 from an external 5V (≥1A) supply.\n" +
                "• Connect the external 5V supply GND to a TM4C123 LaunchPad GND pin so all grounds are common.\n" +
                "• Power the TM4C123GH6PM LaunchPad via its USB cable.\n" +
                "• Wire the Lamp relay contacts in parallel with the manual wall switch so the manual switch still works.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      // Pin Map Table Card with Category Filters
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ext.cardSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.DeveloperBoard,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.pin_map_title),
              style = MaterialTheme.typography.titleMedium,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          ) {
            PinCategory.entries.forEach { category ->
              FilterChip(
                selected = selectedCategory == category,
                onClick = { selectedCategory = category },
                label = { Text(category.label) },
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          filteredPins.forEachIndexed { idx, row ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
              Column(modifier = Modifier.weight(1.3f)) {
                Text(text = row.component, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "Module: ${row.modulePin}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = row.tivaPin,
                  style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }
            if (idx < filteredPins.lastIndex) {
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
              )
            }
          }
        }
      }

      // Protocol Reference Card
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ext.cardSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Cable,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.protocol_title),
              style = MaterialTheme.typography.titleMedium,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "All ASCII lines end with newline (\\n) at 9600 baud.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(10.dp))
          CSE322_PROTOCOL.forEachIndexed { idx, row ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = row.message,
                  style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = row.direction,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = row.meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (idx < CSE322_PROTOCOL.lastIndex) {
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
