package com.kieronquinn.app.smartspacer.utils.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object GsonConverter {

    private val gson = Gson()
    private val setType = object: TypeToken<Set<String>>(){}.type

    @TypeConverter
    fun fromString(value: String): Set<String> {
        return gson.fromJson(value, setType)
    }

    @TypeConverter
    fun fromSet(set: Set<String>): String {
        return gson.toJson(set)
    }

}