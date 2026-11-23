package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand & Primary Palette
val TealPrimaryLight = Color(0xFF00897B)
val TealOnPrimaryLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFFCCFBF1)
val TealOnContainerLight = Color(0xFF003831)

val TealPrimaryDark = Color(0xFF2DD4BF)
val TealOnPrimaryDark = Color(0xFF003731)
val TealContainerDark = Color(0xFF0B4F48)
val TealOnContainerDark = Color(0xFF99F6E4)

// Secondary (Electric Slate / Indigo)
val IndigoSecondaryLight = Color(0xFF2563EB)
val IndigoOnSecondaryLight = Color(0xFFFFFFFF)
val IndigoContainerLight = Color(0xFFDBEAFE)
val IndigoOnContainerLight = Color(0xFF1E3A8A)

val IndigoSecondaryDark = Color(0xFF60A5FA)
val IndigoOnSecondaryDark = Color(0xFF0B2545)
val IndigoContainerDark = Color(0xFF1E3A5F)
val IndigoOnContainerDark = Color(0xFFDBEAFE)

// Tertiary (Warm Amber for 220V Lamp)
val AmberTertiaryLight = Color(0xFFD97706)
val AmberOnTertiaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFEF3C7)
val AmberOnContainerLight = Color(0xFF78350F)

val AmberTertiaryDark = Color(0xFFFBBF24)
val AmberOnTertiaryDark = Color(0xFF451A03)
val AmberContainerDark = Color(0xFF5C3409)
val AmberOnContainerDark = Color(0xFFFEF3C7)

// Surfaces & Neutrals
val BackgroundLight = Color(0xFFF4F7FA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE9EFF5)
val OnSurfaceLight = Color(0xFF0F172A)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)

val BackgroundDark = Color(0xFF0B1117)
val SurfaceDark = Color(0xFF131C26)
val SurfaceVariantDark = Color(0xFF1D2937)
val OnSurfaceDark = Color(0xFFE6EDF3)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF334155)
val OutlineVariantDark = Color(0xFF1E293B)

// Error / Thermal Alarm
val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

@Immutable
data class TivaExtendedColors(
  val lampActive: Color,
  val lampContainer: Color,
  val plugActive: Color,
  val plugContainer: Color,
  val safeEmerald: Color,
  val safeContainer: Color,
  val warningAmber: Color,
  val warningContainer: Color,
  val alarmCrimson: Color,
  val alarmContainer: Color,
  val terminalBackground: Color,
  val terminalRx: Color,
  val terminalTx: Color,
  val terminalAck: Color,
  val terminalSystem: Color,
  val cardSubtleBorder: Color,
)

val LightTivaExtendedColors =
  TivaExtendedColors(
    lampActive = Color(0xFFD97706),
    lampContainer = Color(0xFFFEF3C7),
    plugActive = Color(0xFF2563EB),
    plugContainer = Color(0xFFDBEAFE),
    safeEmerald = Color(0xFF059669),
    safeContainer = Color(0xFFD1FAE5),
    warningAmber = Color(0xFFD97706),
    warningContainer = Color(0xFFFEF3C7),
    alarmCrimson = Color(0xFFDC2626),
    alarmContainer = Color(0xFFFEE2E2),
    terminalBackground = Color(0xFF0F172A),
    terminalRx = Color(0xFF34D399),
    terminalTx = Color(0xFF38BDF8),
    terminalAck = Color(0xFFFBBF24),
    terminalSystem = Color(0xFF94A3B8),
    cardSubtleBorder = Color(0xFFE2E8F0),
  )

val DarkTivaExtendedColors =
  TivaExtendedColors(
    lampActive = Color(0xFFFBBF24),
    lampContainer = Color(0xFF452707),
    plugActive = Color(0xFF60A5FA),
    plugContainer = Color(0xFF172C4F),
    safeEmerald = Color(0xFF34D399),
    safeContainer = Color(0xFF063B2C),
    warningAmber = Color(0xFFFBBF24),
    warningContainer = Color(0xFF452707),
    alarmCrimson = Color(0xFFF87171),
    alarmContainer = Color(0xFF5C1515),
    terminalBackground = Color(0xFF070B10),
    terminalRx = Color(0xFF34D399),
    terminalTx = Color(0xFF38BDF8),
    terminalAck = Color(0xFFFBBF24),
    terminalSystem = Color(0xFF94A3B8),
    cardSubtleBorder = Color(0xFF223042),
  )

val LocalTivaExtendedColors = staticCompositionLocalOf { LightTivaExtendedColors }
