# Déploiement sociomed sur cmkpacs (serveur partagé avec PACS)

Sur ce serveur, le **PACS** utilise déjà les ports **80** et **443** (Caddy ohif-proxy). Le **docker-compose.yml** principal expose Caddy sociomed sur **9080** et **9443** pour éviter tout conflit.

---

## 1. Ports

| Projet | Ports hôte |
|--------|------------|
| PACS (Caddy, Orthanc, etc.) | 80, 443, 4242, 8042, 127.0.0.1:8080, 3010 |
| **sociomed** (Caddy) | **9080** (HTTP), **9443** (HTTPS) |

Accès en production : **https://sociomed-senat.app** via Cloudflare Tunnel (cloudflared → caddy:80 en interne).

---

## 2. Lancer sociomed

```bash
cd /opt/sociomed
cp .env.example .env
nano .env   # Remplir DOMAIN, APP_*_URL, SPRING_DATASOURCE_*, JWT_SECRET, CLOUDFLARED_TUNNEL_TOKEN
```

Créer la base MySQL `senat_courriers_audiences` (voir `DEPLOIEMENT-FROM-SCRATCH.md`), puis :

```bash
docker compose --profile tunnel up -d --build
```

---

## 3. Cloudflare

- **Published application routes** : pour **sociomed-senat.app** (path `*`), **Service** = **`http://caddy:80`**.
- Le token du tunnel doit être dans `.env` : `CLOUDFLARED_TUNNEL_TOKEN=eyJ...`

---

## 4. Vérifications

```bash
docker compose ps
docker logs pec-cloudflared 2>&1 | tail -15
curl -sI http://127.0.0.1:9080
```

Puis ouvrir **https://sociomed-senat.app** dans le navigateur.
