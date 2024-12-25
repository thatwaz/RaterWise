package com.thatwaz.raterwise.ui.screens


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.thatwaz.raterwise.data.model.SessionWithTasksAndEntries
import com.thatwaz.raterwise.ui.viewmodel.TimeCardViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
    // Start the week on Sunday instead of Monday

    var selectedWeekStart by remember {
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)))
    }


    LaunchedEffect(selectedWeekStart) {
        val weekEnd = selectedWeekStart.plusDays(6)
        viewModel.loadEntriesForWeek(selectedWeekStart, weekEnd) // Load the selected week's data
    }


    // Collect the filtered entries for the selected week
    val currentWeekEntries by viewModel.currentWeekEntries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Time Entries",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${selectedWeekStart.format(DateTimeFormatter.ofPattern("E MM/dd"))} - " +
                                    "${selectedWeekStart.plusDays(6).format(DateTimeFormatter.ofPattern("E MM/dd"))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                actions = {
                    Row {
                        IconButton(onClick = {
                            selectedWeekStart = selectedWeekStart.minusWeeks(1)
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Week")
                        }
                        IconButton(onClick = {
                            selectedWeekStart = selectedWeekStart.plusWeeks(1)
                        }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Week")
                        }
                    }
                }
            )
        }
    )

    { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                WeeklySummaryCard(
                    totalDurationText = calculateWeeklyDuration(currentWeekEntries.values.flatten()),
                    numberOfTasks = currentWeekEntries.values.flatten().sumOf { it.session.numberOfTasks },
                    totalOverUnderText = calculateOverUnderText(currentWeekEntries.values.flatten())
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
}

private fun calculateWeeklyDuration(allEntries: List<SessionWithTasksAndEntries>): String {
    val totalSecondsForWeek = allEntries.sumOf { it.session.totalWorkTime.coerceAtLeast(0) }
    val hoursForWeek = totalSecondsForWeek / 3600
    val minutesForWeek = (totalSecondsForWeek % 3600) / 60
    return if (hoursForWeek > 0L) "$hoursForWeek hr $minutesForWeek min" else "$minutesForWeek min"
}

private fun calculateOverUnderText(allEntries: List<SessionWithTasksAndEntries>): String {
    val totalOverUnderAETForWeek = allEntries.sumOf { it.session.totalOverUnderAET }
    return "${totalOverUnderAETForWeek / 60} min"
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
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val displayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d") // Short day name (e.g., Sun)

    val parsedDate = LocalDate.parse(date, formatter)
    val formattedDate = parsedDate.format(displayFormatter) // Format with day name

    // Calculate total time worked
    val totalSeconds = sessionsWithTasksAndEntries.sumOf { entry ->
        entry.session.totalWorkTime.coerceAtLeast(0)
    }
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val durationText = if (hours > 0) "$hours hr $minutes min" else "$minutes min"

    // Check submission status
    val allSubmitted = sessionsWithTasksAndEntries.all { it.session.isSubmitted }
    val statusText = if (allSubmitted) "All Submitted" else "Not All Submitted"
    val statusColor = if (allSubmitted) Color.Green else Color.Red

    // Highlight current day
    val isToday = parsedDate == LocalDate.now()
    val backgroundColor = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
            .background(backgroundColor),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(formattedDate, style = MaterialTheme.typography.titleMedium) // Short day + date
            Text("Total Time: $durationText", style = MaterialTheme.typography.bodyMedium)
            Text("Number of Sessions: ${sessionsWithTasksAndEntries.size}", style = MaterialTheme.typography.bodyMedium)

            // Submission status
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}


















