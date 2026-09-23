package com.socialaiassistant.keyboard.debug

import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.socialaiassistant.keyboard.R

/**
 * Debug-only manual validation surface for real-device IME testing.
 *
 * Stable debug-only resource IDs are intentional: Stage 15 UIAutomator smoke tests
 * can focus a known editor without relying on screen coordinates.
 */
class ImeValidationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Keyboard IME Validation"

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        content.addView(label(
            "Debug-only IME validation harness. Test Bangla/English composition, " +
                "suggestions, Glide, cursor movement, editor actions, and field safety."
        ))

        addField(R.id.validation_normal, content, "Normal text / Done", InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_DONE)
        addField(
            R.id.validation_chat,
            content,
            "Chat / Send",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
            EditorInfo.IME_ACTION_SEND
        )
        addField(
            R.id.validation_multiline,
            content,
            "Multiline / Enter",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            EditorInfo.IME_ACTION_NONE
        )
        addField(
            R.id.validation_no_suggestions,
            content,
            "No suggestions",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
            EditorInfo.IME_ACTION_DONE
        )
        addField(
            R.id.validation_email,
            content,
            "Email (literal Latin)",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            EditorInfo.IME_ACTION_NEXT
        )
        addField(
            R.id.validation_url,
            content,
            "URL (literal Latin)",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            EditorInfo.IME_ACTION_GO
        )
        addField(
            R.id.validation_password,
            content,
            "Password (sensitive)",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            EditorInfo.IME_ACTION_DONE
        )
        addField(R.id.validation_phone, content, "Phone / numeric layer", InputType.TYPE_CLASS_PHONE, EditorInfo.IME_ACTION_DONE)
        addField(
            R.id.validation_number,
            content,
            "Number / numeric layer",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            EditorInfo.IME_ACTION_DONE
        )

        setContentView(ScrollView(this).apply { addView(content) })
    }

    private fun addField(fieldId: Int, parent: LinearLayout, hint: String, inputType: Int, imeOptions: Int) {
        parent.addView(label(hint))
        parent.addView(EditText(this).apply {
            id = fieldId
            contentDescription = hint
            this.hint = hint
            this.inputType = inputType
            this.imeOptions = imeOptions
            minLines = if ((inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) 3 else 1
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        })
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setPadding(0, dp(8), 0, dp(4))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
