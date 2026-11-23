package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.bluetooth.BtConnectionState
import com.example.ui.components.ConnectionStatusPill
import com.example.ui.components.QuickConnectBottomSheet
import com.example.ui.components.ThermalAlertBanner
import com.example.ui.components.TivaDrawerContent
import com.example.ui.navigation.TivaRoute
import com.example.ui.screens.BluetoothScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DoorLogScreen
import com.example.ui.screens.PinMapScreen
import com.example.ui.screens.SerialTerminalScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.HomeControlViewModel
import kotlinx.coroutines.launch

private val PrimaryBarRoutes =
  listOf(
    TivaRoute.DASHBOARD to "Home",
    TivaRoute.DOOR_LOG to "Door Log",
    TivaRoute.BLUETOOTH to "Connect",
    TivaRoute.TERMINAL to "Terminal",
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TivaHomeApp(viewModel: HomeControlViewModel) {
  val navController = rememberNavController()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var showQuickConnectSheet by remember { mutableStateOf(false) }

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
  val doorEvents by viewModel.doorEvents.collectAsStateWithLifecycle()
  val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
  val isBluetoothSupported by viewModel.isBluetoothSupported.collectAsStateWithLifecycle()
  val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
  val hasPermissions by viewModel.hasPermissions.collectAsStateWithLifecycle()
  val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
  val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
  val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
  val serialLogs by viewModel.serialLogs.collectAsStateWithLifecycle()
  val lastPacketMillis by viewModel.lastPacketMillis.collectAsStateWithLifecycle()
  val isAlarmActive by viewModel.isEffectiveAlarmActive.collectAsStateWithLifecycle()

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = TivaRoute.fromRoute(navBackStackEntry?.destination?.route)

  fun navigateTo(route: TivaRoute) {
    if (currentRoute != route) {
      navController.navigate(route.route) {
        popUpTo(TivaRoute.DASHBOARD.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }
  }

  LaunchedEffect(Unit) {
    viewModel.snackbarEvents.collect { msg ->
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(msg)
    }
  }

  BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

  QuickConnectBottomSheet(
    visible = showQuickConnectSheet,
    isBluetoothSupported = isBluetoothSupported,
    isBluetoothEnabled = isBluetoothEnabled,
    hasPermissions = hasPermissions,
    requiredPermissions = viewModel.requiredBluetoothPermissions(),
    connectionState = connectionState,
    pairedDevices = pairedDevices,
    discoveredDevices = discoveredDevices,
    isScanning = isScanning,
    onDismiss = { showQuickConnectSheet = false },
    onPermissionsResult = viewModel::onPermissionsResult,
    onRefreshState = viewModel::refreshBluetoothState,
    onStartScan = viewModel::startDiscovery,
    onStopScan = viewModel::stopDiscovery,
    onConnectDevice = { device -> viewModel.connectToDevice(device.address, device.name) },
    onDisconnect = viewModel::disconnect,
    onSyncStatus = viewModel::requestStatusSync,
    onOpenFullBluetoothScreen = { navigateTo(TivaRoute.BLUETOOTH) },
  )

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      TivaDrawerContent(
        currentRoute = currentRoute,
        connectionState = connectionState,
        lampOn = telemetry.lampOn,
        plugOn = telemetry.plugOn,
        onSelectRoute = { target ->
          scope.launch { drawerState.close() }
          navigateTo(target)
        },
      )
    },
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useRail = maxWidth >= 600.dp

      Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          TopAppBar(
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
              ),
            navigationIcon = {
              IconButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier.testTag("drawer_menu_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Menu,
                  contentDescription = stringResource(R.string.nav_open_drawer),
                )
              }
            },
            title = {
              Text(
                text =
                  if (currentRoute == TivaRoute.DASHBOARD) stringResource(R.string.app_name)
                  else currentRoute.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            actions = {
              ConnectionStatusPill(
                connectionState = connectionState,
                onClick = {
                  viewModel.refreshBluetoothState()
                  showQuickConnectSheet = true
                },
                modifier = Modifier.padding(end = 12.dp),
              )
            },
          )
        },
        bottomBar = {
          if (!useRail) {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
              PrimaryBarRoutes.forEach { (route, shortLabel) ->
                val selected = currentRoute == route
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTo(route) },
                  icon = {
                    val icon = if (selected) route.selectedIcon else route.unselectedIcon
                    if (route == TivaRoute.DOOR_LOG && doorEvents.isNotEmpty()) {
                      BadgedBox(
                        badge = {
                          Badge {
                            Text(
                              text = if (doorEvents.size > 99) "99+" else doorEvents.size.toString()
                            )
                          }
                        }
                      ) {
                        Icon(icon, contentDescription = route.title)
                      }
                    } else {
                      Icon(icon, contentDescription = route.title)
                    }
                  },
                  label = {
                    Text(
                      text = shortLabel,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  },
                  modifier = Modifier.testTag("bottom_nav_${route.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useRail) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxHeight(),
            ) {
              Spacer(modifier = Modifier.weight(0.2f))
              PrimaryBarRoutes.forEach { (route, shortLabel) ->
                val selected = currentRoute == route
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateTo(route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) route.selectedIcon else route.unselectedIcon,
                      contentDescription = route.title,
                    )
                  },
                  label = { Text(shortLabel) },
                  modifier = Modifier.testTag("rail_nav_${route.route}"),
                )
              }
              Spacer(modifier = Modifier.weight(1f))
            }
          }

          Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            ThermalAlertBanner(
              visible = isAlarmActive,
              temperatureCelsius = telemetry.temperatureCelsius,
              limitCelsius = settings.tempLimitCelsius,
              useFahrenheit = settings.useFahrenheit,
              muted = telemetry.alarmMutedForEpisode,
              onMuteClick = viewModel::muteCurrentAlarm,
            )

            NavHost(
              navController = navController,
              startDestination = TivaRoute.DASHBOARD.route,
              modifier = Modifier.weight(1f),
            ) {
              composable(TivaRoute.DASHBOARD.route) {
                DashboardScreen(
                  telemetry = telemetry,
                  settings = settings,
                  connectionState = connectionState,
                  latestDoorEvent = doorEvents.firstOrNull(),
                  lastPacketMillis = lastPacketMillis,
                  isAlarmActive = isAlarmActive,
                  onToggleLamp = viewModel::toggleLamp,
                  onTogglePlug = viewModel::togglePlug,
                  onSetAllAppliances = viewModel::setAllAppliances,
                  onApplyLimit = viewModel::applyTemperatureLimit,
                  onSyncStatus = viewModel::requestStatusSync,
                  onOpenQuickConnect = {
                    viewModel.refreshBluetoothState()
                    showQuickConnectSheet = true
                  },
                  onOpenDoorLogScreen = { navigateTo(TivaRoute.DOOR_LOG) },
                )
              }

              composable(TivaRoute.DOOR_LOG.route) {
                DoorLogScreen(
                  events = doorEvents,
                  onClearHistory = viewModel::clearDoorHistory,
                )
              }

              composable(TivaRoute.BLUETOOTH.route) {
                BluetoothScreen(
                  isBluetoothSupported = isBluetoothSupported,
                  isBluetoothEnabled = isBluetoothEnabled,
                  hasPermissions = hasPermissions,
                  requiredPermissions = viewModel.requiredBluetoothPermissions(),
                  connectionState = connectionState,
                  pairedDevices = pairedDevices,
                  discoveredDevices = discoveredDevices,
                  isScanning = isScanning,
                  lastPacketMillis = lastPacketMillis,
                  onPermissionsResult = viewModel::onPermissionsResult,
                  onRefreshState = viewModel::refreshBluetoothState,
                  onStartScan = viewModel::startDiscovery,
                  onStopScan = viewModel::stopDiscovery,
                  onConnectDevice = { device ->
                    viewModel.connectToDevice(device.address, device.name)
                  },
                  onDisconnect = viewModel::disconnect,
                  onSyncStatus = viewModel::requestStatusSync,
                )
              }

              composable(TivaRoute.TERMINAL.route) {
                SerialTerminalScreen(
                  logs = serialLogs,
                  connectionState = connectionState,
                  onSendCommand = viewModel::sendRawSerialCommand,
                  onClearLogs = viewModel::clearSerialLog,
                )
              }

              composable(TivaRoute.PIN_MAP.route) { PinMapScreen() }

              composable(TivaRoute.SETTINGS.route) {
                SettingsScreen(
                  settings = settings,
                  isConnected = connectionState is BtConnectionState.Connected,
                  onApplyTempLimit = viewModel::applyTemperatureLimit,
                  onToggleFahrenheit = viewModel::setUseFahrenheit,
                  onToggleAutoReconnect = viewModel::setAutoReconnect,
                  onToggleInsecureFallback = viewModel::setInsecureFallback,
                  onToggleAlarmSound = viewModel::setAlarmSoundEnabled,
                  onToggleAlarmVibration = viewModel::setAlarmVibrationEnabled,
                  onSelectThemeMode = viewModel::setThemeMode,
                  onTestAlert = viewModel::testThermalAlertFeedback,
                  onClearDoorHistory = viewModel::clearDoorHistory,
                )
              }
            }
          }
        }
      }
    }
  }
}
