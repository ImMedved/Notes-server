# Notes Server

Standalone backend project for notes and timers.

Notes support `pinned` and `archived` states. Archived notes stay in the snapshot and are sorted after active notes.

## Contents

- Java 17 HTTP server
- PostgreSQL persistence
- Dockerfile for app container
- Separate compose for infrastructure and app
- API and data contracts for future Android and desktop clients

## Build

```powershell
mvn -q -DskipTests package
```

## Run with Docker

All-in-one stack:

```powershell
cd deploy\full
Copy-Item .env.example .env
docker compose up -d --build
```

Infra:

```powershell
cd deploy\infra
docker compose up -d
```

App:

```powershell
cd ..\server
docker compose up -d --build
```

Health check:

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/health
```

## Docs

- [Architecture](../docs/architecture.md)
- [Deployment](../docs/deployment.md)
- [API contract](../docs/contracts/api.md)
- [Models](../docs/contracts/models.md)
- [Android client notes](../docs/android-client.md)
