package com.example.data.local

import kotlinx.coroutines.flow.Flow

class DoorLogRepository(private val dao: DoorEventDao) {
  val allEvents: Flow<List<DoorEventEntity>> = dao.observeAllEvents()

  suspend fun logDoorTransition(
    state: DoorState,
    timestampMillis: Long,
    deviceName: String?,
    deviceAddress: String?,
  ) {
    if (state != DoorState.OPEN && state != DoorState.CLOSED) return
    dao.insertEvent(
      DoorEventEntity(
        state = state.name,
        timestampMillis = timestampMillis,
        deviceName = deviceName,
        deviceAddress = deviceAddress,
      )
    )
  }

  suspend fun clearHistory() {
    dao.clearAll()
  }
}
