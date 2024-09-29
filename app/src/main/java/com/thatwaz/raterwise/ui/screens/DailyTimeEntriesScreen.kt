package com.thatwaz.raterwise.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTimeEntriesScreen(date: String, navController: NavController) {
    // Retrieve the list of time entries based on the date (mock data here for illustration)
    val entries = remember {
        mutableStateListOf(
            TimeEntry("09:00 AM", "10:00 AM", 60, false),
            TimeEntry("01:00 PM", "02:00 PM", 60, true)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Time Entries for $date", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
            Text(
                text = "Unsubmitted Entries",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            LazyColumn {
                items(entries.filter { !it.isSubmitted }) { entry ->
                    TimeEntryItem(entry)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Submitted Entries",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            LazyColumn {
                items(entries.filter { it.isSubmitted }) { entry ->
                    TimeEntryItem(entry)
                }
            }
        }
    }
}

@Composable
fun TimeEntryItem(entry: TimeEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${entry.startTime} - ${entry.endTime} (${entry.duration} mins)", modifier = Modifier.weight(1f))
        Checkbox(
            checked = entry.isSubmitted,
            onCheckedChange = { entry.isSubmitted = it }
        )
    }
}
