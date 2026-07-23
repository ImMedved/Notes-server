# Android Client Notes

## Basic Flow

1. On startup, call `GET /api/v1/snapshot`.
2. Keep a local snapshot cache in memory or SQLite.
3. After any change, send a `PUT` or `DELETE` request.
4. After a successful mutation, fetch `snapshot` again.

## What You Do Not Need to Reinvent

- The note format is already fixed.
- The timer format is already fixed.
- Time fields use Unix epoch milliseconds.
- The `X-Notes-Api-Key` header is already reserved.
- `X-Client-Id`, `X-Client-Platform`, and `X-Client-Version` are already documented.

## Recommended Local Android Architecture

- `Repository`
  Works with the HTTP API and the local cache.
- `SQLite/Room`
  Stores the latest snapshot and drafts if offline-first support is needed later.
- `SyncWorker`
  Performs periodic pull or push work through WorkManager.

## Important Limits of the Current Contract

- The server currently returns a full snapshot, not delta sync.
- Conflicts where two clients edit the same note at the same time are resolved with `last write wins`.
- The server does not store change history.

## What Can Be Added Later Without Breaking Clients

- `GET /api/v1/changes?sinceRevision=...`
- separate authentication endpoints
- push events through Redis/pubsub or a websocket gateway
- device registration and client operation auditing
