package dev.abbasian.data.local

import dev.abbasian.domain.model.DrebinFeatures
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `string list round trip`() {
        val original = listOf("android.permission.INTERNET", "android.permission.CAMERA")
        val json = converters.fromStringList(original)
        assertEquals(original, converters.toStringList(json))
    }

    @Test
    fun `string float map round trip`() {
        val original = mapOf("score" to 0.75f, "confidence" to 0.5f)
        val json = converters.fromStringFloatMap(original)
        val restored = converters.toStringFloatMap(json)
        assertEquals(original.size, restored.size)
        assertEquals(original["score"], restored["score"])
        assertEquals(original["confidence"], restored["confidence"])
    }

    @Test
    fun `drebin features round trip`() {
        val original = DrebinFeatures(s1Hardware = listOf("feature-a"))
        val json = converters.fromDrebinFeatures(original)
        val restored = converters.toDrebinFeatures(json)
        assertEquals(original.s1Hardware, restored.s1Hardware)
    }

    @Test
    fun `blank values return empty defaults`() {
        assertEquals(emptyList<String>(), converters.toStringList(""))
        assertEquals(emptyMap<String, Float>(), converters.toStringFloatMap(""))
        assertEquals(DrebinFeatures(), converters.toDrebinFeatures(""))
    }
}
