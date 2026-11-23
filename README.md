# TivaHome — Bluetooth Smart Home Controller & Telemetry

**TivaHome** is a modern Android companion application built with **Kotlin** and **Jetpack Compose (Material 3)** for the **CSE322: Introduction to Embedded Systems — Home Appliances Control** prototype. It communicates over **Classic Bluetooth SPP (9600 baud ASCII)** with an **HC-05** module wired to a **TI Tiva C Series TM4C123GH6PM LaunchPad**, providing real-time relay control, 1 Hz thermal monitoring, timestamped door activity logging, and built-in hardware debugging tools.

> **100% Offline & Keyless:** TivaHome requires no API keys, cloud accounts, or internet access. All telemetry, door logs, and preferences stay locally on the device.

---

## Features

### Smart Home Control & Live Telemetry
- **Live Room Hero & Master Scene Controls:** Interactive Canvas room overview reflecting real-time lamp glow, energized plug status, magnetic door position, and Bluetooth link health, complete with **All On**, **All Off**, and **Sync (`STATUS?`)** quick actions with haptic feedback.
- **220V Appliance Relay Cards:** Independent switches for the **220V Lamp (`PA2`)** (wired in parallel with the manual wall switch) and **Power Plug (`PA3`)**, featuring pending-state spinners until `ACK:` confirmation arrives from the board.
- **LM35 Room Temperature & Thermal Gauge:** Live **1 Hz** room temperature telemetry (`PE3 / AIN0`) displayed via a spring-animated **240° radial gauge** and a **60-second Canvas sparkline** with a threshold marker.
- **Configurable Thermal Alerts:** Adjust the temperature limit (`10–80 °C`) using steppers or the slider and push `LIMIT:<whole_number>` to the board. Exceeding the limit—or receiving `ALARM:1`—triggers an animated high-priority alert banner with optional alarm chime, haptic vibration, and per-episode mute.
- **Timestamped Door Activity Log:** Every `DOOR:OPEN` and `DOOR:CLOSED` transition from the magnetic door switch (`PA4`) is stamped with the phone's clock and persisted locally in **Room**, grouped by date (*Today*, *Yesterday*, date) with status filters and one-tap **CSV ShareSheet export**.

### Resilient Bluetooth SPP & Engineering Tools
- **One-Tap Quick-Connect Sheet:** Connect or scan for HC-05 modules directly from a `ModalBottomSheet` over the Dashboard, with inline permission and Bluetooth-enable prompts.
- **Multi-Strategy RFCOMM Connectivity:** Uses standard SPP UUID (`00001101-0000-1000-8000-00805F9B34FB`) with automatic fallbacks (Secure SPP → Insecure SPP → Channel 1) and configurable exponential-backoff auto-reconnect.
- **Hybrid Navigation (Bottom Bar + 3-Line Drawer):**
  - **Bottom Navigation Bar** (adapting to a side **Navigation Rail** on tablets/wide screens) for instant switching between **Home**, **Door Log**, **Connect**, and **Terminal**.
  - **3-Line Navigation Drawer** housing all destinations and engineering utilities:
    - **Home Dashboard**
    - **Door Activity Log**
    - **Bluetooth & Connections** (paired list, nearby scanner, transport diagnostics)
    - **Serial Terminal** (color-coded `RX`/`TX`/`ACK`/`SYS`/`ERR` 9600-baud monitor, packet counters, quick-send chips, tap-to-copy lines, raw ASCII sender)
    - **Pin Map & Protocol** (filterable TM4C123GH6PM pinout, power/common-ground checklist, ASCII protocol reference)
    - **Settings** (thermal limit, `°C` / `°F` display unit, sound & vibration toggles, auto-reconnect, RFCOMM fallback, **System / Light / Dark** Material 3 theme)

---

## Hardware Pin Map (TM4C123GH6PM)

Power the 2-channel 5V relay module, LM35, and HC-05 from an external **5 V (≥ 1 A)** supply, and connect the external supply's **GND** to a **GND** pin on the LaunchPad so all grounds are common. Power the LaunchPad itself via USB.

