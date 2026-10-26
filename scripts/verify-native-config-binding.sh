#!/usr/bin/env bash
set -euo pipefail
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
repo=${JACKSON3_MAVEN_REPO:?Set JACKSON3_MAVEN_REPO to the isolated source-installed cache}
image=${GRAALVM_IMAGE:-ghcr.io/graalvm/native-image-community@sha256:a7ccd8ed283a3c6b8395239a56d52b0760ac2c061e0f32ef70cff1b9aa875ae4}
repo=$(realpath "$repo")
[[ "$repo" != "$HOME/.m2/repository" ]] || { echo 'Use an isolated Maven cache' >&2; exit 1; }
module_name=${1:-client-config}
main_class=${2:-NativeOAuthSignConfigTest}
module="$root/$module_name"
jar="$module/target/$module_name-2.4.2-SNAPSHOT.jar"
test -s "$jar"
# CI seeds this pinned plugin before the offline check; local runs never download.
mvn -B -o -Dmaven.repo.local="$repo" -f "$module/pom.xml"     org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath     -DincludeScope=runtime -Dmdep.outputFile=target/native-runtime.classpath
out="$module/target/native-binding"
mkdir -p "$out/classes"
cp="$jar:$(cat "$module/target/native-runtime.classpath")"
javac -cp "$cp" -d "$out/classes" "$module/src/test/native/$main_class.java"
java -cp "$out/classes:$cp" "$main_class"
docker run --rm --pull="${GRAALVM_PULL:-never}" --network=none --user "$(id -u):$(id -g)"     -v "$root:$root" -v "$repo:$repo:ro" -w "$root" --entrypoint native-image "$image"     --no-fallback -cp "$out/classes:$cp" "$main_class" "$out/config-binding"
"$out/config-binding"
