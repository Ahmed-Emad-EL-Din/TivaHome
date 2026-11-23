package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alert.ThermalAlertNotifier
import com.example.bluetooth.BluetoothDeviceItem
import com.example.bluetooth.BluetoothSppManager
import com.example.bluetooth.BtConnectionState
import com.example.bluetooth.SerialDirection
import com.example.bluetooth.SerialLogEntry
import com.example.data.local.DoorEventEntity
import com.example.data.local.DoorLogRepository
import com.example.data.local.DoorState
import com.example.data.local.TivaDatabase
import com.example.data.settings.AppSettings
import com.example.data.settings.SettingsRepository
import com.example.data.settings.ThemeMode
import com.example.protocol.TivaEvent
import com.example.protocol.TivaProtocol
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TempSample(val timestampMillis: Long, val celsius: Float)

data class TelemetryUiState(
  val lampOn: Boolean = false,
  val lampPending: Boolean = false,
  val plugOn: Boolean = false,
  val plugPending: Boolean = false,
  val doorState: DoorState = DoorState.UNKNOWN,
  val temperatureCelsius: Float? = null,
  val temperatureHistory: List<TempSample> = emptyList(),
  val boardAlarmActive: Boolean = false,
  val alarmMutedForEpisode: Boolean = false,
  val lastAckText: String? = null,
) {
  fun isAlarmActive(limitCelsius: Int): Boolean {
    val overLimit = temperatureCelsius != null && temperatureCelsius > limitCelsius
    return boardAlarmActive || overLimit
  }
}

class HomeControlViewModel(application: Application) : AndroidViewModel(application) {

  private val settingsRepository = SettingsRepository(application)
  private val doorLogRepository =
    DoorLogRepository(TivaDatabase.getInstance(application).doorEventDao())
  private val bluetoothManager = BluetoothSppManager(application)
  private val alertNotifier = ThermalAlertNotifier(application)

