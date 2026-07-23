# Deployment

### With One Compose File

Files:

- [deploy/full/docker-compose.yml](../deploy/full/docker-compose.yml)
- [deploy/full/.env.example](../deploy/full/.env)

Start it:

```bash
cd deploy/full
docker compose up -d --build
```

This starts:

- PostgreSQL
- the application server

### Infrastructure Separately

The infra compose stack is separate and starts the shared services.

Files:

- [deploy/infra/docker-compose.yml](../deploy/infra/docker-compose.yml)
- [deploy/infra/.env.example](../deploy/infra/.env.example)

```bash
cd deploy/infra
cp .env.example .env
docker compose up -d
```

This creates:

- PostgreSQL
- the external Docker network `notes-backend`

### Application Server

Files:

- [deploy/server/docker-compose.yml](../deploy/server/docker-compose.yml)
- [deploy/server/.env.example](../deploy/server/.env.example)
- [server/Dockerfile](../Dockerfile)

```bash
cd deploy/server
cp .env.example .env
docker compose up -d --build
```

### Tailscale

On the server:

```bash
sudo tailscale up
tailscale ip -4
```

Then configure the Windows client with:

- `http://<tailscale-ip>:8080`
- or `http://<magicdns-name>:8080`

### Windows App Image

Clone it:

```powershell
git clone https://github.com/ImMedved/Notes-desktop
```

Build it:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-exe.ps1
```

## Local Smoke Test on localhost

If you want to quickly check everything on one PC without Tailscale:

1. Start the infra compose stack.
2. Start the server compose stack with `NOTES_PORT=8080`.
3. Run `NotesWidgetClient.exe`.
4. On the `Sync` tab, keep `http://127.0.0.1:8080`.
5. If `NOTES_API_KEY` is set in `deploy/server/.env`, enter it in the `API key` field.

## 6. Backups

At minimum, back up:

- the PostgreSQL volume from the infra compose stack
- the server `.env` file