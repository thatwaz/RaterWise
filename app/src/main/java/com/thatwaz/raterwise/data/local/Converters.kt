package com.thatwaz.raterwise.data.local

import androidx.room.TypeConverter
import java.util.Date


class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}


//class Converters {
//
//    private val gson = Gson()
//
//    // Converter for List<TaskTimeEntry>
//    @TypeConverter
//    fun fromTaskTimeEntryList(value: List<TaskTimeEntry>): String {
//        val type = object : TypeToken<List<TaskTimeEntry>>() {}.type
//        return gson.toJson(value, type)
//    }
//
//    @TypeConverter
//    fun toTaskTimeEntryList(value: String): List<TaskTimeEntry> {
//        val type = object : TypeToken<List<TaskTimeEntry>>() {}.type
//        return gson.fromJson(value, type)
//    }
//
//    // Converter for List<DailyWorkSummary>
//    @TypeConverter
//    fun fromDailyWorkSummaryList(value: List<DailyWorkSummary>): String {
//        val type = object : TypeToken<List<DailyWorkSummary>>() {}.type
//        return gson.toJson(value, type)
//    }
//
//    @TypeConverter
//    fun toDailyWorkSummaryList(value: String): List<DailyWorkSummary> {
//        val type = object : TypeToken<List<DailyWorkSummary>>() {}.type
//        return gson.fromJson(value, type)
//    }
//
//    // Converter for List<WorkPeriod>
//    @TypeConverter
//    fun fromWorkPeriodList(value: List<WorkPeriod>): String {
//        val type = object : TypeToken<List<WorkPeriod>>() {}.type
//        return gson.toJson(value, type)
//    }
//
//    @TypeConverter
//    fun toWorkPeriodList(value: String): List<WorkPeriod> {
//        val type = object : TypeToken<List<WorkPeriod>>() {}.type
//        return gson.fromJson(value, type)
//    }
//
//    // Optional: If you need to convert other fields or lists, add additional converters here
//}
//
//class Converters {
//
//    // Converter for List<TimeEntry>
//    @TypeConverter
//    fun fromTimeEntryList(value: List<TaskTimeEntry>): String {
//        val gson = Gson()
//        val type = object : TypeToken<List<TaskTimeEntry>>() {}.type
//        return gson.toJson(value, type)
//    }
//
//    @TypeConverter
//    fun toTimeEntryList(value: String): List<TaskTimeEntry> {
//        val gson = Gson()
//        val type = object : TypeToken<List<TaskTimeEntry>>() {}.type
//        return gson.fromJson(value, type)
//    }
//
//    // Converter for List<DailyWorkSummary>
//    @TypeConverter
//    fun fromDailyWorkSummaryList(value: List<DailyWorkSummary>): String {
//        val gson = Gson()
//        val type = object : TypeToken<List<DailyWorkSummary>>() {}.type
//        return gson.toJson(value, type)
//    }
//
//    @TypeConverter
//    fun toDailyWorkSummaryList(value: String): List<DailyWorkSummary> {
//        val gson = Gson()
//        val type = object : TypeToken<List<DailyWorkSummary>>() {}.type
//        return gson.fromJson(value, type)
//    }
//}
