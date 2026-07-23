# Architecture

## Separation

The project is intentionally split into independent parts:

- `shared`
  Contains shared DTOs, enums, JSON mappers, and the snapshot format.
- `server`
  The authoritative backend. It stores notes and timers, serves snapshots, and accepts changes through the HTTP API.
- `windows-client`
  A Windows 11 widget. It is not the source of truth for data; it only edits and displays server state.

## Data Flow

1. The Windows client fetches a `snapshot` from the server.
2. The user changes a note or timer.
3. The client sends a `PUT` or `DELETE` request to the server.
4. The server stores the change in PostgreSQL and increments `revision`.
5. The client requests `snapshot` again and refreshes the UI.

## Rationale

- A single source-of-truth server makes the Android client simpler.
- Tailscale solves network connectivity without exposing a public ingress.
- PostgreSQL is convenient for long-term storage, backups, and migrations.
- Redis remains in the separate infra compose stack as a foundation for background jobs, events, or future push mechanisms.

## Network Access

Clients are expected to connect to the server:

- by Tailscale IP, for example `http://100.x.y.z:8080`
- or by MagicDNS name, for example `http://notes-server.tailnet-name.ts.net:8080`

## Compose Layers

- `deploy/infra/docker-compose.yml`
  Starts the shared infrastructure: PostgreSQL, and the external network `notes-backend`.
- `deploy/server/docker-compose.yml`
  Starts only the application server and connects it to the existing external network `notes-backend`.
- `deploy/full/docker-compose.yml`
  Starts PostgreSQL, and the application server in one compose stack.
