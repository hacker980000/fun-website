# Typing Core v2 - Stage 4

Stage 4 adds bounded persistent on-device typing personalization while keeping the AI/backend/context stack unchanged.

## Added
- `PersistentTypingLearningModel` backed by app-private SharedPreferences.
- Bounded local retention: up to 512 learned words, 1,024 learned word transitions, and 512 roman-to-rendered phonetic pairs.
- Learned word frequency now boosts current-word candidate ranking.
- Learned roman/phonetic pairs can appear as `PERSONAL` candidates.
- A repeatedly confirmed personal phonetic pair can become a conservative exact personal autocorrect after a high-confidence threshold.
- Learned-only bigrams can become next-word candidates even when the pair is absent from the built-in corpus.
- Explicit suggestion taps receive a stronger learning weight than passive word commits.
- Personal learning survives IME/app restarts and remains device-local.

## User control and privacy
- Keyboard Preferences includes a Personal typing learning on/off control.
- Keyboard Preferences includes Clear Learned Typing Data.
- Disabling learning stops new personal records while keeping the standard offline dictionary/corpus available.
- Clearing learned data removes the local word, bigram, and phonetic-pair store.
- Password, PIN, OTP, payment/card, banking-sensitive, and other protected fields do not write personal typing-learning data.
- Learned typing data is not uploaded and is not mixed with AI training.

## Verification
- Stage 1 phonetic self-test: 18 checks PASS.
- Stage 2 suggestion self-test: 14 checks PASS.
- Stage 3 suggestion self-test: 15 checks PASS.
- Stage 3 typing-engine self-test: 9 checks PASS.
- Stage 4 personalization self-test: 7 checks PASS.
- Persistent store Android-stub compile: PASS.
- Persistent store save/reload/clear runtime test using a fake SharedPreferences implementation: 7 checks PASS.
- Stage 2/3/4 static verification, bounded-update verification, and release verification: PASS.
- Full Gradle unit/APK build still requires a network-enabled environment because the Gradle 9.6 distribution is not cached here and `services.gradle.org` cannot be resolved.
