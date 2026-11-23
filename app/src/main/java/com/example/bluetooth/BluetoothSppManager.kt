package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.protocol.TivaProtocol
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BluetoothSppManager(private val context: Context) {

  private val sppUuid: UUID = UUID.fromString(TivaProtocol.SPP_UUID)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val bluetoothManager: BluetoothManager? =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter?
    get() = bluetoothManager?.adapter

  private val _isBluetoothSupported = MutableStateFlow(bluetoothAdapter != null)
  val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

  private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
  val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

  private val _hasPermissions = MutableStateFlow(checkPermissions())
  val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

  private val _connectionState =
    MutableStateFlow<BtConnectionState>(BtConnectionState.Disconnected)
  val connectionState: StateFlow<BtConnectionState> = _connectionState.asStateFlow()

  private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
  val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = _pairedDevices.asStateFlow()

  private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
  val discoveredDevices: StateFlow<List<BluetoothDeviceItem>> = _discoveredDevices.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _incomingLines = MutableSharedFlow<String>(extraBufferCapacity = 128)
  val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

  private val _serialLogs = MutableStateFlow<List<SerialLogEntry>>(emptyList())
  val serialLogs: StateFlow<List<SerialLogEntry>> = _serialLogs.asStateFlow()

  private val _lastPacketMillis = MutableStateFlow<Long?>(null)
  val lastPacketMillis: StateFlow<Long?> = _lastPacketMillis.asStateFlow()

  private val logCounter = AtomicLong(0L)

  private var activeSocket: BluetoothSocket? = null
  private var connectJob: Job? = null
  private var readerJob: Job? = null
  private var writerJob: Job? = null
  private var reconnectJob: Job? = null
  private var outgoingChannel: Channel<String>? = null

  @Volatile private var userInitiatedDisconnect = false
  @Volatile private var autoReconnectEnabled = true
  @Volatile private var insecureFallbackEnabled = true
  @Volatile private var lastConnectedDevice: Pair<String, String>? = null // address to name

  private var receiverRegistered = false

  private val btReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        when (intent?.action) {
          BluetoothAdapter.ACTION_STATE_CHANGED -> {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            val enabled = state == BluetoothAdapter.STATE_ON
            _isBluetoothEnabled.value = enabled
            if (!enabled) {
              _isScanning.value = false
              appendLog(SerialDirection.SYS, "Bluetooth adapter turned off")
              closeActiveSocketOnly()
              _connectionState.value = BtConnectionState.Disconnected
            } else {
              appendLog(SerialDirection.SYS, "Bluetooth adapter enabled")
              refreshPairedDevices()
            }
          }
          BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
            _isScanning.value = true
          }
          BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
            _isScanning.value = false
          }
          BluetoothDevice.ACTION_FOUND -> {
            val device: BluetoothDevice? =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
              } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
              }
            val rssi =
              intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).takeIf {
                it != Short.MIN_VALUE
              }
            if (device != null && hasConnectPermission()) {
              addDiscoveredDevice(device, rssi)
            }
          }
          BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
            refreshPairedDevices()
          }
        }
      }
    }

  init {
    registerReceiver()
    refreshAdapterState()
  }

  fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
      )
    } else {
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
      )
    }
  }

  fun hasConnectPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun hasScanPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED
    }
  }

  fun checkPermissions(): Boolean = hasConnectPermission() && hasScanPermission()

  fun updateConnectionPreferences(autoReconnect: Boolean, insecureFallback: Boolean) {
    this.autoReconnectEnabled = autoReconnect
    this.insecureFallbackEnabled = insecureFallback
  }

  fun refreshAdapterState() {
    val adapter = bluetoothAdapter
    _isBluetoothSupported.value = adapter != null
    _isBluetoothEnabled.value = adapter?.isEnabled == true
    _hasPermissions.value = checkPermissions()
    if (adapter?.isEnabled == true && hasConnectPermission()) {
      refreshPairedDevices()
    } else if (adapter?.isEnabled != true) {
      _pairedDevices.value = emptyList()
    }
  }

  @SuppressLint("MissingPermission")
  fun refreshPairedDevices() {
    val adapter = bluetoothAdapter ?: return
    if (!hasConnectPermission() || !adapter.isEnabled) {
      _pairedDevices.value = emptyList()
      return
    }
    try {
      val bonded = adapter.bondedDevices.orEmpty()
      val items =
        bonded
          .map { device ->
            val safeName = device.name?.takeIf { it.isNotBlank() } ?: "Unnamed Device"
            BluetoothDeviceItem(
              name = safeName,
              address = device.address,
              isPaired = true,
              isLikelyHcModule = isLikelyHcModule(safeName),
            )
          }
          .sortedWith(
            compareByDescending<BluetoothDeviceItem> { it.isLikelyHcModule }.thenBy { it.name }
          )
      _pairedDevices.value = items
    } catch (se: SecurityException) {
      appendLog(SerialDirection.ERR, "Permission denied reading paired devices: ${se.message}")
    }
  }

  @SuppressLint("MissingPermission")
  fun startDiscovery() {
    val adapter = bluetoothAdapter ?: return
    _hasPermissions.value = checkPermissions()
    if (!hasScanPermission() || !adapter.isEnabled) return
    try {
      if (adapter.isDiscovering) {
        adapter.cancelDiscovery()
      }
      _discoveredDevices.value = emptyList()
      val started = adapter.startDiscovery()
      if (started) {
        _isScanning.value = true
        appendLog(SerialDirection.SYS, "Scanning nearby Classic Bluetooth devices…")
      } else {
        appendLog(SerialDirection.ERR, "Unable to start Bluetooth discovery")
      }
    } catch (se: SecurityException) {
      appendLog(SerialDirection.ERR, "Scan permission missing: ${se.message}")
    }
  }

  @SuppressLint("MissingPermission")
  fun stopDiscovery() {
    val adapter = bluetoothAdapter ?: return
    if (!hasScanPermission()) return
    try {
      if (adapter.isDiscovering) {
        adapter.cancelDiscovery()
      }
    } catch (_: SecurityException) {}
    _isScanning.value = false
  }

  @SuppressLint("MissingPermission")
  private fun addDiscoveredDevice(device: BluetoothDevice, rssi: Short?) {
    try {
      val address = device.address ?: return
      val safeName = device.name?.takeIf { it.isNotBlank() } ?: "Unknown ($address)"
      val isPaired = device.bondState == BluetoothDevice.BOND_BONDED
      val item =
        BluetoothDeviceItem(
          name = safeName,
          address = address,
          isPaired = isPaired,
          isLikelyHcModule = isLikelyHcModule(safeName),
          rssi = rssi,
        )
      _discoveredDevices.update { current ->
        val filtered = current.filterNot { it.address == address }
        (filtered + item).sortedWith(
          compareByDescending<BluetoothDeviceItem> { it.isLikelyHcModule }
            .thenByDescending { it.rssi ?: Short.MIN_VALUE }
        )
      }
    } catch (_: SecurityException) {}
  }

  private fun isLikelyHcModule(name: String): Boolean {
    val upper = name.uppercase()
    return upper.contains("HC-05") ||
      upper.contains("HC-06") ||
      upper.contains("HC05") ||
      upper.contains("HC06") ||
      upper.contains("TIVA") ||
      upper.contains("TM4C") ||
      upper.contains("BT04") ||
      upper.contains("SPP")
  }

  @SuppressLint("MissingPermission")
  fun connect(address: String, displayName: String? = null) {
    val adapter = bluetoothAdapter
    if (adapter == null) {
      _connectionState.value = BtConnectionState.Error("Bluetooth hardware not available")
      return
    }
    if (!adapter.isEnabled) {
      _connectionState.value = BtConnectionState.Error("Turn on Bluetooth to connect")
      return
    }
    if (!hasConnectPermission()) {
      _connectionState.value = BtConnectionState.Error("Bluetooth Connect permission required")
      return
    }

    userInitiatedDisconnect = false
    reconnectJob?.cancel()
    connectJob?.cancel()

    connectJob =
      scope.launch {
        stopDiscovery()
        closeActiveSocketOnly()

        val device =
          try {
            adapter.getRemoteDevice(address)
          } catch (e: IllegalArgumentException) {
            _connectionState.value = BtConnectionState.Error("Invalid MAC address: $address")
            return@launch
          }

        val resolvedName =
          try {
            displayName ?: device.name?.takeIf { it.isNotBlank() } ?: address
          } catch (_: SecurityException) {
            displayName ?: address
          }

        lastConnectedDevice = address to resolvedName
        _connectionState.value = BtConnectionState.Connecting(resolvedName, address, attempt = 1)
        appendLog(SerialDirection.SYS, "Connecting to $resolvedName ($address)…")

        val result = openSocketWithFallbacks(device)
        if (result == null) {
          val msg = "Could not open SPP link with $resolvedName. Verify HC-05 power & pairing."
          _connectionState.value = BtConnectionState.Error(msg, resolvedName)
          appendLog(SerialDirection.ERR, msg)
          return@launch
        }

        val (socket, transportLabel) = result
        activeSocket = socket
        _connectionState.value =
          BtConnectionState.Connected(
            deviceName = resolvedName,
            address = address,
            connectedAtMillis = System.currentTimeMillis(),
            transportLabel = transportLabel,
          )
        appendLog(SerialDirection.SYS, "Connected via $transportLabel @ ${TivaProtocol.BAUD_RATE} baud")

        startIoLoops(socket, resolvedName, address)
        // Immediately synchronize full state with the TM4C123 board
        sendLine(TivaProtocol.statusQueryCommand())
      }
  }

  @SuppressLint("MissingPermission")
  private fun openSocketWithFallbacks(device: BluetoothDevice): Pair<BluetoothSocket, String>? {
    // Strategy 1: Standard Secure RFCOMM SPP
    try {
      val socket = device.createRfcommSocketToServiceRecord(sppUuid)
      socket.connect()
      return socket to "RFCOMM Secure SPP"
    } catch (e: IOException) {
      appendLog(SerialDirection.SYS, "Secure SPP failed (${e.message}), trying fallback…")
    } catch (se: SecurityException) {
      appendLog(SerialDirection.ERR, "Security exception on SPP connect: ${se.message}")
      return null
    }

    // Strategy 2: Insecure RFCOMM SPP (helps with HC-05 modules using simplified PIN pairing)
    if (insecureFallbackEnabled) {
      try {
        val insecure = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
        insecure.connect()
        return insecure to "RFCOMM Insecure SPP"
      } catch (e: IOException) {
        appendLog(SerialDirection.SYS, "Insecure SPP failed (${e.message}), trying channel 1…")
      } catch (_: SecurityException) {}

      // Strategy 3: Reflection RFCOMM Channel 1 fallback
      try {
        val method =
          device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        val fallbackSocket = method.invoke(device, 1) as? BluetoothSocket
        if (fallbackSocket != null) {
          fallbackSocket.connect()
          return fallbackSocket to "RFCOMM Channel 1"
        }
      } catch (e: Exception) {
        appendLog(SerialDirection.SYS, "Channel 1 fallback failed (${e.message})")
      }
    }

    return null
  }

  private fun startIoLoops(socket: BluetoothSocket, deviceName: String, address: String) {
    readerJob?.cancel()
    writerJob?.cancel()

    val channel = Channel<String>(capacity = 64)
    outgoingChannel = channel

    writerJob =
      scope.launch {
        val output: OutputStream =
          try {
            socket.outputStream
          } catch (e: IOException) {
            handleUnexpectedDisconnect(deviceName, address, "Output stream unavailable")
            return@launch
          }
        for (line in channel) {
          try {
            val payload = (line.trimEnd('\r', '\n') + "\n").toByteArray(Charsets.US_ASCII)
            output.write(payload)
            output.flush()
            appendLog(SerialDirection.TX, line.trimEnd('\r', '\n'))
          } catch (e: IOException) {
            appendLog(SerialDirection.ERR, "TX failed: ${e.message}")
            handleUnexpectedDisconnect(deviceName, address, "Connection lost while sending")
            break
          }
        }
      }

    readerJob =
      scope.launch {
        try {
          val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.US_ASCII))
          while (isActive) {
            val rawLine = reader.readLine() ?: break
            val cleaned = rawLine.trim()
            if (cleaned.isNotEmpty()) {
              _lastPacketMillis.value = System.currentTimeMillis()
              val dir =
                if (cleaned.uppercase().startsWith("ACK:")) SerialDirection.ACK
                else SerialDirection.RX
              appendLog(dir, cleaned)
              _incomingLines.tryEmit(cleaned)
            }
          }
          if (!userInitiatedDisconnect) {
            handleUnexpectedDisconnect(deviceName, address, "HC-05 closed the connection")
          }
        } catch (e: IOException) {
          if (!userInitiatedDisconnect) {
            handleUnexpectedDisconnect(deviceName, address, e.message ?: "Bluetooth link dropped")
          }
        }
      }
  }

  private fun handleUnexpectedDisconnect(deviceName: String, address: String, reason: String) {
    closeActiveSocketOnly()
    if (userInitiatedDisconnect) {
      _connectionState.value = BtConnectionState.Disconnected
      return
    }
    appendLog(SerialDirection.ERR, "Link dropped: $reason")
    if (autoReconnectEnabled && bluetoothAdapter?.isEnabled == true && hasConnectPermission()) {
      scheduleAutoReconnect(deviceName, address)
    } else {
      _connectionState.value = BtConnectionState.Error(reason, deviceName)
    }
  }

  private fun scheduleAutoReconnect(deviceName: String, address: String) {
    reconnectJob?.cancel()
    reconnectJob =
      scope.launch {
        val backoffMs = longArrayOf(2000L, 4000L, 7000L, 12000L)
        for ((index, waitMs) in backoffMs.withIndex()) {
          if (userInitiatedDisconnect || !autoReconnectEnabled) return@launch
          val attempt = index + 1
          _connectionState.value = BtConnectionState.Connecting(deviceName, address, attempt)
          appendLog(SerialDirection.SYS, "Auto-reconnect attempt $attempt/${backoffMs.size} in ${waitMs / 1000}s…")
          delay(waitMs)
          if (userInitiatedDisconnect || bluetoothAdapter?.isEnabled != true) return@launch

          val adapter = bluetoothAdapter ?: break
          val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: break
          val result = withContext(Dispatchers.IO) { openSocketWithFallbacks(device) }
          if (result != null) {
            val (socket, label) = result
            activeSocket = socket
            _connectionState.value =
              BtConnectionState.Connected(
                deviceName = deviceName,
                address = address,
                connectedAtMillis = System.currentTimeMillis(),
                transportLabel = label,
              )
            appendLog(SerialDirection.SYS, "Reconnected via $label")
            startIoLoops(socket, deviceName, address)
            sendLine(TivaProtocol.statusQueryCommand())
            return@launch
          }
        }
        _connectionState.value =
          BtConnectionState.Error("Auto-reconnect failed for $deviceName", deviceName)
      }
  }

  fun sendLine(line: String): Boolean {
    if (_connectionState.value !is BtConnectionState.Connected) {
      appendLog(SerialDirection.ERR, "Cannot send '$line' — not connected")
      return false
    }
    val accepted = outgoingChannel?.trySend(line)?.isSuccess == true
    if (!accepted) {
      appendLog(SerialDirection.ERR, "TX queue full for '$line'")
    }
    return accepted
  }

  fun disconnect() {
    userInitiatedDisconnect = true
    reconnectJob?.cancel()
    connectJob?.cancel()
    closeActiveSocketOnly()
    _connectionState.value = BtConnectionState.Disconnected
    appendLog(SerialDirection.SYS, "Disconnected by user")
  }

  private fun closeActiveSocketOnly() {
    outgoingChannel?.close()
    outgoingChannel = null
    readerJob?.cancel()
    writerJob?.cancel()
    try {
      activeSocket?.close()
    } catch (_: IOException) {}
    activeSocket = null
  }

  fun clearSerialLogs() {
    _serialLogs.value = emptyList()
  }

  fun appendLog(direction: SerialDirection, text: String) {
    val entry =
      SerialLogEntry(
        id = logCounter.incrementAndGet(),
        timestampMillis = System.currentTimeMillis(),
        direction = direction,
        text = text,
      )
    _serialLogs.update { current ->
      val next = current + entry
      if (next.size > 400) next.drop(next.size - 400) else next
    }
  }

  private fun registerReceiver() {
    if (receiverRegistered) return
    val filter =
      IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
      }
    ContextCompat.registerReceiver(
      context,
      btReceiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED,
    )
    receiverRegistered = true
  }

  fun release() {
    userInitiatedDisconnect = true
    stopDiscovery()
    closeActiveSocketOnly()
    if (receiverRegistered) {
      runCatching { context.unregisterReceiver(btReceiver) }
      receiverRegistered = false
    }
    scope.cancel()
  }
}
