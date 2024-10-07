package com.thatwaz.raterwise.ui.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.ui.viewmodel.TimeCardViewModel

@Composable
fun TimeCardScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        timeEntriesByDay.forEach { (date, entries) ->
            item {
                // Create a Date Card that shows total time worked for the date
                DateCard(
                    date = date,
                    timeEntries = entries,
                    onClick = { navController.navigate("daily_entries/$date") }
                )
            }
        }
    }
}

@Composable
fun DateCard(date: String, timeEntries: List<TimeEntry>, onClick: () -> Unit) {
    val totalMinutes = timeEntries.sumOf { it.duration }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

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
            Text("Total Time: $hours hours $minutes minutes", style = MaterialTheme.typography.bodyMedium)
        }
    }
}













