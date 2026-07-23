# HTTP API

Base prefix: `/api/v1`

Authentication:

- if `NOTES_API_KEY` is set on the server, the client must send the `X-Notes-Api-Key` header
- additional headers for telemetry and debugging:
  - `X-Client-Id`
  - `X-Client-Platform`
  - `X-Client-Version`

All request and response bodies are `application/json`.

## GET /health

Purpose:
Check server availability.

`200` response:

```json
{
  "ok": true,
  "service": "notes-server",
  "serverTimeEpochMillis": 1750438800000
}
```

## GET /api/v1/snapshot

Purpose:
Fetch the full authoritative server state.

`200` response:

```json
{
  "revision": 42,
  "serverTimeEpochMillis": 1750438800000,
  "notes": [
    {
      "id": "3dce4d7d-43de-4890-b8f8-0a0f3adce9df",
      "title": "Sprint ideas",
      "content": "# Idea",
      "pinned": true,
      "archived": false,
      "createdAt": 1750435200000,
      "updatedAt": 1750438800000
    }
  ],
  "timers": []
}
```

## PUT /api/v1/notes/{id}

Purpose:
Create or update a note. The `id` in the path is the source of truth.

Body:

```json
{
  "id": "3dce4d7d-43de-4890-b8f8-0a0f3adce9df",
  "title": "Sprint ideas",
  "content": "# Idea\n\n- add android client",
  "pinned": true,
  "archived": false,
  "createdAt": 1750435200000,
  "updatedAt": 1750438800000
}
```

`200` response:
The note object in its current server-side form.

The `archived` field controls the note's archive state:

- `false` - a regular active note.
- `true` - the note is archived.

## DELETE /api/v1/notes/{id}

Purpose:
Delete a note.

`200` response:

```json
{
  "ok": true,
  "id": "3dce4d7d-43de-4890-b8f8-0a0f3adce9df"
}
```

## PUT /api/v1/timers/{id}

Purpose:
Create or update a timer/stopwatch.

Body:

```json
{
  "id": "884f8b44-f7c4-40e4-bd3f-76658c841e65",
  "name": "Workout",
  "mode": "COUNTDOWN",
  "durationMillis": 5400000,
  "startedAt": 1750438800000,
  "accumulatedMillis": 120000,
  "running": true,
  "createdAt": 1750435200000,
  "updatedAt": 1750438800000
}
```

`200` response:
The timer object in its current server-side form.

## DELETE /api/v1/timers/{id}

Purpose:
Delete a timer.

`200` response:

```json
{
  "ok": true,
  "id": "884f8b44-f7c4-40e4-bd3f-76658c841e65"
}
```

## Client Behavior

- After any `PUT` or `DELETE`, the client should call `GET /api/v1/snapshot` again.
- In `snapshot.notes`, the server returns non-archived notes first and archived notes after them; inside each group, notes are sorted by `pinned` first, then by `updatedAt` descending.
- Clients use `revision` to determine whether the shared state has changed.
- The server does not enforce an offline queue. If the Android client needs offline mode later, the operation queue should live on the client side.
