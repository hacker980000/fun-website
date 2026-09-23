package com.socialaiassistant.keyboard.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ModelResultParserTest {
    private val parser = ModelResultParser()

    @Test
    fun parses_plain_json() {
        val parsed = parser.parse("""{"reply":"Sounds good!","category":"general","confidence":0.88}""")
        assertEquals("Sounds good!", parsed?.reply)
        assertEquals("general", parsed?.category)
        assertEquals(0.88, parsed?.confidence ?: 0.0, 0.001)
    }

    @Test
    fun parses_fenced_json() {
        val parsed = parser.parse("""```json
{"reply":"আমি ভালো আছি 😊","confidence":0.7}
```""")
        assertEquals("আমি ভালো আছি 😊", parsed?.reply)
    }

    @Test
    fun parses_uppercase_label_and_fenced_json() {
        val raw = """JSON
```JSON
{
  "reply": "হ্যাঁ, বলো কি এনে দিতে হবে।",
  "category": "general",
  "confidence": 1.0
}
```
"""
        assertEquals("হ্যাঁ, বলো কি এনে দিতে হবে।", parser.parse(raw)?.reply)
    }

    @Test
    fun malformed_json_looking_payload_is_never_plain_text_fallback() {
        assertNull(parser.parse("""JSON
```json
{"reply":"hello", bad}
```"""))
        assertNull(parser.parse("""{"reply":"hello", bad}"""))
    }

    @Test
    fun plain_natural_language_still_works() {
        assertEquals("Sure, I can do that.", parser.parse("Reply: Sure, I can do that.")?.reply)
    }

    @Test
    fun malformed_json_falls_back_to_clean_plain_text() {
        val parsed = parser.parse("Reply: Sure, tonight works for me!")
        assertEquals("Sure, tonight works for me!", parsed?.reply)
        assertNotNull(parsed)
    }

    @Test
    fun confidence_is_clamped() {
        val parsed = parser.parse("""{"reply":"Okay","confidence":4.0}""")
        assertEquals(1.0, parsed?.confidence ?: 0.0, 0.001)
    }

    @Test
    fun empty_reply_is_rejected() {
        assertNull(parser.parse("""{"reply":"   "}"""))
        assertNull(parser.parse("   "))
    }
}
