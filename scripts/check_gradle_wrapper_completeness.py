#!/usr/bin/env python3
from pathlib import Path
import hashlib
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
PROPS = ROOT / "gradle/wrapper/gradle-wrapper.properties"
JAR = ROOT / "gradle/wrapper/gradle-wrapper.jar"
EXPECTED_VERSION = "9.6.0"
EXPECTED_JAR_SHA = "497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
EXPECTED_DIST_SHA = "bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01"

if not PROPS.exists():
    print("GRADLE WRAPPER COMPLETENESS: FAIL - properties missing")
    sys.exit(1)

text = PROPS.read_text(encoding="utf-8")
if f"gradle-{EXPECTED_VERSION}-bin.zip" not in text:
    print("GRADLE WRAPPER COMPLETENESS: FAIL - unexpected Gradle distribution")
    sys.exit(1)
match = re.search(r"^distributionSha256Sum=([0-9a-f]{64})$", text, re.M)
if not match or match.group(1) != EXPECTED_DIST_SHA:
    print("GRADLE WRAPPER COMPLETENESS: FAIL - distribution checksum pin missing/mismatched")
    sys.exit(1)

if not JAR.exists():
    print("GRADLE WRAPPER COMPLETENESS: BOOTSTRAP REQUIRED - verified wrapper JAR is not vendored")
    print("- run: bash scripts/bootstrap_gradle_wrapper.sh")
    sys.exit(2)

actual = hashlib.sha256(JAR.read_bytes()).hexdigest()
if actual != EXPECTED_JAR_SHA:
    print("GRADLE WRAPPER COMPLETENESS: FAIL - wrapper JAR checksum mismatch")
    print(f"- expected: {EXPECTED_JAR_SHA}")
    print(f"- actual:   {actual}")
    sys.exit(1)

print("GRADLE WRAPPER COMPLETENESS: PASS")
print(f"- Gradle: {EXPECTED_VERSION}")
print(f"- wrapper JAR SHA-256: {actual}")
print(f"- distribution SHA-256: {EXPECTED_DIST_SHA}")
