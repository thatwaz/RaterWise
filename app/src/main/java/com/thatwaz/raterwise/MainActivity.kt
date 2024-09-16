package com.thatwaz.raterwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thatwaz.raterwise.ui.screens.HomeScreen
import com.thatwaz.raterwise.ui.theme.RaterWiseTheme
import dagger.hilt.android.AndroidEntryPoint

//Start Date 08/16/2024
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RaterWiseTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                // Remove navigation to details, so no onNavigateToDetails is needed
            )
        }
        // If details screen is no longer needed, remove the composable for it
        // If you plan to add it back in the future, you can comment it out or remove entirely.
        // composable(
        //     route = "details/{taskId}",
        //     arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        // ) { backStackEntry ->
        //     val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
        //     DetailsScreen(taskId = taskId) // Display task details
        // }
    }
}
