#!/usr/bin/env bash
# Build-only fixture setup. This never publishes full-core qualification evidence.
set -euo pipefail
: "${JACKSON3_MAVEN_REPO:?Set a fresh isolated repository}"
: "${JACKSON3_YAML_RULE:?Set yaml-rule source}"
: "${JACKSON3_HTTP_CLIENT:?Set http-client source}"
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
mkdir -p "$JACKSON3_MAVEN_REPO"
repo=$(realpath "$JACKSON3_MAVEN_REPO")
[[ ! -e "$repo/com/networknt" ]] || { echo 'Native fixture setup requires a fresh internal-artifact cache' >&2; exit 1; }
args=(-B "-Dmaven.repo.local=$repo" -DskipTests)
if [[ ${JACKSON3_MAVEN_OFFLINE:-0} == 1 ]]; then args+=(-o); fi
mvn "${args[@]}" -f "$JACKSON3_YAML_RULE/pom.xml" install
mvn "${args[@]}" -f "$root/pom.xml" \
    -pl status,monad-result,config,client-config,cluster -am install
mvn "${args[@]}" -f "$JACKSON3_HTTP_CLIENT/pom.xml" install
mvn "${args[@]}" -f "$root/pom.xml" \
    -pl "${JACKSON3_NATIVE_MODULES:-client-config}" -am install
