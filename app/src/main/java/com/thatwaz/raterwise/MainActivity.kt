package com.thatwaz.raterwise

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thatwaz.raterwise.ui.navigation.BottomNavigationBar
import com.thatwaz.raterwise.ui.screens.DailyTimeEntriesScreen
import com.thatwaz.raterwise.ui.screens.HomeScreen
import com.thatwaz.raterwise.ui.screens.TimeCardScreen
import com.thatwaz.raterwise.ui.screens.WorkHistoryScreen
import com.thatwaz.raterwise.ui.theme.RaterWiseTheme
import dagger.hilt.android.AndroidEntryPoint

//Start Date 08/16/2024
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RaterWiseTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavigationBar(navController = navController) }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "time_clock",
        modifier = modifier
    ) {
        // HomeScreen (Time Clock)
        composable("time_clock") {
            HomeScreen(navController = navController)
        }

        // TimeCardScreen (Time Card) with ViewModel Injection
        composable("time_card") {
            // Obtain ViewModel using Hilt in the Composable itself
            TimeCardScreen(navController = navController)
        }

        // Work History Screen
        composable("work_history") {
            WorkHistoryScreen(navController = navController)
        }

        // DailyTimeEntriesScreen with Date Argument
        composable(
            route = "daily_entries/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            DailyTimeEntriesScreen(date = date, navController = navController)
        }
    }
}








