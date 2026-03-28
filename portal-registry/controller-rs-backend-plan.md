# Implementation Notes: `portal-registry` on `controller-rs`

This document now describes the current `portal-registry` direction after removing the legacy light-portal REST controller path.

## 1. Scope

`portal-registry` is now aligned only to `controller-rs`.

There is no remaining support for:

- legacy REST lookup
- legacy REST TTL check calls
- generic `/ws` controller RPC
- `controller.register`
- `controller.subscribe`
- `controller.unsubscribe`

## 2. Active Channel Model

The module uses two controller-rs WebSocket contracts.

### 2.1 Registration Channel

Endpoint:

- `/ws/microservice`

Used for:

- service self-registration
- controller-to-service commands
- reconnect-based liveness

Behavior:

- each registered service instance gets its own WebSocket
- first message is `service/register`
- socket close is the deregistration mechanism
- reconnect produces a new `runtimeInstanceId`

### 2.2 Discovery Channel

Endpoint:

- `/ws/discovery`

Used for:

- discovery lookup
- discovery subscribe
- discovery unsubscribe
- `discovery/changed` notifications

Behavior:

- one shared discovery socket per registry client
- authenticated with bearer token on upgrade
- subscriptions are replayed after reconnect

## 3. Current Config Surface

The active config is:

- `portalUrl`
- `portalToken`
- `controllerDiscoveryToken`
- existing timing fields already present in `portal-registry`

Notes:

- `controllerDiscoveryToken` is used for `/ws/discovery`
- if `controllerDiscoveryToken` is blank, `portalToken` is reused
- `portalToken` is also used as the service JWT source passed in `service/register`
- the controller should verify that token from its JWKS endpoint, for example `http://localhost:6881/oauth2/AZZRJE52eXu3t1hseacnGQ/keys`
- current service identity comes from `cid`; future tokens may use `sid`
- `ttlCheck` and `httpCheck` remain in config for compatibility, but the controller-rs path does not use REST heartbeat/check endpoints

## 4. Registry Service Mapping

`PortalRegistryService` now carries a real `version` field.

Required mapping:

- `serviceId` from the light-4j registry URL path
- `envTag` from the environment tag
- `version` from `URL.getVersion()`
- `protocol` from the registry URL
- `port` from the registry URL

Current controller-rs registration payload shape:

```json
{
  "jwt": "<service-jwt>",
  "serviceId": "com.networknt.user-1.0.0",
  "envTag": "prod",
  "environment": "prod",
  "version": "1.0.0",
  "protocol": "https",
  "port": 8443,
  "tags": {}
}
```

## 5. Registry Behavior

### 5.1 Register / Unregister

- `doRegister()` opens or refreshes a `/ws/microservice` socket for the service instance
- `doUnregister()` closes that socket
- no explicit deregister RPC is sent

### 5.2 Available / Unavailable

- `doAvailable(null)` is a no-op for controller-rs
- `doUnavailable(null)` is a no-op for controller-rs
- WebSocket connection state replaces the old TTL heartbeat flow

### 5.3 Subscribe / Discover / Unsubscribe

- `doSubscribe()` ensures `/ws/discovery` is connected and sends `discovery/subscribe`
- `doDiscover()` uses cached entries and falls back to discovery subscribe/lookup behavior
- `doUnsubscribe()` sends `discovery/unsubscribe`
- cache refresh notifications only come from `discovery/changed`

## 6. Reconnect Rules

### 6.1 Discovery Socket

- reconnect with backoff
- replay active subscriptions
- keep notification handler attached

### 6.2 Microservice Sockets

- reconnect by replaying `service/register`
- keep one socket per service instance
- old instance sockets are closed before replay

### 6.3 Explicit Close

Explicit close now:

- closes the discovery socket
- closes all per-service registration sockets
- clears registration and subscription state
- stops reconnect replay from rehydrating old state

## 7. Cleanup Summary

The migration layer has been removed. `portal-registry` should now be treated as a controller-rs client, not a dual-mode adapter.

Any future work in this module should assume:

- `/ws/microservice` is authoritative for registration
- `/ws/discovery` is authoritative for discovery
- MCP is not part of the registry integration
