package com.thatwaz.raterwise.ui.screens

// HomeScreen.kt



import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.thatwaz.raterwise.data.model.TimeEntry

import com.thatwaz.raterwise.ui.viewmodel.TimeCardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: TimeCardViewModel = hiltViewModel()) {
    val isClockedIn by viewModel.isClockedIn.collectAsState()
    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()

    // Get the list of tasks for the current date
    val today = viewModel.getCurrentDateFormatted()
    val taskList = timeEntriesByDay[today] ?: emptyList() // Get tasks for today only

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "RaterWise", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Card: Clock In/Out, Work Time, Max Task Time Input
            TaskControlsCard(viewModel = viewModel, isClockedIn = isClockedIn)

            // Middle Card: Start Task, Chronometer, Finish Task
            TaskTimerControlsCard(viewModel = viewModel, isClockedIn = isClockedIn)

            // Bottom Card: Completed Tasks List for Today
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                CompletedTasksList(taskList = taskList)
            }

            // Temporary Button for Navigation
            Button(
                onClick = { navController.navigate("timesheet") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Go to Time Sheet")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskControlsCard(viewModel: TimeCardViewModel, isClockedIn: Boolean) {
    val scope = rememberCoroutineScope()
    var clockInTime by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClockInOutButton(
                isClockedIn = isClockedIn,
                onClockToggle = {
                    if (!isClockedIn) {
                        clockInTime = viewModel.getCurrentTimeFormatted()
                        viewModel.startWorkSession()
                    } else {
                        viewModel.endWorkSession()
                    }
                }
            )

            if (isClockedIn) {
                ChronometerDisplay(clockInTime = clockInTime, workTime = viewModel.totalWorkTime)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskTimerControlsCard(viewModel: TimeCardViewModel, isClockedIn: Boolean) {
    val taskTime by remember { mutableStateOf(0L) }
    var maxTaskTime by remember { mutableStateOf("") }
    val isMaxTimeInputEnabled = true

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaxTaskTimeInput(
                isClockedIn = isClockedIn,
                contentModifier = if (isClockedIn) Modifier else Modifier.alpha(0.3f),
                maxTaskTime = maxTaskTime,
                onMaxTaskTimeChange = { maxTaskTime = it },
                isEnabled = isMaxTimeInputEnabled
            )

            TaskTimerControls(
                isClockedIn = isClockedIn,
                isTaskStarted = viewModel.isTaskStarted,
                maxTaskTime = maxTaskTime,
                onTaskStart = { viewModel.startTask() }, // Start task only, not session
                onTaskFinish = { viewModel.completeTask() }, // Complete task only
                contentModifier = Modifier.padding(16.dp)
            )
        }
    }
}


@Composable
fun CompletedTasksList(taskList: List<TimeEntry>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp) // Constrain height to avoid infinite constraints
            .padding(16.dp) // Padding inside the card
    ) {
        items(taskList) { task ->
            CompletedTaskItem(task)
        }
    }
}




@Composable
fun ChronometerDisplay(clockInTime: String, workTime: Long) {
    Column {
        Text(
            text = "Clocked in at: $clockInTime",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Work Time: $workTime min",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MaxTaskTimeInput(
    isClockedIn: Boolean,
    contentModifier: Modifier,
    maxTaskTime: String,
    onMaxTaskTimeChange: (String) -> Unit,
    isEnabled: Boolean // New parameter to control enabled state
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(8.dp)
            .then(contentModifier)
    ) {
        // Label for context
        Text(
            text = "Max Time (min):",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )

        // Compact OutlinedTextField
        OutlinedTextField(
            value = maxTaskTime,
            onValueChange = onMaxTaskTimeChange,
            modifier = Modifier
                .width(80.dp) // Set a smaller width to keep it compact
                .height(56.dp), // Consistent height with material design text fields
            enabled = isClockedIn && isEnabled, // Disable based on task status
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Hide keyboard on 'Done'
                }
            ),
            singleLine = true,
            shape = MaterialTheme.shapes.small, // Rounded corners for a softer look
            colors = OutlinedTextFieldDefaults.colors()
        )
    }
}