| Component | Module Pin | TM4C123GH6PM Pin |
| :--- | :--- | :--- |
| **Lamp Relay Module** | `IN` | `PA2` |
| **Plug Relay Module** | `IN` | `PA3` |
| **Magnetic Door Switch** | One wire *(other wire to GND)* | `PA4` |
| **LM35 Temperature Sensor** | `OUT` | `PE3` (`AIN0`) |
| **HC-05 Bluetooth Module** | `TXD` | `PB0` (`U1Rx`) |
| **HC-05 Bluetooth Module** | `RXD` | `PB1` (`U1Tx`) |

---

## Bluetooth UART Protocol (9600 Baud ASCII)

All frames are newline-terminated (`\n`) ASCII strings at **9600 baud (8N1)**.

| Direction | Message | Meaning |
| :--- | :--- | :--- |
| **App → Board** | `LAMP:1` / `LAMP:0` | Switch lamp relay on or off |
| **App → Board** | `PLUG:1` / `PLUG:0` | Switch power plug relay on or off |
| **App → Board** | `LIMIT:30` | Set temperature alert limit in °C (whole number) |
| **App → Board** | `STATUS?` | Request current lamp, plug, door, temperature, and alarm state |
| **Board → App** | `TEMP:27.5` | LM35 room temperature in °C (sent every second) |
| **Board → App** | `DOOR:OPEN` / `DOOR:CLOSED` | Sent on every magnetic door switch change |
| **Board → App** | `ALARM:1` / `ALARM:0` | `1` when temperature > limit, `0` when ≤ limit |
| **Board → App** | `ACK:LAMP:1` | Confirms command receipt by echoing the command |

---

## Architecture & Tech Stack

```text
┌──────────────────────────────────────────────────────────────────────┐
│                     Jetpack Compose UI (Material 3)                  │
│   Adaptive Shell (Bottom Bar / Navigation Rail + 3-Line Drawer)      │
│   Dashboard • Door Log • Quick-Connect Sheet • Terminal • Pin Map    │
└──────────────────────────────────┬───────────────────────────────────┘
                                   │ StateFlow (collectAsStateWithLifecycle)
┌──────────────────────────────────▼───────────────────────────────────┐
│                   HomeControlViewModel + TivaProtocol                │
│   • Command queue & ACK tracking   • Defensive multi-format parser   │
│   • Alarm edge detection           • Rolling 60s thermal buffer      │
└──────────────┬───────────────────────────────────┬───────────────────┘
               │                                   │
┌──────────────▼───────────────┐     ┌─────────────▼───────────────────┐
│     BluetoothSppManager      │     │     Local Persistence Layer     │
│ • Classic RFCOMM + Fallbacks │     │ • Room DB (DoorEventEntity)     │
│ • Paired & Nearby Discovery  │     │ • DataStore (AppSettings)       │
│ • Auto-Reconnect Backoff     │     │ • ThermalAlertNotifier          │
└──────────────────────────────┘     └─────────────────────────────────┘
```

- **Language:** Kotlin (`Java 11` target)
- **UI Toolkit:** Jetpack Compose, Material 3, Navigation Compose, Custom Canvas Gauges & Sparklines
- **Typography:** Locally bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`)
- **Persistence:** Room (`KSP`) for door event logs + Jetpack DataStore Preferences for settings
- **Concurrency:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Testing:** JUnit 4 + Robolectric

---

## Permissions

TivaHome follows least-privilege Android permission practices:
- **Android 12+ (API 31+):** `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (with `neverForLocation` flag)
- **Android 11 and lower (API ≤ 30):** `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` (scoped with `maxSdkVersion="30"` for Classic Bluetooth discovery)
- **Install-time Normal Permission:** `VIBRATE` (for optional haptic thermal alarm feedback)

---

## Building & Testing

1. **Open in Android Studio** (Android Gradle Plugin `9.1.1`, Compile/Target SDK `36`, Min SDK `24`).
2. **Build Debug APK:**
   ```bash
   gradle assembleDebug
   ```
3. **Run Unit & Robolectric Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## License

This project is licensed under the [MIT License](LICENSE).
