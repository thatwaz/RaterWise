package com.thatwaz.raterwise.ui.screens

// HomeScreen.kt



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import com.thatwaz.raterwise.ui.utils.getCurrentTimeFormatted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var isClockedIn by remember { mutableStateOf(false) }
    var isTaskStarted by remember { mutableStateOf(false) }
    var workTime by remember { mutableStateOf(0L) }
    var taskTime by remember { mutableStateOf(0L) }
    var maxTaskTime by remember { mutableStateOf("") }
    var taskTimerColor by remember { mutableStateOf(Color.Green) }
    var clockInTime by remember { mutableStateOf("") }
    var isMaxTimeInputEnabled by remember { mutableStateOf(true) } // State to control input field
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "RaterWise", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clock In/Out Button
            ClockInOutButton(
                isClockedIn = isClockedIn,
                onClockToggle = {
                    isClockedIn = !isClockedIn
                    if (isClockedIn) {
                        clockInTime = getCurrentTimeFormatted()
                        scope.launch {
                            while (isClockedIn) {
                                delay(60000L) // 1 minute delay
                                workTime++
                            }
                        }
                    } else {
                        workTime = 0L // Reset work time when clocking out
                    }
                }
            )

            // Display Clock In Time and Main Chronometer
            if (isClockedIn) {
                ChronometerDisplay(clockInTime = clockInTime, workTime = workTime)
            }

            // Disable further UI components until clocked in
            val contentModifier = if (isClockedIn) Modifier else Modifier.alpha(0.3f)

            // Max Task Time Input
            MaxTaskTimeInput(
                isClockedIn = isClockedIn,
                contentModifier = contentModifier,
                maxTaskTime = maxTaskTime,
                onMaxTaskTimeChange = { maxTaskTime = it },
                isEnabled = isMaxTimeInputEnabled // Control enabled state based on task status
            )

            // Task Timer Controls and Display
            TaskTimerControls(
                isClockedIn = isClockedIn,
                isTaskStarted = isTaskStarted,
                maxTaskTime = maxTaskTime,
                onTaskStart = {
                    isTaskStarted = true
                    isMaxTimeInputEnabled = false // Disable input when task starts
                    taskTime = 0L
                    taskTimerColor = Color.Green
                    scope.launch {
                        while (isTaskStarted) {
                            delay(60000L) // 1 minute delay
                            taskTime++
                            val maxTime = maxTaskTime.toLongOrNull() ?: 0L
                            taskTimerColor = when {
                                taskTime >= maxTime -> Color.Red
                                taskTime >= maxTime * 0.8 -> Color.Yellow
                                else -> Color.Green
                            }
                        }
                    }
                },
                onTaskFinish = {
                    isTaskStarted = false
                    isMaxTimeInputEnabled = true // Re-enable input when task finishes
                },
                contentModifier = contentModifier
            )
        }
    }
}
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun HomeScreen() {
//    var isClockedIn by remember { mutableStateOf(false) }
//    var isTaskStarted by remember { mutableStateOf(false) }
//    var workTime by remember { mutableStateOf(0L) } // Main chronometer time in minutes
//    var taskTime by remember { mutableStateOf(0L) } // Task chronometer time in minutes
//    var maxTaskTime by remember { mutableStateOf("") }
//    var taskTimerColor by remember { mutableStateOf(Color.Green) }
//    var clockInTime by remember { mutableStateOf("") }
//    val scope = rememberCoroutineScope()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(text = "RaterWise", style = MaterialTheme.typography.titleMedium) },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary
//                )
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .padding(paddingValues)
//                .fillMaxSize()
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Clock In/Out Button
//            ClockInOutButton(
//                isClockedIn = isClockedIn,
//                onClockToggle = {
//                    isClockedIn = !isClockedIn
//                    if (isClockedIn) {
//                        clockInTime = getCurrentTimeFormatted()
//                        scope.launch {
//                            while (isClockedIn) {
//                                delay(60000L) // 1 minute delay
//                                workTime++
//                            }
//                        }
//                    } else {
//                        workTime = 0L // Reset work time when clocking out
//                    }
//                }
//            )
//
//            // Display Clock In Time and Main Chronometer
//            if (isClockedIn) {
//                ChronometerDisplay(clockInTime = clockInTime, workTime = workTime)
//            }
//
//            // Disable further UI components until clocked in
//            val contentModifier = if (isClockedIn) Modifier else Modifier.alpha(0.3f)
//
//            // Max Task Time Input
//            MaxTaskTimeInput(
//                isClockedIn = isClockedIn,
//                contentModifier = contentModifier,
//                maxTaskTime = maxTaskTime,
//                onMaxTaskTimeChange = { maxTaskTime = it }
//            )
//
//            // Task Timer Controls and Display
//            TaskTimerControls(
//                isClockedIn = isClockedIn,
//                isTaskStarted = isTaskStarted,
//                maxTaskTime = maxTaskTime,
//                onTaskStart = {
//                    isTaskStarted = true
//                    taskTime = 0L
//                    taskTimerColor = Color.Green
//                    scope.launch {
//                        while (isTaskStarted) {
//                            delay(60000L) // 1 minute delay
//                            taskTime++
//                            val maxTime = maxTaskTime.toLongOrNull() ?: 0L
//                            taskTimerColor = when {
//                                taskTime >= maxTime -> Color.Red
//                                taskTime >= maxTime * 0.8 -> Color.Yellow
//                                else -> Color.Green
//                            }
//                        }
//                    }
//                },
//                onTaskFinish = {
//                    isTaskStarted = false
//                },
//                contentModifier = contentModifier
//            )
//        }
//    }
//}

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


//@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
//@Composable
//fun MaxTaskTimeInput(
//    isClockedIn: Boolean,
//    contentModifier: Modifier,
//    maxTaskTime: String,
//    onMaxTaskTimeChange: (String) -> Unit
//) {
//    val keyboardController = LocalSoftwareKeyboardController.current
//
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.Center,
//        modifier = Modifier
//            .padding(8.dp)
//            .then(contentModifier)
//    ) {
//        // Label for context
//        Text(
//            text = "Max Time (min):",
//            style = MaterialTheme.typography.bodyMedium,
//            modifier = Modifier.padding(end = 8.dp)
//        )
//
//        // Compact OutlinedTextField
//        OutlinedTextField(
//            value = maxTaskTime,
//            onValueChange = onMaxTaskTimeChange,
//            modifier = Modifier
//                .width(80.dp) // Set a smaller width to keep it compact
//                .height(56.dp), // Consistent height with material design text fields
//            enabled = isClockedIn,
//            keyboardOptions = KeyboardOptions(
//                keyboardType = KeyboardType.Number,
//                imeAction = ImeAction.Done
//            ),
//            keyboardActions = KeyboardActions(
//                onDone = {
//                    keyboardController?.hide() // Hide keyboard on 'Done'
//                }
//            ),
//            singleLine = true,
//            shape = MaterialTheme.shapes.small, // Rounded corners for a softer look
//            colors = TextFieldDefaults.outlinedTextFieldColors(
//                focusedBorderColor = MaterialTheme.colorScheme.primary,
//                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
//            )
//        )
//    }
//}



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
    val keyboardController = LocalSoftwareKeyboardController.current // Add keyboard controller

    // State variable to control the task start/stop
    var isTaskRunning by remember { mutableStateOf(false) }

    // Start task timer on LaunchedEffect when task is started
    LaunchedEffect(isTaskRunning) {
        if (isTaskRunning) {
            taskSeconds = 0L
            progress = 0f
            taskTimerColor = Color.Green
            scope.launch {
                while (isTaskRunning) {
                    delay(1000L) // Update every second
                    taskSeconds++
                    val maxTime = maxTaskTime.toLongOrNull() ?: 0L
                    progress = if (maxTime > 0) taskSeconds.toFloat() / (maxTime * 60) else 0f

                    // Update taskTimerColor based on progress
                    taskTimerColor = when {
                        progress >= 1f -> Color.Red
                        progress >= 0.8f -> Color.Yellow
                        else -> Color.Green
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(contentModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Circular Progress Indicator centered
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    CircularProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
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
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
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
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}





