package com.thatwaz.raterwise.ui.screens

// HomeScreen.kt



import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.thatwaz.raterwise.data.model.SessionWithTasks
import com.thatwaz.raterwise.ui.viewmodel.TimeCardViewModel
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: TimeCardViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Observing states from ViewModel
    val isClockedIn by viewModel.isClockedIn.collectAsState()
    val taskSeconds by rememberUpdatedState(viewModel.taskSeconds) // Observe taskSeconds
    val isTaskRunning by rememberUpdatedState(viewModel.isTaskRunning) // Observe task running status
    val clockInTime by viewModel.clockInTime.collectAsState(initial = "Not Clocked In")

    // Debugging log statements
    Log.d("HomeScreen", "Observed isClockedIn: $isClockedIn")
    Log.d("HomeScreen", "Observed isTaskRunning: $isTaskRunning")

    // Restore state and recalculate taskSeconds on app resume
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "LaunchedEffect triggered, calling restoreSessionState")
        viewModel.restoreSessionState()
    }

    // Lifecycle-aware logic to handle app resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("HomeScreen", "ON_RESUME triggered, updating task seconds.")
                viewModel.updateTaskSecondsOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val today = viewModel.getCurrentDateFormatted()
    val taskEntriesByDay = viewModel.timeEntriesByDay.collectAsState().value
    val taskList = taskEntriesByDay[today] ?: emptyList()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "RaterWise") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time Clock Controls
            TimeClockControlsCard(viewModel, isClockedIn, clockInTime)

            // Task Timer Controls
            TaskTimerControlsCard(viewModel, isClockedIn, context)

            // Completed Tasks List
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                CompletedTasksList(taskList)
            }

            // Navigate to Time Sheet
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
fun TaskTimerControlsCard(viewModel: TimeCardViewModel, isClockedIn: Boolean, context: Context) {
    var maxTaskTime by remember { mutableStateOf("") }

    // State to track the selected task time button
    var selectedMinute by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickTaskButtons(
                onSelect = { selectedMinutes ->
                    maxTaskTime = selectedMinutes.toString()
                    viewModel.updateMaxTaskTime(maxTaskTime) // Call viewModel method
                    viewModel.startTask(context, selectedMinutes) // Pass duration as selectedMinutes
                },
                selectedMinute = selectedMinute,
                setSelectedMinute = { selectedMinute = it },
                viewModel = viewModel // Pass the viewModel
            )


            // Task Timer Controls
            TaskTimerControls(
                viewModel = viewModel,
                isClockedIn = isClockedIn,
                maxTaskTime = maxTaskTime,
//                onTaskStart = { /* No direct call to startTask here */ },
                onTaskFinish = {
                    Log.d("Composable", "onTaskFinish invoked.")
                    viewModel.completeTask(context)
//                    viewModel.stopForegroundService(context)
                },
                contentModifier = Modifier.padding(16.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun QuickTaskButtons(
    onSelect: (Int) -> Unit,
    selectedMinute: Int?,
    setSelectedMinute: (Int?) -> Unit,
    viewModel: TimeCardViewModel
) {
    // State to toggle the visibility of additional buttons
    var showMoreButtons by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First Row: 1-6 minutes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (1..6).forEach { minute ->
                TaskButton(
                    minute = minute,
                    isSelected = selectedMinute == minute,
                    onSelect = {
                        setSelectedMinute(minute)
                        viewModel.updateMaxTaskTime(minute.toString())
                        onSelect(minute)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Second Row: 7-12 minutes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (7..12).forEach { minute ->
                TaskButton(
                    minute = minute,
                    isSelected = selectedMinute == minute,
                    onSelect = {
                        setSelectedMinute(minute)
                        viewModel.updateMaxTaskTime(minute.toString())
                        onSelect(minute)
                    }
                )
            }
        }

        // Show additional buttons (13-18 minutes) if toggled
        if (showMoreButtons) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (13..18).forEach { minute ->
                    TaskButton(
                        minute = minute,
                        isSelected = selectedMinute == minute,
                        onSelect = {
                            setSelectedMinute(minute)
                            viewModel.updateMaxTaskTime(minute.toString())
                            onSelect(minute)
                        }
                    )
                }
            }

            // "Less" button to hide additional buttons
            Text(
                text = "Less",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { showMoreButtons = false }
                    .padding(vertical = 8.dp)
            )
        } else {
            // "More" button to show additional buttons
            Text(
                text = "More",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { showMoreButtons = true }
                    .padding(vertical = 8.dp)
            )
        }
    }
}




@Composable
fun TaskButton(minute: Int, isSelected: Boolean, onSelect: () -> Unit) {
    Button(
        onClick = onSelect,
        modifier = Modifier.size(56.dp),
        contentPadding = PaddingValues(0.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "$minute",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}





@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeClockControlsCard(
    viewModel: TimeCardViewModel,
    isClockedIn: Boolean,
    clockInTime: String // Accept clockInTime as a parameter
) {
    Log.d("TimeClockControlsCard", "Rendering TimeClockControlsCard - isClockedIn: $isClockedIn, clockInTime: $clockInTime")

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
            // Clock In/Out Button
            Button(
                onClick = {
                    if (isClockedIn) {
                        Log.d("TimeClockControlsCard", "Clock Out button clicked")
                        viewModel.endWorkSession()
                    } else {
                        Log.d("TimeClockControlsCard", "Clock In button clicked")
                        viewModel.startWorkSession()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isClockedIn) "Clock Out" else "Clock In")
            }

            // Display Clocked In Time only when the user is clocked in
            if (isClockedIn) {
                Log.d("TimeClockControlsCard", "Displaying clockInTime: $clockInTime")
                Text(
                    text = "Clocked in at: $clockInTime",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}





@Composable
fun CompletedTasksList(taskList: List<SessionWithTasks>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp) // Constrain height to avoid infinite constraints
            .padding(16.dp) // Padding inside the card
    ) {
        items(taskList) { task ->
//            CompletedTaskItem(task)
        }
    }
}






@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TaskTimerControls(
    viewModel: TimeCardViewModel,
    isClockedIn: Boolean,
    maxTaskTime: String,
    onTaskFinish: () -> Unit,
    contentModifier: Modifier
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var taskTimerColor by remember { mutableStateOf(Color.Green) }
    var countdownSeconds by remember { mutableStateOf(0L) }
    var showCountdown by remember { mutableStateOf(false) }

    val taskSeconds = viewModel.taskSeconds
//    val taskSeconds by viewModel.taskSeconds // Observing the ViewModel taskSeconds
    val isTaskRunning by rememberUpdatedState(viewModel.isTaskRunning)

    // Observe taskSeconds changes to verify they’re accurately updated
    LaunchedEffect(taskSeconds) {
        Log.d("TaskTimerControls", "Observed taskSeconds change: $taskSeconds seconds")
    }

    // Initialize the task timer, ensuring it resumes accurately
    LaunchedEffect(isTaskRunning) {
        if (isTaskRunning) {
            Log.d("TaskTimerControls", "Task timer started or resumed, initial taskSeconds: $taskSeconds")
            while (isTaskRunning) {
                delay(1000L)

                // Update taskSeconds in the ViewModel
                viewModel.updateTaskSeconds(viewModel.taskSeconds + 1)

                // Calculate progress
                val maxTime = maxTaskTime.toLongOrNull() ?: 0L
                progress = if (maxTime > 0) taskSeconds.toFloat() / (maxTime * 60) else 0f

                Log.d("TaskTimerControls", "Timer tick - taskSeconds: ${viewModel.taskSeconds}, progress: $progress")

                // Update timer color and countdown logic
                when {
                    progress >= 1f -> {
                        taskTimerColor = Color.Red
                        showCountdown = false
                        Log.d("TaskTimerControls", "Progress maxed out, timer color set to red")
                    }
                    progress >= 0.8f -> {
                        taskTimerColor = Color.Yellow
                        showCountdown = true
                        countdownSeconds = (maxTime * 60) - taskSeconds
                        Log.d("TaskTimerControls", "Countdown warning: ${countdownSeconds}s left")
                    }
                    else -> {
                        taskTimerColor = Color.Green
                        showCountdown = false
                        Log.d("TaskTimerControls", "Timer color set to green, no countdown")
                    }
                }
            }
        } else {
            Log.d("TaskTimerControls", "Task timer not running, stopped.")
        }
    }

    // UI for displaying and controlling task timer
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
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
                    Text(
                        text = String.format("%02d:%02d", taskSeconds / 60, taskSeconds % 60),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                }

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

            Button(
                onClick = {
                    Log.d("TaskTimerControls", "Finish Task button clicked")
                    viewModel.completeTask(context)
                    onTaskFinish()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isClockedIn && isTaskRunning
            ) {
                Text(text = "Finish Task")
            }
        }
    }
}

















