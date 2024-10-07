package com.thatwaz.raterwise.ui.screens

import android.util.Log
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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.ui.viewmodel.TimeCardViewModel




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTimeEntriesScreen(date: String, navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()
    val entries = timeEntriesByDay[date] ?: emptyList()

    // Separate the entries into unsubmitted and submitted categories
    val unsubmittedEntries = entries.filter { !it.isSubmitted }
    val submittedEntries = entries.filter { it.isSubmitted }

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
            // Add the temporary delete button here
            Button(
                onClick = { viewModel.deleteAllEntries() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Delete All Entries (Temp Button)")
            }

            LazyColumn {
                // Display Unsubmitted Entries First
                item {
                    Text(
                        text = "Unsubmitted Entries",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(unsubmittedEntries) { entry ->
                    TimeEntryItem(
                        entry = entry,
                        onCheckChanged = { updatedEntry ->
                            Log.d("TimeEntryToggle", "Toggling entry: $updatedEntry")
                            viewModel.toggleTimeEntrySubmission(updatedEntry) // Toggle submission state
                        }
                    )
                }

                // Display Submitted Entries Below
                item {
                    Text(
                        text = "Submitted Entries",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(submittedEntries) { entry ->
                    TimeEntryItem(
                        entry = entry,
                        onCheckChanged = { updatedEntry ->
                            Log.d("TimeEntryToggle", "Toggling entry: $updatedEntry")
                            viewModel.toggleTimeEntrySubmission(updatedEntry) // Toggle submission state
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun TimeEntryItem(entry: TimeEntry, onCheckChanged: ((TimeEntry) -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${entry.startTime} - ${entry.endTime} (${entry.duration} mins)", style = MaterialTheme.typography.bodyMedium)
        }

        // Use a local state to track the checkbox state visually
        val checkedState = remember { mutableStateOf(entry.isSubmitted) }

        // Sync the state with the latest value from the entry whenever it changes
        LaunchedEffect(entry.isSubmitted) {
            checkedState.value = entry.isSubmitted
        }

        Checkbox(
            checked = checkedState.value,
            onCheckedChange = { isChecked ->
                checkedState.value = isChecked // Update the local state for immediate UI feedback
                onCheckChanged?.invoke(entry.copy(isSubmitted = isChecked)) // Invoke callback to update state in ViewModel
            },
            enabled = onCheckChanged != null
        )
    }
}




//@Composable
//fun TimeEntryItem(entry: TimeEntry, onCheckChanged: ((TimeEntry) -> Unit)?) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Column(modifier = Modifier.weight(1f)) {
//            Text(text = "${entry.startTime} - ${entry.endTime} (${entry.duration} mins)", style = MaterialTheme.typography.bodyMedium)
//        }
//
//        // Use a local state to track changes before they are confirmed
//        val checkedState = remember { mutableStateOf(entry.isSubmitted) }
//
//        Checkbox(
//            checked = checkedState.value,
//            onCheckedChange = {
//                checkedState.value = it // Update local state first
//                onCheckChanged?.invoke(entry.copy(isSubmitted = it)) // Trigger the change
//            },
//            enabled = onCheckChanged != null // Only enable if interaction is allowed
//        )
//    }
//}





