package com.thatwaz.raterwise.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DetailsScreen(taskId: String) {
    Text(text = "Details for Task: $taskId", style = MaterialTheme.typography.bodyMedium)
}
