package com.thatwaz.raterwise.ui.screens

// HomeScreen.kt



import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.thatwaz.raterwise.data.model.SessionWithTasks
import com.thatwaz.raterwise.ui.utils.TimerService
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
    val timeEntriesByDay by viewModel.timeEntriesByDay.collectAsState()
    val clockInTime by viewModel.clockInTime.collectAsState(initial = "Not Clocked In")

    // Log statements to track the state values
    Log.d("HomeScreen", "Observed isClockedIn: $isClockedIn")
    Log.d("HomeScreen", "Observed clockInTime: $clockInTime")

    // Restore state when the Composable is launched
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "LaunchedEffect triggered, calling restoreSessionState")
        viewModel.restoreSessionState() // Ensure session state restoration
    }

    // Local variable for TimerService reference
    var timerService: TimerService? by remember { mutableStateOf(null) }

    // Handle service binding and unbinding safely
    DisposableEffect(Unit) {
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val localBinder = binder as? TimerService.TimerBinder
                timerService = localBinder?.getService()

                Log.d("HomeScreen", "Service connected, isClockedIn: $isClockedIn, clockInTime: $clockInTime")

                // Start the timer if user is clocked in
                if (isClockedIn) {
                    timerService?.startTimer(clockInTime)
                }

                // Start the task timer if a task is running
                if (viewModel.isTaskRunning) {
                    timerService?.startTaskTimer(viewModel.taskSeconds)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d("HomeScreen", "Service disconnected")
                timerService = null
            }
        }

        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        onDispose {
            Log.d("HomeScreen", "Service unbinding")
            context.unbindService(serviceConnection)
        }
    }

    val today = viewModel.getCurrentDateFormatted()
    val taskList = timeEntriesByDay[today] ?: emptyList()

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
            // Pass clockInTime to TimeClockControlsCard
            Log.d("HomeScreen", "Passing clockInTime to TimeClockControlsCard: $clockInTime")
            TimeClockControlsCard(viewModel, isClockedIn, clockInTime)
            TaskTimerControlsCard(viewModel, isClockedIn, context)

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                CompletedTasksList(taskList)
            }

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                        viewModel.updateMaxTaskTime(minute.toString()) // Update max task time
                        onSelect(minute)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                        viewModel.updateMaxTaskTime(minute.toString()) // Update max task time
                        onSelect(minute)
                    }
                )
            }
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


//@RequiresApi(Build.VERSION_CODES.O)
//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//fun TaskTimerControls(
//    viewModel: TimeCardViewModel,
//    isClockedIn: Boolean,
//    maxTaskTime: String,
//    onTaskFinish: () -> Unit,
//    contentModifier: Modifier
//) {
//    val context = LocalContext.current
//    var progress by remember { mutableStateOf(0f) }
//    var taskTimerColor by remember { mutableStateOf(Color.Green) }
//    var countdownSeconds by remember { mutableStateOf(0L) }
//    var showCountdown by remember { mutableStateOf(false) }
//
//    val taskSeconds = viewModel.taskSeconds
//    val isTaskRunning = viewModel.isTaskRunning
//
//    // Log taskSeconds on change to ensure state is correctly observed
//    LaunchedEffect(taskSeconds) {
//        Log.d("TaskTimerControls", "Observed taskSeconds change: $taskSeconds seconds")
//    }
//
//    // Start or resume the task timer when task is running
//    LaunchedEffect(isTaskRunning) {
//        if (isTaskRunning) {
//            Log.d("TaskTimerControls", "Task timer started or resumed, initial taskSeconds: $taskSeconds")
//            while (isTaskRunning) {
//                delay(1000L)
//                viewModel.updateTaskSeconds(viewModel.taskSeconds + 1)
//
//                val maxTime = maxTaskTime.toLongOrNull() ?: 0L
//                progress = if (maxTime > 0) taskSeconds.toFloat() / (maxTime * 60) else 0f
//
//                Log.d("TaskTimerControls", "Timer tick - taskSeconds: ${viewModel.taskSeconds}, progress: $progress")
//
//                // Update taskTimerColor and countdown logic
//                when {
//                    progress >= 1f -> {
//                        taskTimerColor = Color.Red
//                        showCountdown = false
//                        Log.d("TaskTimerControls", "Progress maxed out, timer color set to red")
//                    }
//                    progress >= 0.8f -> {
//                        taskTimerColor = Color.Yellow
//                        showCountdown = true
//                        countdownSeconds = (maxTime * 60) - taskSeconds
//                        Log.d("TaskTimerControls", "Countdown warning: ${countdownSeconds}s left")
//                    }
//                    else -> {
//                        taskTimerColor = Color.Green
//                        showCountdown = false
//                        Log.d("TaskTimerControls", "Timer color set to green, no countdown")
//                    }
//                }
//            }
//        } else {
//            Log.d("TaskTimerControls", "Task timer not running, stopped.")
//        }
//    }
//
//    // UI for displaying and controlling task timer
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .then(contentModifier)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.Center,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(8.dp)
//            ) {
//                Box(
//                    contentAlignment = Alignment.Center,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    CircularProgressIndicator(
//                        progress = progress.coerceIn(0f, 1f),
//                        color = taskTimerColor,
//                        strokeWidth = 8.dp,
//                        modifier = Modifier.size(100.dp)
//                    )
//                    Text(
//                        text = String.format("%02d:%02d", taskSeconds / 60, taskSeconds % 60),
//                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
//                    )
//                }
//
//                Text(
//                    text = String.format("%02d:%02d", taskSeconds / 60, taskSeconds % 60),
//                    style = MaterialTheme.typography.bodyMedium.copy(
//                        color = Color.Black,
//                        fontWeight = FontWeight.Bold
//                    ),
//                    modifier = Modifier
//                        .padding(start = 16.dp)
//                        .weight(1f)
//                )
//            }
//
//            if (showCountdown) {
//                Text(
//                    text = "Warning: ${countdownSeconds}s left",
//                    style = MaterialTheme.typography.bodyLarge.copy(
//                        color = Color.Red,
//                        fontWeight = FontWeight.Bold
//                    ),
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//            }
//
//            Button(
//                onClick = {
//                    Log.d("TaskTimerControls", "Finish Task button clicked")
//                    viewModel.completeTask(context)
//                    onTaskFinish()
//                },
//                modifier = Modifier.fillMaxWidth(),
//                enabled = isClockedIn && isTaskRunning
//            ) {
//                Text(text = "Finish Task")
//            }
//        }
//    }
//}




