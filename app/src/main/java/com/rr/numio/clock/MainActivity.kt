package com.rr.numio.clock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rr.numio.clock.ui.alarm.AlarmFiringScreen
import com.rr.numio.clock.ui.alarm.AlarmScreen
import com.rr.numio.clock.ui.clock.ClockScreen
import com.rr.numio.clock.ui.settings.SettingsScreen
import com.rr.numio.clock.ui.setup.NumioPermissions
import com.rr.numio.clock.ui.setup.PermissionSetupScreen
import com.rr.numio.clock.ui.stopwatch.StopwatchScreen
import com.rr.numio.clock.ui.theme.ClockByNumioTheme
import com.rr.numio.clock.ui.theme.NumioAmber
import com.rr.numio.clock.ui.theme.NumioDark
import com.rr.numio.clock.ui.theme.NumioTextMuted
import com.rr.numio.clock.ui.timer.TimerScreen

sealed class Screen(val route: String, val label: String, val icon: Int) {
    object Clock     : Screen("clock",     "Clock",     R.drawable.ic_clock)
    object Alarm     : Screen("alarm",     "Alarm",     R.drawable.ic_alarm)
    object Timer     : Screen("timer",     "Timer",     R.drawable.ic_timer)
    object Stopwatch : Screen("stopwatch", "Stopwatch", R.drawable.ic_stopwatch)
    object Settings  : Screen("settings",  "Settings",  R.drawable.ic_settings)
}

val bottomNavItems = listOf(
    Screen.Clock,
    Screen.Alarm,
    Screen.Timer,
    Screen.Stopwatch,
    Screen.Settings
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateToFiring = intent?.getBooleanExtra("navigate_to_firing", false) == true
        val alarmHour   = intent?.getIntExtra("alarm_hour", 7) ?: 7
        val alarmMinute = intent?.getIntExtra("alarm_minute", 0) ?: 0
        val alarmLabel  = intent?.getStringExtra("alarm_label") ?: "Alarm"

        // Show the setup screen if a required permission is missing
        // (first launch, after a reinstall, or if the user turned one off)
        val needsSetup = !navigateToFiring && !NumioPermissions.allRequired(this)

        setContent {
            ClockByNumioTheme {
                var showSetup by rememberSaveable { mutableStateOf(needsSetup) }
                if (showSetup) {
                    PermissionSetupScreen(onDone = { showSetup = false })
                } else {
                    MainScaffold(
                        navigateToFiring = navigateToFiring,
                        alarmHour = alarmHour,
                        alarmMinute = alarmMinute,
                        alarmLabel = alarmLabel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}

@Composable
fun MainScaffold(
    navigateToFiring: Boolean = false,
    alarmHour: Int = 7,
    alarmMinute: Int = 0,
    alarmLabel: String = "Alarm"
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(navigateToFiring) {
        if (navigateToFiring) {
            navController.navigate("firing/$alarmHour/$alarmMinute/$alarmLabel")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NumioDark,
        bottomBar = {
            NavigationBar(containerColor = NumioDark) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.icon),
                                contentDescription = screen.label
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NumioAmber,
                            selectedTextColor = NumioAmber,
                            unselectedIconColor = NumioTextMuted,
                            unselectedTextColor = NumioTextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Clock.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Clock.route)     { ClockScreen() }
            composable(Screen.Alarm.route)     { AlarmScreen() }
            composable(Screen.Timer.route)     { TimerScreen() }
            composable(Screen.Stopwatch.route) { StopwatchScreen() }
            composable(Screen.Settings.route)  { SettingsScreen() }
            composable("firing/{hour}/{minute}/{label}") { backStack ->
                AlarmFiringScreen(
                    hour   = backStack.arguments?.getString("hour")?.toInt() ?: alarmHour,
                    minute = backStack.arguments?.getString("minute")?.toInt() ?: alarmMinute,
                    label  = backStack.arguments?.getString("label") ?: alarmLabel
                )
            }
        }
    }
}