#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
work_dir="$(mktemp -d /tmp/light4j-hmac-phase4.XXXXXX)"
container_name=""

cleanup() {
    if [[ -n "$container_name" ]]; then
        docker rm -f "$container_name" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT

cd "$repo_dir"

echo "[phase4] Running affected HMAC and Unified Security regression tests"
mvn -q -pl hmac-redis,unified-security -am \
    -Dtest='HandlerTest,Hmac*Test,LocalWebhookReplayStoreTest,RedisWebhookReplayStoreTest,UnifiedSecurity*Test' \
    -Dsurefire.failIfNoSpecifiedTests=false test

echo "[phase4] HTTP/1.1 and HTTP/2 counting-callee matrix passed"

redis_url="${HMAC_PHASE4_REDIS_URL:-}"
if [[ -z "$redis_url" ]]; then
    redis_image="${HMAC_PHASE4_REDIS_IMAGE:-redis:7-alpine}"
    container_name="light4j-hmac-phase4-$$"
    echo "[phase4] Starting disposable Redis container from $redis_image"
    docker run -d --rm --name "$container_name" -p 127.0.0.1::6379 "$redis_image" >"$work_dir/container-id"
    for attempt in {1..30}; do
        if docker exec "$container_name" redis-cli ping 2>/dev/null | rg -q '^PONG$'; then
            break
        fi
        if [[ "$attempt" == 30 ]]; then
            echo "Redis did not become ready" >&2
            exit 1
        fi
        sleep 1
    done
    port_mapping="$(docker port "$container_name" 6379/tcp)"
    redis_url="redis://127.0.0.1:${port_mapping##*:}"
fi

echo "[phase4] Running the complete Undertow/HMAC chain against real Redis"
WEBHOOK_REPLAY_REDIS_URL="$redis_url" mvn -q -pl hmac-redis -am \
    -Dtest=RedisHmacChainQualificationTest \
    -Dhmac.phase4.redisChain=true \
    -Dsurefire.failIfNoSpecifiedTests=false test

report="hmac-redis/target/surefire-reports/TEST-com.networknt.hmac.redis.RedisWebhookReplayStoreTest.xml"
classpath="$(sed -n 's/.*<property name="java.class.path" value="\([^"]*\)"\/>.*/\1/p' "$report" | head -1)"
if [[ -z "$classpath" ]]; then
    echo "Unable to obtain the tested Maven classpath from $report" >&2
    exit 1
fi

key_prefix="light:hmac-phase4:$$:"
delivery_id="phase4-distributed-race-$$"
probe_class="com.networknt.hmac.redis.RedisReplayQualificationProcess"

echo "[phase4] Racing two independent JVMs against $redis_url"
java -cp "$classpath" "$probe_class" "$redis_url" "$key_prefix" "$delivery_id" 300 >"$work_dir/process-1.log" 2>&1 &
first_pid=$!
java -cp "$classpath" "$probe_class" "$redis_url" "$key_prefix" "$delivery_id" 300 >"$work_dir/process-2.log" 2>&1 &
second_pid=$!
wait "$first_pid"
wait "$second_pid"

reserved_count="$(rg --no-filename 'HMAC_PHASE4_OUTCOME=RESERVED' "$work_dir/process-1.log" "$work_dir/process-2.log" | wc -l)"
duplicate_count="$(rg --no-filename 'HMAC_PHASE4_OUTCOME=DUPLICATE' "$work_dir/process-1.log" "$work_dir/process-2.log" | wc -l)"
if [[ "$reserved_count" != 1 || "$duplicate_count" != 1 ]]; then
    echo "Expected one RESERVED and one DUPLICATE result" >&2
    sed -n '/HMAC_PHASE4_OUTCOME=/p' "$work_dir"/process-*.log >&2
    exit 1
fi

echo "[phase4] Starting a third JVM to prove replay state survives process restart"
java -cp "$classpath" "$probe_class" "$redis_url" "$key_prefix" "$delivery_id" 300 >"$work_dir/process-3.log" 2>&1
if ! rg -q 'HMAC_PHASE4_OUTCOME=DUPLICATE' "$work_dir/process-3.log"; then
    echo "Expected restarted JVM to observe DUPLICATE" >&2
    sed -n '/HMAC_PHASE4_OUTCOME=/p' "$work_dir/process-3.log" >&2
    exit 1
fi

echo "[phase4] PASS: protocol matrix, real-Redis full-chain, and multi-JVM replay qualification"
