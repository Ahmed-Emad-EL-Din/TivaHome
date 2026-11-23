package com.example

import com.example.data.local.DoorState
import com.example.protocol.TivaEvent
import com.example.protocol.TivaProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TivaProtocolTest {

  @Test
  fun `command builders format exact CSE322 ASCII strings`() {
    assertEquals("LAMP:1", TivaProtocol.lampCommand(true))
    assertEquals("LAMP:0", TivaProtocol.lampCommand(false))
    assertEquals("PLUG:1", TivaProtocol.plugCommand(true))
    assertEquals("PLUG:0", TivaProtocol.plugCommand(false))
    assertEquals("LIMIT:30", TivaProtocol.limitCommand(30))
    assertEquals("STATUS?", TivaProtocol.statusQueryCommand())
  }

  @Test
  fun `parses standalone telemetry and alarm lines`() {
    val tempEvents = TivaProtocol.parseLine("TEMP:27.5\r")
    assertEquals(1, tempEvents.size)
    assertEquals(27.5f, (tempEvents[0] as TivaEvent.Temperature).celsius, 0.001f)

    val doorOpen = TivaProtocol.parseLine("DOOR:OPEN")
    assertEquals(TivaEvent.DoorChanged(DoorState.OPEN, false), doorOpen.first())

    val doorClosed = TivaProtocol.parseLine("DOOR:CLOSED")
    assertEquals(TivaEvent.DoorChanged(DoorState.CLOSED, false), doorClosed.first())

    val alarmOn = TivaProtocol.parseLine("ALARM:1")
    assertEquals(TivaEvent.AlarmChanged(true), alarmOn.first())

    val alarmOff = TivaProtocol.parseLine("ALARM:0")
    assertEquals(TivaEvent.AlarmChanged(false), alarmOff.first())
  }

  @Test
  fun `parses ACK and combined STATUS lines`() {
    val ackEvents = TivaProtocol.parseLine("ACK:LAMP:1")
    assertTrue(ackEvents.contains(TivaEvent.Ack("LAMP:1")))
    assertTrue(ackEvents.contains(TivaEvent.LampState(true)))

    val statusEvents =
      TivaProtocol.parseLine("STATUS:LAMP=1,PLUG=0,DOOR=CLOSED,TEMP=24.8,ALARM=0")
    assertEquals(5, statusEvents.size)
    assertTrue(statusEvents.contains(TivaEvent.LampState(true)))
    assertTrue(statusEvents.contains(TivaEvent.PlugState(false)))
    assertTrue(statusEvents.contains(TivaEvent.DoorChanged(DoorState.CLOSED, true)))
    assertTrue(statusEvents.contains(TivaEvent.AlarmChanged(false)))
    val temp = statusEvents.filterIsInstance<TivaEvent.Temperature>().single()
    assertEquals(24.8f, temp.celsius, 0.001f)
  }
}
