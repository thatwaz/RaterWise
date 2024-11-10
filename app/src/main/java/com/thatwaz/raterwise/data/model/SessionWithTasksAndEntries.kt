package com.thatwaz.raterwise.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithTasksAndEntries(
    @Embedded val session: SessionWithTasks,

    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val taskEntries: List<TaskEntry> // List of related TaskEntry objects
)

