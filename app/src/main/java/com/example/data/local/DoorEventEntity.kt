package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DoorState {
  UNKNOWN,
  OPEN,
  CLOSED,
}

@Entity(tableName = "door_events")
data class DoorEventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val state: String, // "OPEN" or "CLOSED"
  val timestampMillis: Long,
  val deviceName: String?,
  val deviceAddress: String?,
)
