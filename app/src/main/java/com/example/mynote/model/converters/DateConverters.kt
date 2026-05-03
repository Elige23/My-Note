package com.example.mynote.model.converters

import androidx.room.TypeConverter
import java.util.Date

class DateConverters {

    // Converts Long (DB) → Date (Model)
    @TypeConverter
    fun longToDate(value: Long): Date{
        return Date(value)
    }

    @TypeConverter
    fun dateToLong(date: Date): Long{
        return date.time
    }
}