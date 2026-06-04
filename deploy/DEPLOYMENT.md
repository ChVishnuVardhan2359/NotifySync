# NotifySync — Deployment Guide

This covers three deployment paths: **Docker Compose** (fastest), **IIS on Windows**, and **Azure**.

---

## Option A — Docker Compose (one command)

From the `deploy/` folder:

```bash
# optional: override secrets
export SA_PASSWORD='YourStrong!Passw0rd'
export JWT_KEY='a-very-long-random-production-signing-key-min-32-chars'

docker compose up -d --build
```

| Service | URL |
|---------|-----|
| Web dashboard | http://localhost:8081 |
| API + Swagger | http://localhost:8080/swagger |
| SQL Server | localhost:14333 |

The web container (nginx) reverse-proxies `/api` and `/hubs` to the API container, so the
production Angular build talks to a same-origin `/api` (see `environment.ts`).

Tear down: `docker compose down` (add `-v` to also drop the database volume).

---

## Option B — IIS on Windows Server

### 1. Backend API (ASP.NET Core 8)

1. Install the **.NET 8 Hosting Bundle** (gives IIS the ASP.NET Core Module v2).
2. Publish:
   ```powershell
   dotnet publish backend/src/NotifySync.Api/NotifySync.Api.csproj -c Release -o C:\inetpub\notifysync-api
   ```
3. In IIS Manager → **Add Website**:
   - Physical path: `C:\inetpub\notifysync-api`
   - Binding: `https`, port `8443` (bind a TLS cert)
   - App pool: **No Managed Code** (Kestrel runs out-of-process)
4. Set environment variables on the app pool (or `web.config` / `appsettings.Production.json`):
   - `ConnectionStrings__DefaultConnection`
   - `Jwt__Key`  (long random secret)
   - `Cors__AllowedOrigins__0 = https://your-web-host`
5. Apply the database schema: run `database/01_schema.sql`, **or** let the API auto-migrate on
   first start (it calls `Database.Migrate()`).

### 2. Web dashboard (Angular)

1. Build the production bundle:
   ```bash
   cd frontend/notifysync-web
   npm ci
   npm run build -- --configuration production
   ```
2. Copy `dist/notifysync-web/browser/*` to e.g. `C:\inetpub\notifysync-web`.
3. Create an IIS site pointing there. Install **URL Rewrite** and add a SPA fallback so deep
   links resolve to `index.html`:
   ```xml
   <rewrite>
     <rules>
       <rule name="SPA" stopProcessing="true">
         <match url=".*" />
         <conditions logicalGrouping="MatchAll">
           <add input="{REQUEST_FILENAME}" matchType="IsFile" negate="true" />
           <add input="{REQUEST_URI}" pattern="^/(api|hubs)/" negate="true" />
         </conditions>
         <action type="Rewrite" url="/index.html" />
       </rule>
     </rules>
   </rewrite>
   ```
4. Either host the API under the same domain at `/api` (recommended — `environment.ts` already
   uses a relative `/api`), or point `environment.ts` at the API host and rebuild.

---

## Option C — Azure

**Database** — Azure SQL Database. Copy the ADO.NET connection string into
`ConnectionStrings__DefaultConnection` (App Service → Configuration). Add the App Service
outbound IPs to the SQL firewall.

**API** — Azure App Service (Linux, .NET 8):
```bash
az webapp up --runtime "DOTNET:8.0" --name notifysync-api --resource-group notifysync-rg
```
Set app settings: `Jwt__Key`, `ConnectionStrings__DefaultConnection`, `Cors__AllowedOrigins__0`.
SignalR works on a single instance out of the box; to scale out add the **Azure SignalR Service**
and `.AddSignalR().AddAzureSignalR(connectionString)` in `Program.cs`.

**Web** — Azure Static Web Apps (point at `frontend/notifysync-web`, build `npm run build`,
output `dist/notifysync-web/browser`), or another App Service serving the static files.

**Mobile** — build the release APK/AAB (see `mobile/README.md`) and upload the `.aab` to the
Google Play Console.

---

## Production checklist

- [ ] Replace `Jwt:Key` with a long random secret (store in Key Vault / App settings, not in git)
- [ ] Use a strong `sa` password or a dedicated least-privilege SQL login
- [ ] Restrict `Cors:AllowedOrigins` to your real web origin(s)
- [ ] Terminate TLS (HTTPS) at IIS / App Service / a load balancer
- [ ] Point the Android app at your HTTPS API: `-PapiBaseUrl="https://api.yourdomain.com/"`
- [ ] Review rate-limit thresholds in `Program.cs` for your traffic
