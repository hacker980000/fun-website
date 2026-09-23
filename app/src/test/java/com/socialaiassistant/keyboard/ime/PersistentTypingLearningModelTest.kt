package com.socialaiassistant.keyboard.ime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PersistentTypingLearningModelTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PersistentTypingLearningModel.clearStoredLearning(context)
    }

    @After
    fun tearDown() {
        PersistentTypingLearningModel.clearStoredLearning(context)
    }

    @Test
    fun learning_survives_new_model_instance_and_can_be_cleared() {
        val first = PersistentTypingLearningModel(context)
        repeat(3) {
            first.recordWord("\u0986\u09ae\u09bf")
            first.recordTransition("\u0986\u09ae\u09bf", "\u098f\u0996\u09a8")
            first.recordPhonetic("omor", "\u0993\u09ae\u09b0", weight = 3)
        }
        first.flush()

        val second = PersistentTypingLearningModel(context)
        assertTrue(second.wordBoost("\u0986\u09ae\u09bf") > 0)
        assertEquals("\u098f\u0996\u09a8", second.transitionCandidates("\u0986\u09ae\u09bf", 1).first().text)
        assertEquals("\u0993\u09ae\u09b0", second.trustedPhoneticText("omor", minimumWeight = 9))

        PersistentTypingLearningModel.clearStoredLearning(context)
        val cleared = PersistentTypingLearningModel(context)
        assertEquals(TypingLearningStats(0, 0, 0), cleared.stats())
    }
}
