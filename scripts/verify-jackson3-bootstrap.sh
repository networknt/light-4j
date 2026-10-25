#!/usr/bin/env bash
# Verify both migration source trees without any cached networknt artifacts.
set -euo pipefail
: "${JACKSON3_MAVEN_REPO:?Set a dedicated empty-internal-artifact Maven repository}"
: "${JACKSON3_HTTP_CLIENT:?Set the migrated http-client source directory}"
: "${JACKSON3_YAML_RULE:?Set the migrated yaml-rule source directory}"
script_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
mkdir -p -- "$JACKSON3_MAVEN_REPO"
verification_repo=$(cd -- "$JACKSON3_MAVEN_REPO" && pwd -P)
if [[ -e "$verification_repo/com/networknt" ]]; then
    echo "Refusing cached networknt artifacts; select a fresh verification repository." >&2
    exit 1
fi
http_sources=$(cd -- "$JACKSON3_HTTP_CLIENT" && pwd -P)
yaml_sources=$(cd -- "$JACKSON3_YAML_RULE" && pwd -P)
for source in "$script_root" "$http_sources" "$yaml_sources"; do
    [[ -f "$source/pom.xml" ]] || { echo "Missing pom.xml in $source" >&2; exit 1; }
done
if [[ ${JACKSON3_REUSE_CORE_QUALIFICATION:-0} == 1 ]]; then
    : "${JACKSON3_CORE_QUALIFICATION_REPORT:?Set the qualification evidence path}"
    python3 "$script_root/scripts/jackson3_core_qualification.py" --source "$script_root" --report "$JACKSON3_CORE_QUALIFICATION_REPORT"
fi
maven_args=("-Dmaven.repo.local=$verification_repo")
if [[ ${JACKSON3_MAVEN_OFFLINE:-0} == 1 ]]; then
    # Third-party artifacts/plugins may be seeded for an offline run; networknt must be absent.
    maven_args=(-o "${maven_args[@]}")
fi
(cd -- "$yaml_sources" && mvn "${maven_args[@]}" install -DskipTests)
(cd -- "$script_root" && mvn "${maven_args[@]}" -pl status,monad-result,config,client-config,cluster -am install -DskipTests)
(cd -- "$http_sources" && mvn "${maven_args[@]}" clean install)
if [[ ${JACKSON3_REUSE_CORE_QUALIFICATION:-0} != 1 ]]; then
    (cd -- "$script_root" && mvn "${maven_args[@]}" clean test)
fi
# Tests have passed; package/install without repeating the suite.
(cd -- "$script_root" && mvn "${maven_args[@]}" install -DskipTests)

# Optional downstream sources let consumer CI reproduce the unpublished stack.
if [[ -n ${JACKSON3_CONSUMER:-} ]]; then
    : "${JACKSON3_JSON_OVERLAY:?Set the migrated json-overlay source directory}"
    : "${JACKSON3_OPENAPI_PARSER:?Set the migrated openapi-parser source directory}"
    for source in "$JACKSON3_JSON_OVERLAY" "$JACKSON3_OPENAPI_PARSER"; do
        (cd -- "$source" && mvn "${maven_args[@]}" clean install)
    done
    (cd -- "$JACKSON3_CONSUMER" && mvn "${maven_args[@]}" clean test)
fi