//@RequiresApi(Build.VERSION_CODES.O)
//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//fun TaskTimerControls(
//    viewModel: TimeCardViewModel,
//    isClockedIn: Boolean,
//    maxTaskTime: String,
//    onTaskStart: () -> Unit,
//    onTaskFinish: () -> Unit,
//    contentModifier: Modifier
//) {
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//    var progress by remember { mutableStateOf(0f) }
//    var taskTimerColor by remember { mutableStateOf(Color.Green) }
//    val snackbarHostState = remember { SnackbarHostState() }
//    val keyboardController = LocalSoftwareKeyboardController.current
//    var countdownSeconds by remember { mutableStateOf(0L) }
//    var showCountdown by remember { mutableStateOf(false) }
//
//    val taskSeconds = viewModel.taskSeconds
//    val isTaskRunning = viewModel.isTaskRunning
//
//    // Ensure timer and progress are maintained across recompositions
//    LaunchedEffect(isTaskRunning) {
//        if (isTaskRunning) {
//            scope.launch {
//                while (viewModel.isTaskRunning) {
//                    delay(1000L)
//                    viewModel.updateTaskSeconds(viewModel.taskSeconds + 1)
//                    val maxTime = maxTaskTime.toLongOrNull() ?: 0L
//                    progress = if (maxTime > 0) taskSeconds.toFloat() / (maxTime * 60) else 0f
//
//                    // Update taskTimerColor and countdown logic
//                    when {
//                        progress >= 1f -> {
//                            taskTimerColor = Color.Red
//                            showCountdown = false
//                        }
//
//                        progress >= 0.8f -> {
//                            taskTimerColor = Color.Yellow
//                            showCountdown = true
//                            countdownSeconds = (maxTime * 60) - taskSeconds
//                        }
//
//                        else -> {
//                            taskTimerColor = Color.Green
//                            showCountdown = false
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .then(contentModifier)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.Center,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(8.dp)
//            ) {
//                Box(
//                    contentAlignment = Alignment.Center,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    CircularProgressIndicator(
//                        progress = progress.coerceIn(0f, 1f),
//                        color = taskTimerColor,
//                        strokeWidth = 8.dp,
//                        modifier = Modifier.size(100.dp)
//                    )
//                    Text(
//                        text = String.format("%02d:%02d", taskSeconds / 60, taskSeconds % 60),
//                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
//                    )
//                }
//
//                Text(
//                    text = String.format("%02d:%02d", taskSeconds / 60, taskSeconds % 60),
//                    style = MaterialTheme.typography.bodyMedium.copy(
//                        color = Color.Black,
//                        fontWeight = FontWeight.Bold
//                    ),
//                    modifier = Modifier
//                        .padding(start = 16.dp)
//                        .weight(1f)
//                )
//            }
//
//            if (showCountdown) {
//                Text(
//                    text = "Warning: ${countdownSeconds}s left",
//                    style = MaterialTheme.typography.bodyLarge.copy(
//                        color = Color.Red,
//                        fontWeight = FontWeight.Bold
//                    ),
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//            }
//
//            Button(
//                onClick = {
//                    // Stop the task when the button is clicked
//                    viewModel.stopTask(context)
//
//                    // Invoke the onTaskFinish callback directly after stopping the task
//                    onTaskFinish()
//                },
//                modifier = Modifier.fillMaxWidth(),
//                enabled = isClockedIn && isTaskRunning
//            ) {
//                Text(text = "Finish Task")
//            }
//        }





        // TEMP CODE for display
//        @Composable
//        fun CompletedTaskItem(task: TaskTimeEntry) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(8.dp),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(text = "Start: ${task.startTime}", style = MaterialTheme.typography.bodyMedium)
//                Text(text = "End: ${task.endTime}", style = MaterialTheme.typography.bodyMedium)
//                Text(
//                    text = "Duration: ${task.duration} min",
//                    style = MaterialTheme.typography.bodyMedium
//                )
//                Text(
//                    text = "Expected: ${task.expectedDuration} min",
//                    style = MaterialTheme.typography.bodyMedium
//                )
//                Text(
//                    text = "Status: ${if (task.isOverUnderAET) "Over/Under AET" else "Within AET"}",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = if (task.isOverUnderAET) Color.Red else Color.Green // Color coding for AET status
//                )
//            }
//        }














