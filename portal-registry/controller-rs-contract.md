# Contract: `light-4j/portal-registry` with `controller-rs`

This document defines the active interoperability contract between:

- `light-4j/portal-registry`
- `controller-rs`

It reflects the current code, not a migration target.

## 1. Principles

The integration uses two internal control-plane channels:

1. service self-registration
2. consumer-side discovery

These concerns are intentionally separate.

This contract does not use:

- `/ws/mcp`
- `/ws/portal-events`
- legacy REST controller endpoints
- generic `/ws` controller RPC

## 2. Channel Model

### 2.1 Registration Channel

Endpoint:

- `wss://<controller-host>:<port>/ws/microservice`

Purpose:

- service registration
- service socket liveness
- controller-to-service commands
- service-to-controller command responses

### 2.2 Discovery Channel

Endpoint:

- `wss://<controller-host>:<port>/ws/discovery`

Purpose:

- one-shot discovery lookup
- discovery subscriptions
- discovery change notifications

## 3. Authentication Model

### 3.1 Registration Channel

Authentication is part of the first message payload.

Method:

- `service/register`

Field:

- `params.jwt`

The JWT is sent without a `Bearer ` prefix inside the JSON payload.

### 3.2 Discovery Channel

Authentication is on the WebSocket upgrade request.

Header:

- `Authorization: Bearer <token>`

`portal-registry` uses:

- `controllerDiscoveryToken` when configured
- otherwise `portalToken`

For local light-portal development, the controller JWKS endpoint is:

- `http://localhost:6881/oauth2/AZZRJE52eXu3t1hseacnGQ/keys`

## 4. Registration Contract

### 4.1 Register Request

```json
{
  "jsonrpc": "2.0",
  "id": "register-1",
  "method": "service/register",
  "params": {
    "jwt": "<service-jwt>",
    "serviceId": "com.networknt.user-1.0.0",
    "envTag": "prod",
    "environment": "prod",
    "version": "1.0.0",
    "protocol": "https",
    "port": 8443,
    "tags": {}
  }
}
```

Rules:

- `version` comes from the light-4j registry URL version, not from `serviceId`
- `address` is not part of the payload; `controller-rs` derives it from the socket remote address
- one service instance maps to one registration socket
- the service JWT must be `RS256`
- the controller resolves the signing key from JWKS by `kid` and refreshes when a new `kid` is seen
- service identity is matched with claim fallback `sid`, then `service_id`, then `cid`, then `sub`

### 4.2 Register Ack

```json
{
  "jsonrpc": "2.0",
  "id": "register-1",
  "result": {
    "runtimeInstanceId": "0195b1d4-4b3e-7f6b-9f88-47d8b11b0c15",
    "status": "registered"
  }
}
```

`portal-registry` stores:

- socket state
- returned `runtimeInstanceId`

### 4.3 Metadata Update

Optional follow-up notification from service to controller:

```json
{
  "jsonrpc": "2.0",
  "method": "service/update_metadata",
  "params": {
    "version": "1.0.1",
    "tags": {
      "commit": "abc123"
    }
  }
}
```

### 4.4 Disconnect Semantics

There is no deregister RPC.

Contract:

- registration socket close marks the instance disconnected
- reconnect creates a new `runtimeInstanceId`

## 5. Discovery Contract

Use JSON-RPC 2.0 messages on `/ws/discovery`.

### 5.1 Lookup

Request:

```json
{
  "jsonrpc": "2.0",
  "id": "lookup-1",
  "method": "discovery/lookup",
  "params": {
    "serviceId": "com.networknt.user-1.0.0",
    "envTag": "prod",
    "protocol": "https"
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": "lookup-1",
  "result": {
    "serviceId": "com.networknt.user-1.0.0",
    "envTag": "prod",
    "protocol": "https",
    "nodes": [
      {
        "runtimeInstanceId": "0195b1d4-4b3e-7f6b-9f88-47d8b11b0c15",
        "serviceId": "com.networknt.user-1.0.0",
        "envTag": "prod",
        "environment": "prod",
        "version": "1.0.0",
        "protocol": "https",
        "address": "10.0.1.25",
        "port": 8443,
        "tags": {},
        "connectedAt": "2026-03-27T14:00:00Z",
        "lastSeenAt": "2026-03-27T14:00:04Z",
        "connected": true
      }
    ]
  }
}
```

### 5.2 Subscribe

Request:

```json
{
  "jsonrpc": "2.0",
  "id": "subscribe-1",
  "method": "discovery/subscribe",
  "params": {
    "serviceId": "com.networknt.user-1.0.0",
    "envTag": "prod",
    "protocol": "https"
  }
}
```

Response:

- same shape as `discovery/lookup`

Effect:

- registers a live subscription for subsequent `discovery/changed` notifications

### 5.3 Unsubscribe

Request:

```json
{
  "jsonrpc": "2.0",
  "id": "unsubscribe-1",
  "method": "discovery/unsubscribe",
  "params": {
    "serviceId": "com.networknt.user-1.0.0",
    "envTag": "prod",
    "protocol": "https"
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": "unsubscribe-1",
  "result": {
    "status": "unsubscribed",
    "serviceId": "com.networknt.user-1.0.0",
    "envTag": "prod",
    "protocol": "https"
  }
}
```

### 5.4 Change Notification

Notification:

```json
{
  "jsonrpc": "2.0",
  "method": "discovery/changed",
  "params": {
    "serviceId": "com.networknt.user-1.0.0",
    "envTag": "prod",
    "protocol": "https",
    "nodes": [
      {
        "runtimeInstanceId": "0195b1d4-4b3e-7f6b-9f88-47d8b11b0c15",
        "serviceId": "com.networknt.user-1.0.0",
        "envTag": "prod",
        "environment": "prod",
        "version": "1.0.0",
        "protocol": "https",
        "address": "10.0.1.25",
        "port": 8443,
        "tags": {},
        "connectedAt": "2026-03-27T14:00:00Z",
        "lastSeenAt": "2026-03-27T14:00:04Z",
        "connected": true
      }
    ]
  }
}
```

Rules:

- notifications are filtered by subscribed `serviceId`
- `envTag` and `protocol` filters narrow the node set returned to the client

## 6. `portal-registry` Mapping Rules

When converting discovery nodes back to light-4j URLs:

- `protocol` maps to the URL protocol
- `address` maps to the host
- `port` maps to the port
- `envTag` maps to the environment parameter
- `version` should remain available from the service metadata mapping layer

## 7. Explicit Non-Contracts

Do not add or use:

- `controller.register`
- `controller.deregister`
- `controller.subscribe`
- `controller.unsubscribe`
- `controller.discovery.changed`

The controller-rs registry contract is only:

- `service/register`
- `service/update_metadata`
- `discovery/lookup`
- `discovery/subscribe`
- `discovery/unsubscribe`
- `discovery/changed`
