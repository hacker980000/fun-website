package com.socialaiassistant.keyboard.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelEnvelopeNormalizerTest {
    @Test fun strips_standalone_json_label_and_case_insensitive_fence() {
        val raw = """JSON
```JSON
{"reply":"hello"}
```"""
        assertEquals("{\"reply\":\"hello\"}", ModelEnvelopeNormalizer.normalize(raw))
    }

    @Test fun identifies_json_like_invalid_payloads() {
        assertTrue(ModelEnvelopeNormalizer.looksLikeEnvelope("{\"reply\":\"hello\", bad}"))
        assertTrue(ModelEnvelopeNormalizer.looksLikeEnvelope("```json bad ```"))
        assertTrue(ModelEnvelopeNormalizer.looksLikeEnvelope("JSON\n{bad}"))
        assertFalse(ModelEnvelopeNormalizer.looksLikeEnvelope("Reply: hello"))
    }
}
