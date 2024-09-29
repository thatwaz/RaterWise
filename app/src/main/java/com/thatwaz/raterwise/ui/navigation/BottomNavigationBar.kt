package com.thatwaz.raterwise.ui.navigation


import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.thatwaz.raterwise.R

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_clock), contentDescription = "Time Clock") },
            label = { Text("Time Clock") },
            selected = currentRoute == "time_clock",
            onClick = {
                navController.navigate("time_clock") {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    restoreState = true
                    launchSingleTop = true
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_hourglass), contentDescription = "Time Card") },
            label = { Text("Time Card") },
            selected = currentRoute == "time_card",
            onClick = {
                navController.navigate("time_card") {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    restoreState = true
                    launchSingleTop = true
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_workhistory), contentDescription = "Work History") },
            label = { Text("Work History") },
            selected = currentRoute == "work_history",
            onClick = {
                navController.navigate("work_history") {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    restoreState = true
                    launchSingleTop = true
                }
            }
        )
    }
}
