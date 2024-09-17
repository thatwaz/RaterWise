package com.thatwaz.raterwise.tempdata

data class Task(
    val taskNumber: Int,
    val maxTime: Long,
    val actualTime: Long,
    val difference: Long // Can be negative if under the max time
)

