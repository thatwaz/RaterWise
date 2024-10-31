package com.thatwaz.raterwise.ui.screens


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.thatwaz.raterwise.data.model.Session
import com.thatwaz.raterwise.ui.viewmodel.TimeCardViewModel


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()
    Log.d("TimeCardScreen", "Time entries by day: $timeEntriesByDay") // Add this to debug

    // Calculate weekly totals
    val allEntries = timeEntriesByDay.values.flatten()
    val totalSecondsForWeek = allEntries.sumOf { it.totalWorkTime }
//    val numberOfTasksForWeek = allEntries.size
    val numberOfTasksForWeek = allEntries.sumOf { it.numberOfTasks }

    val totalOverUnderAETForWeek = allEntries.sumOf { it.totalOverUnderAET }

    // Create formatted text for weekly totals
    val hoursForWeek = totalSecondsForWeek / 3600
    val minutesForWeek = (totalSecondsForWeek % 3600) / 60
    val weeklyDurationText = when {
        hoursForWeek > 0 -> "$hoursForWeek hr ${minutesForWeek} min"
        else -> "$minutesForWeek min"
    }

    val weeklyOverUnderText = if (totalOverUnderAETForWeek >= 0) {
        "+${totalOverUnderAETForWeek / 60} min" // Display in minutes
    } else {
        "${totalOverUnderAETForWeek / 60} min"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Add the weekly summary card at the top
        item {
            WeeklySummaryCard(
                totalDurationText = weeklyDurationText,
                numberOfTasks = numberOfTasksForWeek,
                totalOverUnderText = weeklyOverUnderText
            )
        }

        if (timeEntriesByDay.isEmpty()) {
            item {
                Text("No entries found", modifier = Modifier.padding(16.dp))
            }
        } else {
            items(timeEntriesByDay.entries.toList()) { (date, sessions) ->
                DateCard(
                    date = date.ifEmpty { viewModel.getCurrentDateFormatted() },
                    sessions = sessions, // Pass the list of sessions to DateCard
                    onClick = {
                        val targetDate = date.ifEmpty { viewModel.getCurrentDateFormatted() }
                        navController.navigate("daily_entries/$targetDate")
                    }
                )
            }
        }
    }
}


@Composable
fun WeeklySummaryCard(totalDurationText: String, numberOfTasks: Int, totalOverUnderText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Total Hours Worked This Week: $totalDurationText", style = MaterialTheme.typography.titleMedium)
            Text("Number of Tasks: $numberOfTasks", style = MaterialTheme.typography.bodyMedium)
            Text("Over/Under AET: $totalOverUnderText", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun DateCard(date: String, sessions: List<Session>, onClick: () -> Unit) {
    // Calculate the total duration and other aggregated information from sessions
    val totalSeconds = sessions.sumOf { it.totalWorkTime }
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    // Create a formatted duration text
    val durationText = when {
        hours > 0 -> "$hours hr ${minutes} min"
        else -> "$minutes min"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }, // Make the card clickable
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Date: $date", style = MaterialTheme.typography.titleMedium)
            Text("Total Time: $durationText", style = MaterialTheme.typography.bodyMedium)
            Text("Number of Sessions: ${sessions.size}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}




//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
//    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()
//    Log.d("TimeCardScreen", "Time entries by day: $timeEntriesByDay") // Add this to debug
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        if (timeEntriesByDay.isEmpty()) {
//            item {
//                Text("No entries found", modifier = Modifier.padding(16.dp))
//            }
//        } else {
//            items(timeEntriesByDay.entries.toList()) { (date, entries) ->
//                DateCard(
//                    date = date.ifEmpty { viewModel.getCurrentDateFormatted() },
//                    timeEntries = entries,
//                    onClick = {
//                        val targetDate = date.ifEmpty { viewModel.getCurrentDateFormatted() }
//                        navController.navigate("daily_entries/$targetDate")
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun DateCard(date: String, timeEntries: List<TaskTimeEntry>, onClick: () -> Unit) {
//    // Calculate the total duration in seconds
//    val totalSeconds = timeEntries.sumOf { it.duration }
//    val hours = totalSeconds / 3600
//    val minutes = (totalSeconds % 3600) / 60
//
//    // Create a formatted duration text
//    val durationText = when {
//        hours > 0 -> "$hours hr ${minutes} min"
//        else -> "$minutes min"
//    }
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp)
//            .clickable { onClick() }, // Make the card clickable
//        elevation = CardDefaults.cardElevation(8.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text("Date: $date", style = MaterialTheme.typography.titleMedium)
//            Text("Total Time: $durationText", style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}



//@Composable
//fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
//    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        timeEntriesByDay.forEach { (date, entries) ->
//            item {
//                // Create a Date Card that shows total time worked for the date
//                DateCard(
//                    date = date,
//                    timeEntries = entries,
//                    onClick = { navController.navigate("daily_entries/$date") }
//                )
//            }
//        }
//    }
//}
//














