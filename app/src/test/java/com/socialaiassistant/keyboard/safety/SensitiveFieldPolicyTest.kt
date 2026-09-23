package com.socialaiassistant.keyboard.safety

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Test

class SensitiveFieldPolicyTest {
    private val policy = SensitiveFieldPolicy()

    @Test
    fun password_blocks_ai() {
        val descriptor = EditorDescriptor(
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            packageName = "com.example.app",
            hintText = "Password",
            accessibilityPassword = true
        )
        assertEquals(FieldSafety.BLOCK_AI, policy.evaluate(descriptor))
    }

    @Test
    fun visible_and_web_passwords_block_ai() {
        assertBlocked(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, "Credential")
        assertBlocked(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, "Credential")
        assertBlocked(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, null)
    }

    @Test
    fun otp_pin_card_and_cvv_metadata_block_ai() {
        assertBlocked(InputType.TYPE_CLASS_NUMBER, "Enter OTP")
        assertBlocked(InputType.TYPE_CLASS_NUMBER, "PIN")
        assertBlocked(InputType.TYPE_CLASS_NUMBER, "Card number")
        assertBlocked(InputType.TYPE_CLASS_NUMBER, "CVV")
    }

    @Test
    fun camel_case_autofill_and_html_tokens_block_ai() {
        assertEquals(
            FieldSafety.BLOCK_AI,
            policy.evaluate(
                EditorDescriptor(
                    InputType.TYPE_CLASS_TEXT,
                    "com.example",
                    null,
                    false,
                    autofillHints = listOf("smsOTPCode", "2faAppOTPCode")
                )
            )
        )
        assertEquals(
            FieldSafety.BLOCK_AI,
            policy.evaluate(
                EditorDescriptor(
                    InputType.TYPE_CLASS_TEXT,
                    "com.example",
                    null,
                    false,
                    editorMetadataHints = listOf("autocomplete=one-time-code", "creditCardSecurityCode")
                )
            )
        )
    }

    @Test
    fun field_name_label_and_private_ime_options_are_security_signals() {
        assertEquals(
            FieldSafety.BLOCK_AI,
            policy.evaluate(
                EditorDescriptor(
                    InputType.TYPE_CLASS_TEXT,
                    "com.example",
                    null,
                    false,
                    fieldName = "verificationCodeInput"
                )
            )
        )
        assertEquals(
            FieldSafety.BLOCK_AI,
            policy.evaluate(
                EditorDescriptor(
                    InputType.TYPE_CLASS_TEXT,
                    "com.example",
                    null,
                    false,
                    labelText = "Payment PIN"
                )
            )
        )
        assertEquals(
            FieldSafety.BLOCK_AI,
            policy.evaluate(
                EditorDescriptor(
                    InputType.TYPE_CLASS_TEXT,
                    "com.example",
                    null,
                    false,
                    privateImeOptions = "credential=creditCardNumber"
                )
            )
        )
    }

    @Test
    fun bangla_sensitive_metadata_blocks_ai() {
        assertBlocked(InputType.TYPE_CLASS_TEXT, "ওটিপি লিখুন")
        assertBlocked(InputType.TYPE_CLASS_NUMBER, "বিকাশ পিন")
        assertBlocked(InputType.TYPE_CLASS_NUMBER, "কার্ড নম্বর")
    }

    @Test
    fun unlabeled_or_unknown_numeric_field_fails_closed() {
        assertEquals(
            FieldSafety.BLOCK_AI,
            policy.evaluate(EditorDescriptor(InputType.TYPE_CLASS_NUMBER, "com.example", null, false))
        )
        assertEquals(
            FieldSafety.BLOCK_AI,
            policy.evaluate(EditorDescriptor(InputType.TYPE_CLASS_NUMBER, "com.example", "Number", false))
        )
    }

    @Test
    fun explicitly_benign_numeric_fields_disable_conversation_without_secret_classification() {
        listOf("Amount", "Quantity", "Age", "Postal code", "দাম").forEach { hint ->
            assertEquals(
                "hint=$hint",
                FieldSafety.NO_CONVERSATION,
                policy.evaluate(EditorDescriptor(InputType.TYPE_CLASS_NUMBER, "com.example", hint, false))
            )
        }
    }

    @Test
    fun phone_and_datetime_fields_are_not_conversation_surfaces() {
        assertEquals(
            FieldSafety.NO_CONVERSATION,
            policy.evaluate(EditorDescriptor(InputType.TYPE_CLASS_PHONE, "com.example", "Phone", false))
        )
        assertEquals(
            FieldSafety.NO_CONVERSATION,
            policy.evaluate(EditorDescriptor(InputType.TYPE_CLASS_DATETIME, "com.example", "Date", false))
        )
    }

    @Test
    fun ordinary_message_allows_ai() {
        val descriptor = EditorDescriptor(
            inputType = InputType.TYPE_CLASS_TEXT,
            packageName = "org.telegram.messenger",
            hintText = "Message",
            accessibilityPassword = false
        )
        assertEquals(FieldSafety.ALLOW_AI, policy.evaluate(descriptor))
    }

    @Test
    fun banking_support_chat_is_not_blocked_by_package_name_alone() {
        val descriptor = EditorDescriptor(
            inputType = InputType.TYPE_CLASS_TEXT,
            packageName = "com.example.bank",
            hintText = "Message support",
            accessibilityPassword = false
        )
        assertEquals(FieldSafety.ALLOW_AI, policy.evaluate(descriptor))
    }

    private fun assertBlocked(inputType: Int, hint: String?) {
        assertEquals(
            FieldSafety.BLOCK_AI,
            policy.evaluate(EditorDescriptor(inputType, "com.example", hint, false))
        )
    }
}
