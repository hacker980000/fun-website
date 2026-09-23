#!/usr/bin/env python3
from pathlib import Path
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
dao = (ROOT / "app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationDao.kt").read_text()
repo = (ROOT / "app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationRepository.kt").read_text()
test = (ROOT / "app/src/test/java/com/socialaiassistant/keyboard/memory/ConversationRepositoryTest.kt").read_text()

checks = []

def require(condition: bool, message: str):
    if not condition:
        raise SystemExit(f"FAIL: {message}")
    checks.append(message)
    print(f"PASS: {message}")

require("import androidx.room.Upsert" in dao, "Room Upsert annotation imported")
require("@Upsert\n    suspend fun upsertConversation" in dao, "conversation metadata uses non-destructive @Upsert")
require("OnConflictStrategy.REPLACE" not in dao, "conversation DAO no longer uses REPLACE")
require("COARSE_TIME_BUCKET_MS" not in repo, "coarse-time dedupe removed")
require("appendStartIndex(" in repo, "rolling snapshot overlap merge is present")
require("snapshot.capturedAtMillis.toString()" in repo and "snapshotIndex.toString()" in repo,
        "new-row dedupe identity distinguishes same-text occurrences inside a snapshot")
for name in [
    "same_visible_snapshot_in_a_later_time_bucket_does_not_duplicate_history",
    "later_snapshot_preserves_existing_history_and_appends_only_new_tail",
    "identical_messages_inside_one_snapshot_are_preserved_as_distinct_rows",
]:
    require(name in test, f"regression test present: {name}")

# Reproduce the FK behavior the fix is designed to avoid. SQLite UPSERT updates the
# existing parent row in place, so ON DELETE CASCADE must not remove child rows.
conn = sqlite3.connect(":memory:")
conn.execute("PRAGMA foreign_keys = ON")
conn.execute("CREATE TABLE conversations(key TEXT PRIMARY KEY, updated INTEGER NOT NULL)")
conn.execute("CREATE TABLE messages(id INTEGER PRIMARY KEY, conversationKey TEXT NOT NULL REFERENCES conversations(key) ON DELETE CASCADE)")
conn.execute("INSERT INTO conversations(key, updated) VALUES('conv', 1)")
conn.execute("INSERT INTO messages(conversationKey) VALUES('conv')")
conn.execute("INSERT INTO conversations(key, updated) VALUES('conv', 2) ON CONFLICT(key) DO UPDATE SET updated=excluded.updated")
child_count = conn.execute("SELECT COUNT(*) FROM messages WHERE conversationKey='conv'").fetchone()[0]
require(child_count == 1, "SQLite UPSERT preserves cascade-linked message rows")
conn.close()

# Android-free model of the exact tail/prefix overlap rule used in ConversationRepository.
def append_start(existing, incoming):
    if not existing or not incoming:
        return 0
    for size in range(min(len(existing), len(incoming)), 0, -1):
        if existing[-size:] == incoming[:size]:
            return size
    return 0

require(append_start(["hello"], ["hello"]) == 1, "same visible snapshot appends nothing")
require(append_start(["hello", "hi"], ["hi", "how are you?"]) == 1,
        "rolling snapshot appends only the new tail")
require(append_start(["a", "b"], ["c", "d"]) == 0, "non-overlapping snapshot remains appendable")

print(f"Conversation memory Stage 1 verifier: {len(checks)}/{len(checks)} PASS")
