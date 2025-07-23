package com.wang.phoneaiassistant.data.database

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun stringToFloatArray(value: String?): FloatArray? {
        return value?.split(",")?.map { it.toFloat() }?.toFloatArray()
    }
}