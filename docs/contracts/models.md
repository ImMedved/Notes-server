# Data Models

## Note

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

Fields:

- `id` - UUID string created by the client.
- `title` - a short title.
- `content` - Markdown text.
- `pinned` - whether the note is pinned to the top of the list.
- `archived` - whether the note is archived.
- `createdAt` - Unix epoch in milliseconds.
- `updatedAt` - Unix epoch in milliseconds.

## TimerEntry

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

Fields:

- `mode` - `COUNTDOWN` or `STOPWATCH`.
- `durationMillis` - the total countdown duration; for a stopwatch, `31536000000` can be stored.
- `startedAt` - the current start time if the timer is running.
- `accumulatedMillis` - accumulated time before the current run.
- `running` - whether the timer is currently active.

## ServerSnapshot

```json
{
  "revision": 42,
  "serverTimeEpochMillis": 1750438800000,
  "notes": [],
  "timers": []
}
```

Fields:

- `revision` - a global monotonically increasing state revision.
- `serverTimeEpochMillis` - the current server time.
- `notes` - the full list of notes; non-archived notes come first, followed by archived notes.
- `timers` - the full list of timers.

## API Error

```json
{
  "error": {
    "code": "unauthorized",
    "message": "X-Notes-Api-Key is invalid or missing."
  }
}
```