  val settings: StateFlow<AppSettings> =
    settingsRepository.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = AppSettings(),
    )

  val doorEvents: StateFlow<List<DoorEventEntity>> =
    doorLogRepository.allEvents.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val connectionState: StateFlow<BtConnectionState> = bluetoothManager.connectionState
  val isBluetoothSupported: StateFlow<Boolean> = bluetoothManager.isBluetoothSupported
  val isBluetoothEnabled: StateFlow<Boolean> = bluetoothManager.isBluetoothEnabled
  val hasPermissions: StateFlow<Boolean> = bluetoothManager.hasPermissions
  val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = bluetoothManager.pairedDevices
  val discoveredDevices: StateFlow<List<BluetoothDeviceItem>> = bluetoothManager.discoveredDevices
  val isScanning: StateFlow<Boolean> = bluetoothManager.isScanning
  val serialLogs: StateFlow<List<SerialLogEntry>> = bluetoothManager.serialLogs
  val lastPacketMillis: StateFlow<Long?> = bluetoothManager.lastPacketMillis

  private val _telemetry = MutableStateFlow(TelemetryUiState())
  val telemetry: StateFlow<TelemetryUiState> = _telemetry.asStateFlow()

  val isEffectiveAlarmActive: StateFlow<Boolean> =
    combine(_telemetry, settings) { t, s -> t.isAlarmActive(s.tempLimitCelsius) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
      )

  private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
  val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

  private var lampPendingTimeoutJob: Job? = null
  private var plugPendingTimeoutJob: Job? = null
  private var previousEffectiveAlarm = false

  init {
    viewModelScope.launch {
      settings.collect { appSettings ->
        bluetoothManager.updateConnectionPreferences(
          autoReconnect = appSettings.autoReconnect,
          insecureFallback = appSettings.insecureRfcommFallback,
        )
        evaluateAlarmTransition()
      }
    }

    viewModelScope.launch {
      bluetoothManager.incomingLines.collect { line ->
        handleIncomingLine(line)
      }
    }

    viewModelScope.launch {
      connectionState.collect { state ->
        if (state is BtConnectionState.Connected) {
          settingsRepository.saveLastDevice(state.address, state.deviceName)
        }
      }
    }
  }

  fun requiredBluetoothPermissions(): Array<String> = bluetoothManager.requiredPermissions()

  fun onPermissionsResult() {
    bluetoothManager.refreshAdapterState()
  }

  fun refreshBluetoothState() {
    bluetoothManager.refreshAdapterState()
  }

  fun startDiscovery() {
    bluetoothManager.startDiscovery()
  }

  fun stopDiscovery() {
    bluetoothManager.stopDiscovery()
  }

  fun connectToDevice(address: String, name: String? = null) {
    bluetoothManager.connect(address, name)
  }

  fun quickConnect(): Boolean {
    val lastAddr = settings.value.lastDeviceAddress
    if (!lastAddr.isNullOrBlank()) {
      bluetoothManager.connect(lastAddr, settings.value.lastDeviceName)
      return true
    }
    val preferred =
      pairedDevices.value.firstOrNull { it.isLikelyHcModule } ?: pairedDevices.value.firstOrNull()
    if (preferred != null) {
      bluetoothManager.connect(preferred.address, preferred.name)
      return true
    }
    return false
  }

  fun disconnect() {
    bluetoothManager.disconnect()
  }

  fun toggleLamp(targetOn: Boolean) {
    if (connectionState.value !is BtConnectionState.Connected) {
      _snackbarEvents.tryEmit("Connect to HC-05 first to control the 220V lamp")
      return
    }
    val cmd = TivaProtocol.lampCommand(targetOn)
    _telemetry.update { it.copy(lampOn = targetOn, lampPending = true) }
    bluetoothManager.sendLine(cmd)
    lampPendingTimeoutJob?.cancel()
    lampPendingTimeoutJob =
      viewModelScope.launch {
        delay(2200)
        _telemetry.update { it.copy(lampPending = false) }
      }
  }

  fun togglePlug(targetOn: Boolean) {
    if (connectionState.value !is BtConnectionState.Connected) {
      _snackbarEvents.tryEmit("Connect to HC-05 first to control the power plug")
      return
    }
    val cmd = TivaProtocol.plugCommand(targetOn)
    _telemetry.update { it.copy(plugOn = targetOn, plugPending = true) }
    bluetoothManager.sendLine(cmd)
    plugPendingTimeoutJob?.cancel()
    plugPendingTimeoutJob =
      viewModelScope.launch {
        delay(2200)
        _telemetry.update { it.copy(plugPending = false) }
      }
  }

  fun setAllAppliances(turnOn: Boolean) {
    if (connectionState.value !is BtConnectionState.Connected) {
      _snackbarEvents.tryEmit("Connect to HC-05 first to control appliances")
      return
    }
    _telemetry.update {
      it.copy(lampOn = turnOn, lampPending = true, plugOn = turnOn, plugPending = true)
    }
    bluetoothManager.sendLine(TivaProtocol.lampCommand(turnOn))
    bluetoothManager.sendLine(TivaProtocol.plugCommand(turnOn))
    lampPendingTimeoutJob?.cancel()
    plugPendingTimeoutJob?.cancel()
    lampPendingTimeoutJob =
      viewModelScope.launch {
        delay(2200)
        _telemetry.update { it.copy(lampPending = false) }
      }
    plugPendingTimeoutJob =
      viewModelScope.launch {
        delay(2200)
        _telemetry.update { it.copy(plugPending = false) }
      }
  }

  fun requestStatusSync() {
    if (connectionState.value !is BtConnectionState.Connected) {
      _snackbarEvents.tryEmit("Connect to HC-05 to query STATUS?")
      return
    }
    bluetoothManager.sendLine(TivaProtocol.statusQueryCommand())
  }

  fun applyTemperatureLimit(limitCelsius: Int) {
    val clamped = limitCelsius.coerceIn(10, 80)
    viewModelScope.launch {
      settingsRepository.setTempLimitCelsius(clamped)
      if (connectionState.value is BtConnectionState.Connected) {
        bluetoothManager.sendLine(TivaProtocol.limitCommand(clamped))
        _snackbarEvents.tryEmit("Sent LIMIT:$clamped to TM4C123")
      } else {
        _snackbarEvents.tryEmit("Saved limit $clamped°C locally (connect to sync)")
      }
    }
  }

  fun sendRawSerialCommand(rawCommand: String) {
    val trimmed = rawCommand.trim()
    if (trimmed.isEmpty()) return
    if (connectionState.value !is BtConnectionState.Connected) {
      _snackbarEvents.tryEmit("Not connected to HC-05")
      return
    }
    bluetoothManager.sendLine(trimmed)
  }

  fun clearSerialLog() {
    bluetoothManager.clearSerialLogs()
  }

  fun muteCurrentAlarm() {
    _telemetry.update { it.copy(alarmMutedForEpisode = true) }
  }

  fun clearDoorHistory() {
    viewModelScope.launch {
      doorLogRepository.clearHistory()
      _snackbarEvents.tryEmit("Door activity history cleared")
    }
  }

  fun setUseFahrenheit(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setUseFahrenheit(enabled) }
  }

  fun setAutoReconnect(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setAutoReconnect(enabled) }
  }

  fun setInsecureFallback(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setInsecureRfcommFallback(enabled) }
  }

  fun setAlarmSoundEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setAlarmSoundEnabled(enabled) }
  }

  fun setAlarmVibrationEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setAlarmVibrationEnabled(enabled) }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { settingsRepository.setThemeMode(mode) }
  }

  fun testThermalAlertFeedback() {
    val s = settings.value
    alertNotifier.triggerAlert(
      playSound = s.alarmSoundEnabled,
      vibrate = s.alarmVibrationEnabled,
    )
  }

  private fun handleIncomingLine(line: String) {
    val events = TivaProtocol.parseLine(line)
    val now = System.currentTimeMillis()
    val connectedInfo = connectionState.value as? BtConnectionState.Connected

    for (event in events) {
      when (event) {
        is TivaEvent.Temperature -> {
          _telemetry.update { state ->
            val updatedHistory =
              (state.temperatureHistory + TempSample(now, event.celsius)).takeLast(60)
            state.copy(
              temperatureCelsius = event.celsius,
              temperatureHistory = updatedHistory,
            )
          }
          evaluateAlarmTransition()
        }
        is TivaEvent.DoorChanged -> {
          val previousDoor = _telemetry.value.doorState
          _telemetry.update { it.copy(doorState = event.state) }
          // Always log spontaneous DOOR:OPEN / DOOR:CLOSED events, or state transitions from known state
          val shouldLog =
            !event.fromStatusPacket || (previousDoor != DoorState.UNKNOWN && previousDoor != event.state)
          if (shouldLog) {
            viewModelScope.launch {
              doorLogRepository.logDoorTransition(
                state = event.state,
                timestampMillis = now,
                deviceName = connectedInfo?.deviceName,
                deviceAddress = connectedInfo?.address,
              )
            }
          }
        }
        is TivaEvent.AlarmChanged -> {
          _telemetry.update { it.copy(boardAlarmActive = event.active) }
          evaluateAlarmTransition()
        }
        is TivaEvent.LampState -> {
          lampPendingTimeoutJob?.cancel()
          _telemetry.update { it.copy(lampOn = event.on, lampPending = false) }
        }
        is TivaEvent.PlugState -> {
          plugPendingTimeoutJob?.cancel()
          _telemetry.update { it.copy(plugOn = event.on, plugPending = false) }
        }
        is TivaEvent.LimitState -> {
          viewModelScope.launch { settingsRepository.setTempLimitCelsius(event.celsius) }
        }
        is TivaEvent.Ack -> {
          val cmdUpper = event.command.uppercase()
          _telemetry.update { state ->
            state.copy(
              lampPending = if (cmdUpper.startsWith("LAMP")) false else state.lampPending,
              plugPending = if (cmdUpper.startsWith("PLUG")) false else state.plugPending,
              lastAckText = event.command,
            )
          }
          if (cmdUpper.startsWith("LAMP")) lampPendingTimeoutJob?.cancel()
          if (cmdUpper.startsWith("PLUG")) plugPendingTimeoutJob?.cancel()
        }
        is TivaEvent.Unknown -> {
          bluetoothManager.appendLog(SerialDirection.SYS, "Unparsed frame: ${event.raw}")
        }
      }
    }
  }

  private fun evaluateAlarmTransition() {
    val active = _telemetry.value.isAlarmActive(settings.value.tempLimitCelsius)
    if (active && !previousEffectiveAlarm) {
      _telemetry.update { it.copy(alarmMutedForEpisode = false) }
      val s = settings.value
      alertNotifier.triggerAlert(
        playSound = s.alarmSoundEnabled,
        vibrate = s.alarmVibrationEnabled,
      )
    } else if (!active && previousEffectiveAlarm) {
      _telemetry.update { it.copy(alarmMutedForEpisode = false) }
    }
    previousEffectiveAlarm = active
  }

  override fun onCleared() {
    super.onCleared()
    bluetoothManager.release()
  }
}
