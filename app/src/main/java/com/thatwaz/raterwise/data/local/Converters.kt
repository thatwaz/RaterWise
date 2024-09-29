package com.thatwaz.raterwise.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.TimeEntry

class Converters {

    // Converter for List<TimeEntry>
    @TypeConverter
    fun fromTimeEntryList(value: List<TimeEntry>): String {
        val gson = Gson()
        val type = object : TypeToken<List<TimeEntry>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toTimeEntryList(value: String): List<TimeEntry> {
        val gson = Gson()
        val type = object : TypeToken<List<TimeEntry>>() {}.type
        return gson.fromJson(value, type)
    }

    // Converter for List<DailyWorkSummary>
    @TypeConverter
    fun fromDailyWorkSummaryList(value: List<DailyWorkSummary>): String {
        val gson = Gson()
        val type = object : TypeToken<List<DailyWorkSummary>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toDailyWorkSummaryList(value: String): List<DailyWorkSummary> {
        val gson = Gson()
        val type = object : TypeToken<List<DailyWorkSummary>>() {}.type
        return gson.fromJson(value, type)
    }
}
