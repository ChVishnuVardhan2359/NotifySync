# NotifySync

A notification synchronization platform: an **Android app** captures notifications from selected
apps and uploads them to an **ASP.NET Core API**, and an **Angular dashboard** displays them in
**real time** via SignalR.

```
  📱 Android (Kotlin/Compose)            💻 Angular dashboard
     captures notifications                 live grid + stats
            │  POST /api/notifications          ▲  SignalR push
            ▼                                    │
        🌐 ASP.NET Core 8 Web API  ──────────────┘
            │  EF Core
            ▼
        🗄️ SQL Server
```

## Tech stack

| Layer | Stack |
|-------|-------|
| Backend | ASP.NET Core 8, EF Core, SQL Server, SignalR, JWT, FluentValidation, Serilog, Swagger |
| Web | Angular 18, Angular Material, RxJS, SignalR client, dark mode, CSV/Excel export |
| Mobile | Kotlin, Jetpack Compose, NotificationListenerService, Room, WorkManager, Retrofit |

## Repository layout

```
NotifySync/
├── backend/          ASP.NET Core solution (Domain / Application / Infrastructure / Api)
├── frontend/         Angular dashboard (notifysync-web)
├── mobile/           Android app (NotifySyncApp) + build guide
├── database/         SQL schema script
├── deploy/           Dockerfiles, docker-compose, DEPLOYMENT.md (IIS + Azure)
└── NotifySync-v1.0.0-debug.apk   ← installable Android app
```

## Run it locally

### 1. Database (Docker)
```bash
docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=NotifySync!Pass123" \
  -p 14333:1433 --name notifysync-sql -d mcr.microsoft.com/mssql/server:2022-latest
```

### 2. Backend API
```bash
cd backend
dotnet run --project src/NotifySync.Api        # http://localhost:5080  (Swagger at /swagger)
```
The API auto-applies EF migrations on startup and creates the `NotifySync` database.

### 3. Web dashboard
```bash
cd frontend/notifysync-web
npm install
ng serve                                        # http://localhost:4200
```
Register an account, then explore Dashboard / Notifications / Devices / Settings.

### 4. Android app
Install `NotifySync-v1.0.0-debug.apk` on a device/emulator, or build from `mobile/` — see
[mobile/README.md](mobile/README.md). Point it at your API with
`-PapiBaseUrl="http://10.0.2.2:5080/"` (emulator) or your LAN IP (physical device).

## API surface

| Method | Route | Purpose |
|--------|-------|---------|
| POST | `/api/auth/register`, `/api/auth/login` | Auth → JWT |
| POST | `/api/device/register`, `/api/device/heartbeat` | Device lifecycle |
| GET  | `/api/device` | List devices (online/offline) |
| POST | `/api/notifications` | Ingest a notification (from the phone) |
| GET  | `/api/notifications?page=&pageSize=` | Paged list |
| GET  | `/api/notifications/search?query=` | Search |
| DELETE | `/api/notifications/{id}` | Delete |
| GET  | `/api/dashboard/stats` | Totals, today, active devices, top apps |
| GET/PUT | `/api/settings/profile`, `/password`, `/notifications` | Profile & prefs |
| WS | `/hubs/notifications` | SignalR real-time push (`ReceiveNotification`) |

All `/api/*` routes except auth require `Authorization: Bearer <token>`.

## Deployment

See [deploy/DEPLOYMENT.md](deploy/DEPLOYMENT.md) — Docker Compose, IIS, and Azure paths.

## Notes

- Each user's data is isolated; the `userId` is taken from the JWT, never from the request body.
- Notifications are pushed only to the owning user's SignalR group.
- The Android app captures **only** apps the user explicitly selects, and only while sync is on.
