package io.foxbird.edumate.data.local.converter

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray?): ByteArray? {
        if (value == null) return null
        val buffer = ByteBuffer.allocate(value.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(value)
        return buffer.array()
    }

    @TypeConverter
    fun toFloatArray(value: ByteArray?): FloatArray? {
        if (value == null) return null
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(value.size / 4)
        buffer.asFloatBuffer().get(floats)
        return floats
    }
}
