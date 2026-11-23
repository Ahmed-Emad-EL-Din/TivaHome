package com.example.bluetooth

data class BluetoothDeviceItem(
  val name: String,
  val address: String,
  val isPaired: Boolean,
  val isLikelyHcModule: Boolean,
  val rssi: Short? = null,
)

sealed interface BtConnectionState {
  data object Disconnected : BtConnectionState

  data class Connecting(val deviceName: String, val address: String, val attempt: Int = 1) :
    BtConnectionState

  data class Connected(
    val deviceName: String,
    val address: String,
    val connectedAtMillis: Long,
    val transportLabel: String,
  ) : BtConnectionState

  data class Error(val message: String, val deviceName: String? = null) : BtConnectionState
}

enum class SerialDirection {
  RX,
  TX,
  ACK,
  SYS,
  ERR,
}

data class SerialLogEntry(
  val id: Long,
  val timestampMillis: Long,
  val direction: SerialDirection,
  val text: String,
)
