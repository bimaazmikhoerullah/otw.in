#!/bin/bash
set -e

# Some environments have Android Studio's JBR but no global java/JAVA_HOME.
if [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null 2>&1; then
  for candidate in /snap/android-studio/current/jbr /snap/android-studio/*/jbr; do
    if [ -x "$candidate/bin/java" ]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

echo "=== Harness Initialization ==="

echo "=== ./gradlew test ==="
./gradlew test

echo "=== ./gradlew build ==="
./gradlew build

echo "=== ./gradlew lint ==="
./gradlew lint

echo "=== Verification Complete ==="
echo ""
echo "Next steps:"
echo "1. Read feature_list.json to see current feature state"
echo "2. Pick ONE unfinished feature to work on"
echo "3. Implement only that feature"
echo "4. Re-run verification before claiming done"
