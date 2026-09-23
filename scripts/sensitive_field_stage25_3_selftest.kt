import com.socialaiassistant.keyboard.safety.EditorDescriptor
import com.socialaiassistant.keyboard.safety.FieldSafety
import com.socialaiassistant.keyboard.safety.SensitiveFieldPolicy
import com.socialaiassistant.keyboard.safety.SensitiveMetadataClassifier

private const val TEXT = 0x00000001
private const val NUMBER = 0x00000002
private const val PHONE = 0x00000003
private const val NUMBER_PASSWORD = 0x00000012
private const val TEXT_PASSWORD = 0x00000081

fun main() {
    val policy = SensitiveFieldPolicy()
    fun expect(name: String, expected: FieldSafety, descriptor: EditorDescriptor) {
        val actual = policy.evaluate(descriptor)
        check(actual == expected) { "$name expected=$expected actual=$actual" }
        println("PASS: $name -> $actual")
    }

    expect("text password", FieldSafety.BLOCK_AI, EditorDescriptor(TEXT_PASSWORD, "pkg", null, false))
    expect("number password", FieldSafety.BLOCK_AI, EditorDescriptor(NUMBER_PASSWORD, "pkg", null, false))
    expect("generic numeric fail closed", FieldSafety.BLOCK_AI, EditorDescriptor(NUMBER, "pkg", null, false))
    expect("benign amount numeric", FieldSafety.NO_CONVERSATION, EditorDescriptor(NUMBER, "pkg", "Amount", false))
    expect("phone no conversation", FieldSafety.NO_CONVERSATION, EditorDescriptor(PHONE, "pkg", "Phone", false))
    expect("normal message", FieldSafety.ALLOW_AI, EditorDescriptor(TEXT, "pkg", "Message", false))
    expect("fieldName OTP", FieldSafety.BLOCK_AI, EditorDescriptor(TEXT, "pkg", null, false, fieldName = "verificationCodeInput"))
    expect("autofill SMS OTP", FieldSafety.BLOCK_AI, EditorDescriptor(TEXT, "pkg", null, false, autofillHints = listOf("smsOTPCode")))
    expect("HTML one-time-code", FieldSafety.BLOCK_AI, EditorDescriptor(TEXT, "pkg", null, false, editorMetadataHints = listOf("autocomplete=one-time-code")))
    expect("credit card security code", FieldSafety.BLOCK_AI, EditorDescriptor(TEXT, "pkg", null, false, editorMetadataHints = listOf("creditCardSecurityCode")))
    expect("Bangla bKash PIN", FieldSafety.BLOCK_AI, EditorDescriptor(NUMBER, "pkg", "বিকাশ পিন", false))
    expect("bank support text", FieldSafety.ALLOW_AI, EditorDescriptor(TEXT, "com.example.bank", "Message support", false))

    check(SensitiveMetadataClassifier.normalize("smsOTPCode") == "sms otp code")
    check(SensitiveMetadataClassifier.normalize("creditCardSecurityCode") == "credit card security code")
    println("STAGE25.3 SENSITIVE FIELD SELFTEST: PASS")
}
