#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="9.6.0"
EXPECTED_JAR_SHA="497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
OFFICIAL_JAR_URL="https://raw.githubusercontent.com/gradle/gradle/v${VERSION}/gradle/wrapper/gradle-wrapper.jar"

sha256_file() {
  sha256sum "$1" | awk '{print $1}'
}

verify_candidate() {
  local candidate="$1"
  local actual
  actual="$(sha256_file "$candidate")"
  if [[ "$actual" != "$EXPECTED_JAR_SHA" ]]; then
    echo "Wrapper JAR checksum mismatch: $actual" >&2
    echo "Expected: $EXPECTED_JAR_SHA" >&2
    return 1
  fi
  return 0
}

if [[ -f "$JAR" ]]; then
  if verify_candidate "$JAR"; then
    echo "Verified Gradle wrapper JAR already present."
    exit 0
  fi
  echo "Refusing unverified Gradle wrapper JAR." >&2
  exit 3
fi

mkdir -p "$(dirname "$JAR")"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
candidate="$tmp/gradle-wrapper.jar"

# Fast local bootstrap: download the official Gradle-repository wrapper JAR and
# accept it only if its SHA-256 matches Gradle's published release checksum.
if command -v curl >/dev/null 2>&1; then
  if curl --fail --location --silent --show-error --retry 2 --connect-timeout 15 \
      --output "$candidate" "$OFFICIAL_JAR_URL" && verify_candidate "$candidate"; then
    cp "$candidate" "$JAR"
    echo "Gradle wrapper JAR downloaded from the official Gradle repository and verified."
    exit 0
  fi
  rm -f "$candidate"
elif command -v wget >/dev/null 2>&1; then
  if wget -q --timeout=20 --tries=2 -O "$candidate" "$OFFICIAL_JAR_URL" && verify_candidate "$candidate"; then
    cp "$candidate" "$JAR"
    echo "Gradle wrapper JAR downloaded from the official Gradle repository and verified."
    exit 0
  fi
  rm -f "$candidate"
fi

# Deterministic offline/enterprise fallback when Gradle 9.6.0 is already installed.
if command -v gradle >/dev/null 2>&1; then
  installed="$(gradle --version | awk '/^Gradle / {print $2; exit}')"
  if [[ "$installed" == "$VERSION" ]]; then
    printf 'rootProject.name = "wrapper-bootstrap"\n' > "$tmp/settings.gradle.kts"
    : > "$tmp/build.gradle.kts"
    gradle -p "$tmp" --no-daemon wrapper --gradle-version "$VERSION" --distribution-type bin
    generated="$tmp/gradle/wrapper/gradle-wrapper.jar"
    if verify_candidate "$generated"; then
      cp "$generated" "$JAR"
      echo "Gradle wrapper JAR generated locally and verified."
      exit 0
    fi
    exit 4
  fi
  echo "Installed Gradle is ${installed:-unknown}; Gradle $VERSION is required for local generation." >&2
fi

echo "Unable to restore the verified Gradle wrapper JAR." >&2
echo "Use an internet connection that can reach raw.githubusercontent.com, or install Gradle $VERSION and rerun this script." >&2
exit 2
