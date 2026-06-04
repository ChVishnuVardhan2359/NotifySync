# NotifySync — Kubernetes deployment

Images (Docker Hub, public):
- API: `docker.io/vishnuvardhan2359/notifysync-api:latest`
- Web: `docker.io/vishnuvardhan2359/notifysync-web:latest`

Domains:
- Dashboard: `https://fs-notify.platform.kb.snovasys.com`
- API:       `https://fs-notify.api.platform.kb.snovasys.com`

## Prerequisites on the cluster
- An **NGINX ingress controller** installed (the ingress uses `ingressClassName: nginx`).
- DNS A/CNAME records for both hosts pointing at your ingress load balancer.
- TLS certs for both hosts (a wildcard `*.platform.kb.snovasys.com` works for both).
- Network path from the cluster to your PostgreSQL server (e.g. `192.168.61.6:5432`).

## Deploy steps

```bash
cd deploy/k8s

# 1. Namespace
kubectl apply -f 00-namespace.yaml

# 2. App secret (DB connection + JWT key). Edit Jwt__Key first!
kubectl apply -f 01-secret.yaml

# 3. TLS secrets — from your cert files (wildcard cert can be reused for both):
kubectl create secret tls notifysync-web-tls \
  --cert=fullchain.pem --key=privkey.pem -n notifysync
kubectl create secret tls notifysync-api-tls \
  --cert=fullchain.pem --key=privkey.pem -n notifysync
#   (or edit & apply 05-tls-secret.example.yaml)

# 4. Workloads + ingress
kubectl apply -f 02-api.yaml
kubectl apply -f 03-web.yaml
kubectl apply -f 04-ingress.yaml

# 5. Watch it come up
kubectl -n notifysync get pods,svc,ingress
kubectl -n notifysync logs deploy/notifysync-api -f
```

The API **creates the database and schema automatically** on first start (EF Core
`Database.Migrate()`), using the connection string in the secret. The SQL login
must be allowed to create the database `Snovasys.NotifySync.Platform` (or create it
beforehand and grant the login db_owner on it).

## Notes
- **CORS**: the API allows the dashboard origin via the `Cors__AllowedOrigins__0`
  env var in `02-api.yaml`. If you change the dashboard domain, update it there.
- **API base URL** is compiled into the web image (`environment.ts` →
  `https://fs-notify.api.platform.kb.snovasys.com`). If the API domain changes,
  rebuild the web image.
- **Scaling**: SignalR is single-instance out of the box. To run >1 API replica,
  add the Azure SignalR Service (or a Redis backplane) — otherwise keep `replicas: 1`.
- **Android app**: point it at `https://fs-notify.api.platform.kb.snovasys.com/`
  in the app's “Server address” field. The APK is downloadable from
  `https://fs-notify.api.platform.kb.snovasys.com/api/app/download`.

## Updating an image later
```bash
docker build -f ../api.Dockerfile -t vishnuvardhan2359/notifysync-api:latest ../../backend
docker push vishnuvardhan2359/notifysync-api:latest
kubectl -n notifysync rollout restart deploy/notifysync-api
```
