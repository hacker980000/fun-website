package com.socialaiassistant.keyboard.ime

import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.socialaiassistant.keyboard.safety.FieldSafety
import com.socialaiassistant.keyboard.safety.SensitiveFieldPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorInfoSafetyAdapterTest {
    @Test
    fun label_field_name_and_action_label_are_forwarded() {
        val editor = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.example"
            label = "Secure payment"
            fieldName = "paymentPinInput"
            actionLabel = "Verify"
        }
        val descriptor = EditorInfoSafetyAdapter.descriptor(editor)
        assertEquals("Secure payment", descriptor.labelText)
        assertEquals("paymentPinInput", descriptor.fieldName)
        assertEquals("Verify", descriptor.actionLabel)
        assertEquals(FieldSafety.BLOCK_AI, SensitiveFieldPolicy().evaluate(descriptor))
    }

    @Test
    fun relevant_editor_extras_are_bounded_and_classified() {
        val editor = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.example"
            extras = Bundle().apply {
                putString("androidx.autofill.hint", "smsOTPCode")
                putString("unrelated.large.payload", "do-not-need-this")
            }
        }
        val descriptor = EditorInfoSafetyAdapter.descriptor(editor)
        assertTrue(descriptor.editorMetadataHints.any { it.contains("autofill", ignoreCase = true) })
        assertTrue(descriptor.editorMetadataHints.any { it.contains("smsOTPCode") })
        assertEquals(FieldSafety.BLOCK_AI, SensitiveFieldPolicy().evaluate(descriptor))
    }
}
