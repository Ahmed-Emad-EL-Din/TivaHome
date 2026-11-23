package com.example.protocol

import com.example.data.local.DoorState
import kotlin.math.roundToInt

sealed interface TivaEvent {
  data class Temperature(val celsius: Float) : TivaEvent

  data class DoorChanged(val state: DoorState, val fromStatusPacket: Boolean = false) : TivaEvent

  data class AlarmChanged(val active: Boolean) : TivaEvent

  data class LampState(val on: Boolean) : TivaEvent

  data class PlugState(val on: Boolean) : TivaEvent

  data class LimitState(val celsius: Int) : TivaEvent

  data class Ack(val command: String) : TivaEvent

  data class Unknown(val raw: String) : TivaEvent
}

object TivaProtocol {
  const val BAUD_RATE = 9600
  const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

  fun lampCommand(on: Boolean): String = if (on) "LAMP:1" else "LAMP:0"

  fun plugCommand(on: Boolean): String = if (on) "PLUG:1" else "PLUG:0"

  fun limitCommand(celsius: Int): String = "LIMIT:${celsius.coerceIn(10, 80)}"

  fun statusQueryCommand(): String = "STATUS?"

  fun celsiusToDisplay(celsius: Float, useFahrenheit: Boolean): Float {
    return if (useFahrenheit) (celsius * 9f / 5f) + 32f else celsius
  }

  fun celsiusToDisplayInt(celsius: Int, useFahrenheit: Boolean): Int {
    return if (useFahrenheit) ((celsius * 9f / 5f) + 32f).roundToInt() else celsius
  }

  fun unitSymbol(useFahrenheit: Boolean): String = if (useFahrenheit) "°F" else "°C"

  /**
   * Parses a single ASCII line from the TM4C123GH6PM board (without trailing \r/\n).
   * Supports both standalone lines (e.g. "TEMP:27.5", "DOOR:OPEN", "ACK:LAMP:1")
   * and multi-field STATUS replies (e.g. "STATUS:LAMP=1,PLUG=0,DOOR=CLOSED,TEMP=27.5,ALARM=0").
   */
  fun parseLine(rawLine: String): List<TivaEvent> {
    val line = rawLine.trim()
    if (line.isEmpty()) return emptyList()

    val upper = line.uppercase()

    // Exact ACK:<command>
    if (upper.startsWith("ACK:")) {
      val ackPayload = line.substringAfter(':', "").trim()
      val events = mutableListOf<TivaEvent>(TivaEvent.Ack(ackPayload))
      // Also synchronize state if the ACK wraps a state command like ACK:LAMP:1 or ACK:PLUG:0
      parseSingleToken(ackPayload, fromStatusPacket = false)?.let { inner ->
        if (inner !is TivaEvent.Unknown) {
          events.add(inner)
        }
      }
      return events
    }

    // Combined STATUS packet: e.g. "STATUS:LAMP=1,PLUG=0,DOOR=CLOSED,TEMP=27.5,ALARM=0"
    // or "LAMP:1,PLUG:0,DOOR:CLOSED,TEMP:27.5,ALARM:0"
    if (upper.startsWith("STATUS:") || upper.startsWith("STATUS ") || line.contains(',') || line.contains(';')) {
      val body =
        when {
          upper.startsWith("STATUS:") -> line.substringAfter(':').trim()
          upper.startsWith("STATUS ") -> line.substringAfter(' ').trim()
          else -> line
        }
      val tokens = body.split(',', ';', '|').map { it.trim() }.filter { it.isNotEmpty() }
      if (tokens.size > 1 || upper.startsWith("STATUS:") || upper.startsWith("STATUS ")) {
        val parsed =
          tokens.mapNotNull { token ->
            parseSingleToken(token, fromStatusPacket = true)
          }
        if (parsed.isNotEmpty()) return parsed
      }
    }

    val single = parseSingleToken(line, fromStatusPacket = false)
    return if (single != null) listOf(single) else listOf(TivaEvent.Unknown(line))
  }

  private fun parseSingleToken(token: String, fromStatusPacket: Boolean): TivaEvent? {
    val cleaned = token.trim()
    if (cleaned.isEmpty()) return null

    val separatorIdx = cleaned.indexOfFirst { it == ':' || it == '=' }
    if (separatorIdx <= 0) return TivaEvent.Unknown(cleaned)

    val key = cleaned.substring(0, separatorIdx).trim().uppercase()
    val value = cleaned.substring(separatorIdx + 1).trim()
    val valueUpper = value.uppercase()

    return when (key) {
      "TEMP", "TEMPERATURE" -> {
        val temp = value.removeSuffix("C").removeSuffix("°C").trim().toFloatOrNull()
        if (temp != null && temp.isFinite()) TivaEvent.Temperature(temp)
        else TivaEvent.Unknown(cleaned)
      }
      "DOOR" -> {
        when (valueUpper) {
          "OPEN", "OPENED", "1" -> TivaEvent.DoorChanged(DoorState.OPEN, fromStatusPacket)
          "CLOSED", "CLOSE", "0" -> TivaEvent.DoorChanged(DoorState.CLOSED, fromStatusPacket)
          else -> TivaEvent.Unknown(cleaned)
        }
      }
      "ALARM" -> {
        when (valueUpper) {
          "1", "ON", "TRUE", "HIGH" -> TivaEvent.AlarmChanged(true)
          "0", "OFF", "FALSE", "LOW" -> TivaEvent.AlarmChanged(false)
          else -> TivaEvent.Unknown(cleaned)
        }
      }
      "LAMP" -> {
        when (valueUpper) {
          "1", "ON", "TRUE" -> TivaEvent.LampState(true)
          "0", "OFF", "FALSE" -> TivaEvent.LampState(false)
          else -> TivaEvent.Unknown(cleaned)
        }
      }
      "PLUG" -> {
        when (valueUpper) {
          "1", "ON", "TRUE" -> TivaEvent.PlugState(true)
          "0", "OFF", "FALSE" -> TivaEvent.PlugState(false)
          else -> TivaEvent.Unknown(cleaned)
        }
      }
      "LIMIT" -> {
        val limit = value.toFloatOrNull()?.roundToInt()
        if (limit != null) TivaEvent.LimitState(limit) else TivaEvent.Unknown(cleaned)
      }
      else -> TivaEvent.Unknown(cleaned)
    }
  }
}
