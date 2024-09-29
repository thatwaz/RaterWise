package com.thatwaz.raterwise.ui.screens


import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCardScreen(navController: NavController) {
    val workedDays = remember {
        mutableStateListOf(
            DailyWorkSummary(
                "2024-09-15", 120L, false,
                timeEntries = mutableListOf(
                    TimeEntry("09:00 AM", "10:00 AM", 60, false),
                    TimeEntry("01:00 PM", "02:00 PM", 60, true)
                )
            ),
            DailyWorkSummary(
                "2024-09-14", 75L, true,
                timeEntries = mutableListOf(
                    TimeEntry("08:00 AM", "09:15 AM", 75, true)
                )
            ),
            DailyWorkSummary(
                "2024-09-13", 45L, false,
                timeEntries = mutableListOf(
                    TimeEntry("12:00 PM", "12:45 PM", 45, false)
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Time Card", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(workedDays) { day ->
                    DaySummaryItem(day = day, navController = navController)
                }
            }
        }
    }
}

@Composable
fun DaySummaryItem(day: DailyWorkSummary, navController: NavController) {
    val borderColor = if (day.timeEntries.all { it.isSubmitted }) Color.Green else Color.Red

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, borderColor) // Change border color based on submission status
            .clickable {
                // Navigate to DailyTimeEntriesScreen with the selected day’s data
                navController.navigate("daily_entries/${day.date}")
            },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Date: ${day.date}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Total Time Worked: ${day.timeWorked / 60} hours ${day.timeWorked % 60} minutes",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "All Entries Submitted: ${if (day.isSubmitted) "Yes" else "No"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


// Composable to display each day's summary
@Composable
fun DaySummaryItem(day: DailyWorkSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Date: ${day.date}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Time Worked: ${day.timeWorked / 60} hours ${day.timeWorked % 60} minutes",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Submitted: ${if (day.isSubmitted) "Yes" else "No"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Data class representing daily work summary
data class TimeEntry(
    val startTime: String, // Start time of the work session
    val endTime: String,   // End time of the work session
    val duration: Long,    // Duration in minutes
    var isSubmitted: Boolean // Indicates whether this time entry has been submitted
)

data class DailyWorkSummary(
    val date: String,
    val timeWorked: Long, // Total time worked in minutes for this day
    var isSubmitted: Boolean, // Indicates if all time entries for this day are submitted
    val timeEntries: MutableList<TimeEntry> // List of individual time entries
)




