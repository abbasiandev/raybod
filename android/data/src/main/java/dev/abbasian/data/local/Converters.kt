package dev.abbasian.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.abbasian.domain.model.DrebinFeatures

class Converters {

    companion object {
        private val gson = Gson()
        private val stringListType = TypeToken.getParameterized(List::class.java, String::class.java).type
        private val stringFloatMapType =
            TypeToken.getParameterized(Map::class.java, String::class.java, Float::class.java).type
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return gson.fromJson(value, stringListType) ?: emptyList()
    }

    @TypeConverter
    fun fromStringFloatMap(value: Map<String, Float>): String = gson.toJson(value)

    @TypeConverter
    fun toStringFloatMap(value: String): Map<String, Float> {
        if (value.isBlank()) return emptyMap()
        return gson.fromJson(value, stringFloatMapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromDrebinFeatures(value: DrebinFeatures): String = gson.toJson(value)

    @TypeConverter
    fun toDrebinFeatures(value: String): DrebinFeatures {
        if (value.isBlank()) return DrebinFeatures()
        return gson.fromJson(value, DrebinFeatures::class.java) ?: DrebinFeatures()
    }
}
