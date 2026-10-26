#!/usr/bin/env bash
set -euo pipefail
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
repo=${JACKSON3_MAVEN_REPO:?Set JACKSON3_MAVEN_REPO to the isolated source-installed cache}
image=${GRAALVM_IMAGE:-$(cat "$root/scripts/graalvm-native-image.txt")}
repo=$(realpath "$repo")
[[ "$repo" != "$HOME/.m2/repository" ]] || { echo 'Use an isolated Maven cache' >&2; exit 1; }
module_name=${1:-client-config}
main_class=${2:-NativeOAuthSignConfigTest}
module="$root/$module_name"
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
maven_args=(-B -o "-Dmaven.repo.local=$repo" -f "$module/pom.xml")
if [[ -n "${JACKSON3_NATIVE_MAVEN_PROFILE:-}" ]]; then
    maven_args+=("-P$JACKSON3_NATIVE_MAVEN_PROFILE")
fi
# CI seeds these pinned plugins before the offline check; local runs never download.
mvn "${maven_args[@]}" \
    org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom \
    "-Doutput=$work/effective-pom.xml"
python3 "$root/scripts/native_config_artifact.py" "$work/effective-pom.xml" > "$work/paths"
mapfile -t paths < "$work/paths"
jar=${paths[1]}
test -s "$jar" || { echo "Build the module jar first: $jar" >&2; exit 1; }
out="${paths[0]}/native-binding"
mkdir -p "$out/classes"
mvn "${maven_args[@]}" \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
    -DincludeScope=runtime "-Dmdep.outputFile=$out/runtime.classpath"
mvn "${maven_args[@]}" \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
    -DincludeScope=test -DincludeGroupIds=com.networknt -DincludeArtifactIds=jackson-defaults \
    -DincludeClassifiers=tests "-Dmdep.outputFile=$out/shared-cases.classpath"
shared_cases=$(cat "$out/shared-cases.classpath")
test -s "$shared_cases" || { echo 'Missing jackson-defaults test-support jar' >&2; exit 1; }
fixture_classpath="$jar:$(cat "$out/runtime.classpath"):$shared_cases"
javac -cp "$fixture_classpath" -d "$out/classes" "$module/src/test/native/$main_class.java"
java -cp "$out/classes:$fixture_classpath" "$main_class"
native_args=(--no-fallback)
if [[ ${JACKSON3_NATIVE_ALL_CHARSETS:-0} == 1 ]]; then
    native_args+=(-H:+AddAllCharsets)
fi
if [[ -n "${JACKSON3_NATIVE_EXTRA_REFLECTION:-}" ]]; then
    native_args+=("-H:ReflectionConfigurationFiles=$JACKSON3_NATIVE_EXTRA_REFLECTION")
fi
docker run --rm --pull="${GRAALVM_PULL:-never}" --network=none --user "$(id -u):$(id -g)" \
    -v "$root:$root" -v "$repo:$repo:ro" -w "$root" --entrypoint native-image "$image" \
    "${native_args[@]}" -cp "$out/classes:$fixture_classpath" "$main_class" "$out/config-binding"
"$out/config-binding"
