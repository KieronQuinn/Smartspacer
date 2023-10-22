package com.kieronquinn.app.smartspacer.utils.gson

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalTime

class LocalTimeAdapter: JsonDeserializer<LocalTime>, JsonSerializer<LocalTime> {

    companion object {
        private const val KEY_NANOS = "nanos"
    }

    override fun serialize(
        src: LocalTime,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonObject().apply {
            addProperty(KEY_NANOS, src.toNanoOfDay())
        }
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalTime {
        return LocalTime.ofNanoOfDay(json.asJsonObject.get(KEY_NANOS).asLong)
    }
}