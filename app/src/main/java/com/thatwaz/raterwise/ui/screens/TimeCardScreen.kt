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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.thatwaz.raterwise.data.model.SessionWithTasksAndEntries
import com.thatwaz.raterwise.ui.viewmodel.TimeCardViewModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
    // Load current week's entries when the screen is first composed
    LaunchedEffect(viewModel) {
        viewModel.loadCurrentWeekEntries()
    }

    // Collect the filtered entries for the current week
    val currentWeekEntries by viewModel.currentWeekEntries.collectAsState()
    Log.d("TimeCardScreen", "Current week entries: $currentWeekEntries")

    val allEntries = currentWeekEntries.values.flatten()
// Calculate total work time and over/under AET for the week
    val totalSecondsForWeek = allEntries.sumOf { it.session.totalWorkTime.coerceAtLeast(0) }
    val numberOfTasksForWeek = allEntries.sumOf { it.session.numberOfTasks }

    val totalOverUnderAETForWeek = allEntries.sumOf { it.taskEntries.sumOf { task -> task.secondsOverUnderAET } }

    // Format the weekly summary text
    val hoursForWeek = totalSecondsForWeek / 3600
    val minutesForWeek = (totalSecondsForWeek % 3600) / 60
    val weeklyDurationText = if (hoursForWeek > 0L) "$hoursForWeek hr $minutesForWeek min" else "$minutesForWeek min"
    val weeklyOverUnderText = "${totalOverUnderAETForWeek / 60} min"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            WeeklySummaryCard(
                totalDurationText = weeklyDurationText,
                numberOfTasks = numberOfTasksForWeek,
                totalOverUnderText = weeklyOverUnderText
            )
        }

        if (currentWeekEntries.isEmpty()) {
            item { Text("No entries found", modifier = Modifier.padding(16.dp)) }
        } else {
            items(currentWeekEntries.entries.toList()) { (date, sessionsWithTasksAndEntries) ->
                DateCard(
                    date = date,
                    sessionsWithTasksAndEntries = sessionsWithTasksAndEntries,
                    onClick = { navController.navigate("daily_entries/$date") }
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
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateCard(date: String, sessionsWithTasksAndEntries: List<SessionWithTasksAndEntries>, onClick: () -> Unit) {
    val primaryFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a")
    val fallbackFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // Calculate total seconds for the day by summing each session's duration
    val totalSeconds = sessionsWithTasksAndEntries.sumOf { entry ->
        val session = entry.session

        // Parse start time
        val startDateTime = try {
            LocalDateTime.parse("${session.date} ${session.clockInTime}", primaryFormatter)
        } catch (e: DateTimeParseException) {
            LocalDateTime.parse("${session.date} ${session.clockInTime}", fallbackFormatter)
        }

        // Parse end time, or use current time
        val endDateTime = session.clockOutTime?.let {
            try {
                LocalDateTime.parse("${session.date} $it", primaryFormatter).let { end ->
                    if (end.isBefore(startDateTime)) end.plusDays(1) else end
                }
            } catch (e: DateTimeParseException) {
                LocalDateTime.parse("${session.date} $it", fallbackFormatter).let { end ->
                    if (end.isBefore(startDateTime)) end.plusDays(1) else end
                }
            }
        } ?: LocalDateTime.now()

        // Calculate duration
        Duration.between(startDateTime, endDateTime).seconds
    }

    // Format total time for the day
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val durationText = if (hours > 0) "$hours hr $minutes min" else "$minutes min"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Date: $date", style = MaterialTheme.typography.titleMedium)
            Text("Total Time: $durationText", style = MaterialTheme.typography.bodyMedium)
            Text("Number of Sessions: ${sessionsWithTasksAndEntries.size}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}





//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
//    // Load current week's entries when the screen is first composed
//    LaunchedEffect(viewModel) {
//        viewModel.loadCurrentWeekEntries()
//    }
//
//    // Collect the filtered entries for the current week
//    val currentWeekEntries by viewModel.currentWeekEntries.collectAsState()
//    Log.d("TimeCardScreen", "Current week entries: $currentWeekEntries")
//
//    // Calculate totals based on the current week entries
//    val allEntries = currentWeekEntries.values.flatten()
//    allEntries.forEach { entry ->
//        Log.d("TimeCardScreen", "Session totalWorkTime: ${entry.first.totalWorkTime}, Tasks total duration: ${entry.second.sumOf { it.duration }}")
//    }
//
////    val totalSecondsForWeek = allEntries.sumOf { it.first.totalWorkTime + it.second.sumOf { task -> task.duration } }
//    // Calculate total work time without double-counting task durations
//    val totalSecondsForWeek = allEntries.sumOf { it.first.totalWorkTime.coerceAtLeast(0) }
//
////    val totalSecondsForWeek = allEntries.sumOf {
////        (it.first.totalWorkTime + it.second.sumOf { task -> task.duration }).coerceAtLeast(0)
////    }
//
//    val numberOfTasksForWeek = allEntries.sumOf { it.first.numberOfTasks }
//    val totalOverUnderAETForWeek = allEntries.sumOf { it.second.sumOf { task -> task.secondsOverUnderAET } }
//
//    // Format the weekly summary text
//    val hoursForWeek = totalSecondsForWeek / 3600
//    val minutesForWeek = (totalSecondsForWeek % 3600) / 60
//    val weeklyDurationText = if (hoursForWeek > 0L) "$hoursForWeek hr ${minutesForWeek} min" else "$minutesForWeek min"
//    val weeklyOverUnderText = "${totalOverUnderAETForWeek / 60} min"
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        item {
//            WeeklySummaryCard(
//                totalDurationText = weeklyDurationText,
//                numberOfTasks = numberOfTasksForWeek,
//                totalOverUnderText = weeklyOverUnderText
//            )
//        }
//
//        if (currentWeekEntries.isEmpty()) {
//            item { Text("No entries found", modifier = Modifier.padding(16.dp)) }
//        } else {
//            items(currentWeekEntries.entries.toList()) { (date, sessions) ->
//                DateCard(
//                    date = date,
//                    sessions = sessions.map { it.first },
//                    onClick = { navController.navigate("daily_entries/$date") }
//                )
//            }
//        }
//    }
//}
//
//
//
//
//@Composable
//fun WeeklySummaryCard(totalDurationText: String, numberOfTasks: Int, totalOverUnderText: String) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        elevation = CardDefaults.cardElevation(8.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text("Total Hours Worked This Week: $totalDurationText", style = MaterialTheme.typography.titleMedium)
//            Text("Number of Tasks: $numberOfTasks", style = MaterialTheme.typography.bodyMedium)
//            Text("Over/Under AET: $totalOverUnderText", style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun DateCard(date: String, sessions: List<Session>, onClick: () -> Unit) {
//    val totalSeconds = sessions.sumOf { session ->
//        // Parse start and end times with "h:mm a" pattern
//        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a")
//
//        val startDateTime = LocalDateTime.parse("${session.date} ${session.clockInTime}", formatter)
//
//        val endDateTime = if (session.clockOutTime != null) {
//            val potentialEndDateTime = LocalDateTime.parse("${session.date} ${session.clockOutTime}", formatter)
//
//            if (potentialEndDateTime.isBefore(startDateTime)) {
//                potentialEndDateTime.plusDays(1) // Add a day if the end time is after midnight
//            } else {
//                potentialEndDateTime
//            }
//        } else {
//            LocalDateTime.now() // Use current time if session is ongoing
//        }
//
//        Duration.between(startDateTime, endDateTime).seconds
//    }
//
//    val hours = totalSeconds / 3600
//    val minutes = (totalSeconds % 3600) / 60
//    val durationText = if (hours > 0) "$hours hr ${minutes} min" else "$minutes min"
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp)
//            .clickable { onClick() },
//        elevation = CardDefaults.cardElevation(8.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text("Date: $date", style = MaterialTheme.typography.titleMedium)
//            Text("Total Time: $durationText", style = MaterialTheme.typography.bodyMedium)
//            Text("Number of Sessions: ${sessions.size}", style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}




//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
//    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()
//    val currentWeekEntries = getCurrentWeekEntries(timeEntriesByDay)
//
//    // Calculate weekly totals for the current week
//    val allEntries = currentWeekEntries.values.flatten()
//    val totalSecondsForWeek = allEntries.sumOf { it.totalWorkTime }
//    val numberOfTasksForWeek = allEntries.sumOf { it.numberOfTasks }
//    val totalOverUnderAETForWeek = allEntries.sumOf { it.totalOverUnderAET }
//
//    // Create formatted text for weekly totals
//    val hoursForWeek = totalSecondsForWeek / 3600
//    val minutesForWeek = (totalSecondsForWeek % 3600) / 60
//    val minutes = if (hoursForWeek == 0L) minutesForWeek else 0
//    val weeklyDurationText = if (hoursForWeek > 0L) {
//        "$hoursForWeek hr ${minutesForWeek} min"
//    } else {
//        "$minutes min"
//    }
//
//    val weeklyOverUnderText = if (totalOverUnderAETForWeek >= 0) {
//        "+${totalOverUnderAETForWeek / 60} min"
//    } else {
//        "${totalOverUnderAETForWeek / 60} min"
//    }
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        // Button to navigate to previous weeks
//        item {
//            Button(
//                onClick = { navController.navigate("previous_weeks") },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("View Previous Weeks")
//            }
//        }
//
//        // Add the weekly summary card at the top
//        item {
//            WeeklySummaryCard(
//                totalDurationText = weeklyDurationText,
//                numberOfTasks = numberOfTasksForWeek,
//                totalOverUnderText = weeklyOverUnderText
//            )
//        }
//
//        if (currentWeekEntries.isEmpty()) {
//            item {
//                Text("No entries found", modifier = Modifier.padding(16.dp))
//            }
//        } else {
//            items(currentWeekEntries.entries.toList()) { (date, sessions) ->
//                DateCard(
//                    date = date,
//                    sessions = sessions,
//                    onClick = {
//                        navController.navigate("daily_entries/$date")
//                    }
//                )
//            }
//        }
//    }
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//private fun getCurrentWeekEntries(entries: Map<String, List<Session>>): Map<String, List<Session>> {
//    val today = LocalDate.now()
//
//    // Adjust the start of the week to the most recent Sunday
//    val startOfWeek = today.with(DayOfWeek.SUNDAY)
//    // Set the end of the week to Saturday of the current week
//    val endOfWeek = startOfWeek.plusDays(6)
//
//    return entries.filter { (date, _) ->
//        val entryDate = LocalDate.parse(date)
//        entryDate in startOfWeek..endOfWeek
//    }
//}
//
//
//
//@Composable
//fun WeeklySummaryCard(totalDurationText: String, numberOfTasks: Int, totalOverUnderText: String) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        elevation = CardDefaults.cardElevation(8.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text("Total Hours Worked This Week: $totalDurationText", style = MaterialTheme.typography.titleMedium)
//            Text("Number of Tasks: $numberOfTasks", style = MaterialTheme.typography.bodyMedium)
//            Text("Over/Under AET: $totalOverUnderText", style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}
//
//@Composable
//fun DateCard(date: String, sessions: List<Session>, onClick: () -> Unit) {
//    val totalSeconds = sessions.sumOf { it.totalWorkTime }
//    val hours = totalSeconds / 3600
//    val minutes = (totalSeconds % 3600) / 60
//    val durationText = if (hours > 0) "$hours hr ${minutes} min" else "$minutes min"
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp)
//            .clickable { onClick() },
//        elevation = CardDefaults.cardElevation(8.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text("Date: $date", style = MaterialTheme.typography.titleMedium)
//            Text("Total Time: $durationText", style = MaterialTheme.typography.bodyMedium)
//            Text("Number of Sessions: ${sessions.size}", style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}

//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
//    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()
//    Log.d("TimeCardScreen", "Time entries by day: $timeEntriesByDay") // Add this to debug
//
//    // Calculate weekly totals
//    val allEntries = timeEntriesByDay.values.flatten()
//    val totalSecondsForWeek = allEntries.sumOf { it.totalWorkTime }
////    val numberOfTasksForWeek = allEntries.size
//    val numberOfTasksForWeek = allEntries.sumOf { it.numberOfTasks }
//
//    val totalOverUnderAETForWeek = allEntries.sumOf { it.totalOverUnderAET }
//
//    // Create formatted text for weekly totals
//    val hoursForWeek = totalSecondsForWeek / 3600
//    val minutesForWeek = (totalSecondsForWeek % 3600) / 60
//    val weeklyDurationText = when {
//        hoursForWeek > 0 -> "$hoursForWeek hr ${minutesForWeek} min"
//        else -> "$minutesForWeek min"
//    }
//
//    val weeklyOverUnderText = if (totalOverUnderAETForWeek >= 0) {
//        "+${totalOverUnderAETForWeek / 60} min" // Display in minutes
//    } else {
//        "${totalOverUnderAETForWeek / 60} min"
//    }
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        // Add the weekly summary card at the top
//        item {
//            WeeklySummaryCard(
//                totalDurationText = weeklyDurationText,
//                numberOfTasks = numberOfTasksForWeek,
//                totalOverUnderText = weeklyOverUnderText
//            )
//        }
//
//        if (timeEntriesByDay.isEmpty()) {
//            item {
//                Text("No entries found", modifier = Modifier.padding(16.dp))
//            }
//        } else {
//            items(timeEntriesByDay.entries.toList()) { (date, sessions) ->
//                DateCard(
//                    date = date.ifEmpty { viewModel.getCurrentDateFormatted() },
//                    sessions = sessions, // Pass the list of sessions to DateCard
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
//
//@Composable
//fun WeeklySummaryCard(totalDurationText: String, numberOfTasks: Int, totalOverUnderText: String) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        elevation = CardDefaults.cardElevation(8.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text("Total Hours Worked This Week: $totalDurationText", style = MaterialTheme.typography.titleMedium)
//            Text("Number of Tasks: $numberOfTasks", style = MaterialTheme.typography.bodyMedium)
//            Text("Over/Under AET: $totalOverUnderText", style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}
//
//@Composable
//fun DateCard(date: String, sessions: List<Session>, onClick: () -> Unit) {
//    // Calculate the total duration and other aggregated information from sessions
//    val totalSeconds = sessions.sumOf { it.totalWorkTime }
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
//            Text("Number of Sessions: ${sessions.size}", style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}




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