@Composable
fun ClockInOutButton(isClockedIn: Boolean, onClockToggle: () -> Unit) {
    Button(onClick = onClockToggle, modifier = Modifier.fillMaxWidth()) {
        Text(if (isClockedIn) "Clock Out" else "Clock In")
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TaskTimerControls(
    isClockedIn: Boolean,
    isTaskStarted: Boolean,
    maxTaskTime: String,
    onTaskStart: () -> Unit,
    onTaskFinish: () -> Unit,
    contentModifier: Modifier
) {
    val scope = rememberCoroutineScope()
    var taskSeconds by remember { mutableStateOf(0L) }
    var progress by remember { mutableStateOf(0f) }
    var taskTimerColor by remember { mutableStateOf(Color.Green) }
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var countdownSeconds by remember { mutableStateOf(0L) } // State for countdown
    var showCountdown by remember { mutableStateOf(false) } // Control visibility of countdown

    // State variable to control the task start/stop
    var isTaskRunning by remember { mutableStateOf(false) }

    // Start task timer on LaunchedEffect when task is started
    LaunchedEffect(isTaskRunning) {
        if (isTaskRunning) {
            taskSeconds = 0L
            progress = 0f
            taskTimerColor = Color.Green
            showCountdown = false // Reset countdown visibility
            scope.launch {
                while (isTaskRunning) {
                    delay(1000L) // Update every second
                    taskSeconds++
                    val maxTime = maxTaskTime.toLongOrNull() ?: 0L
                    progress = if (maxTime > 0) taskSeconds.toFloat() / (maxTime * 60) else 0f

                    // Update taskTimerColor and manage countdown
                    when {
                        progress >= 1f -> {
                            taskTimerColor = Color.Red
                            showCountdown = false // Hide countdown if time exceeded
                        }
                        progress >= 0.8f -> {
                            taskTimerColor = Color.Yellow
                            showCountdown = true
                            countdownSeconds = (maxTime * 60) - taskSeconds // Calculate countdown
                        }
                        else -> {
                            taskTimerColor = Color.Green
                            showCountdown = false
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // Use the full available size
            .then(contentModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), // Adds spacing between elements
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Task Start Button
            Button(
                onClick = {
                    keyboardController?.hide() // Hide the keyboard when start task is pressed
                    isTaskRunning = true // Start the task
                    onTaskStart() // Notify parent that task has started
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isClockedIn && maxTaskTime.isNotBlank() && !isTaskRunning
            ) {
                Text(text = "Start Task")
            }

            // Row to contain the centered circular progress indicator and the task chronometer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center, // Center the contents horizontally
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Circular Progress Indicator centered
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f) // Center the circular indicator by using weight
                ) {
                    CircularProgressIndicator(
                        progress = progress.coerceIn(0f, 1f), // Ensure progress is within 0f and 1f
                        color = taskTimerColor,
                        strokeWidth = 8.dp,
                        modifier = Modifier.size(100.dp)
                    )

                    // Display task time inside the circular progress indicator
                    Text(
                        text = String.format("%02d:%02d", taskSeconds / 60, taskSeconds % 60),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                }

                // Task Chronometer Display to the right of the circular progress
                Text(
                    text = String.format("%02d:%02d", taskSeconds / 60, taskSeconds % 60),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black, // Display the chronometer in black
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .padding(start = 16.dp) // Add space between the indicator and the text
                        .weight(1f) // Allow the text to use the remaining space
                )
            }

            // Countdown Timer Display when progress is yellow
            if (showCountdown) {
                Text(
                    text = "Warning: ${countdownSeconds}s left",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Finish Task Button
            Button(
                onClick = {
                    isTaskRunning = false // Stop the task
                    scope.launch {
                        // Show snackbar when task is finished
                        val result = snackbarHostState.showSnackbar(
                            message = "Task time recorded: ${String.format("%02d:%02d", taskSeconds / 60, taskSeconds % 60)}",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            // Undo action: reset task time and keep the task running
                            isTaskRunning = true
                            taskSeconds = 0L
                            progress = 0f
                            taskTimerColor = Color.Green
                        } else {
                            // Complete the task normally
                            onTaskFinish()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isClockedIn && isTaskRunning
            ) {
                Text(text = "Finish Task")
            }
        }

        // Correct positioning of SnackbarHost at the bottom of the Box
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Use Alignment.BottomCenter within Box
                .padding(bottom = 16.dp) // Add padding to avoid overlap with screen edges
        )
    }
}



@Composable
fun CompletedTaskItem(task: TimeEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Start: ${task.startTime}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "End: ${task.endTime}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Duration: ${task.duration} min", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Expected: ${task.expectedDuration} min",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Status: ${if (task.isOverUnderAET) "Over/Under AET" else "Within AET"}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.isOverUnderAET) Color.Red else Color.Green // Color coding for AET status
        )
    }
}












