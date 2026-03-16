# Configuration Cloudflare Tunnel pour SOCIO-MED / PEC

Ce guide permet d’exposer l’application via un tunnel Cloudflare (cloudflared) et de faciliter le déploiement.

## 1. Route publiée : pointer vers Caddy (obligatoire)

Actuellement, si la route pointe vers **`http://frontend:80`**, les appels à **`/api/*`** n’atteignent jamais le backend.

**À faire dans le tableau de bord Cloudflare :**

1. **Zero Trust** → **Networks** → **Tunnels** → onglet **Published application routes**.
2. Pour la route **sociomed-senat.app** (path `*`) :
   - Cliquez sur le menu (⋮) → **Edit**.
   - **Service** : remplacez `http://frontend:80` par **`http://caddy:80`**.
   - Enregistrez.

Ainsi, tout le trafic (y compris `/api/*`) passe par **Caddy**, qui envoie :
- `/api/*` → backend (Spring Boot)
- le reste → frontend (SPA).

Résumé :

| Avant (incorrect) | Après (correct)     |
|-------------------|----------------------|
| Service: `http://frontend:80` | Service: **`http://caddy:80`** |

## 2. Token du tunnel

Le connecteur cloudflared utilise un **token** fourni par Cloudflare (créé lors de la configuration du tunnel).

- **Ne commitez jamais le token** (il est dans `.env`, déjà dans `.gitignore`).
- Dans `.env` à la racine du projet, ajoutez :

```bash
# Token du tunnel Cloudflare (Zero Trust > Networks > Tunnels > votre tunnel)
CLOUDFLARED_TUNNEL_TOKEN=eyJhIjoi...
```

## 3. Lancer le stack avec le tunnel

Avec le service **cloudflared** dans le `docker-compose` (profile `tunnel`) :

```bash
# Tout le stack + tunnel (backend, frontend, Caddy, Redis, cloudflared)
docker compose --profile tunnel up -d
```

Sans le tunnel (ex. accès direct par IP/port) :

```bash
docker compose up -d
```

## 4. Déploiement avec le script et tunnel

Pour déployer **et** redémarrer le tunnel après mise à jour :

```bash
./scripts/deploy-with-rollback.sh
# Puis si vous utilisez le tunnel :
docker compose --profile tunnel up -d
```

Ou utilisez la variable pour que le script relance aussi cloudflared (voir section 5).

## 5. Variables utiles

| Variable | Description |
|----------|-------------|
| `CLOUDFLARED_TUNNEL_TOKEN` | Token du tunnel (obligatoire pour `--profile tunnel`) |
| `DOMAIN` | Domaine (ex. `sociomed-senat.app`) pour Caddy / app |
| `APP_FRONT_BASE_URL` | URL publique du front (ex. `https://sociomed-senat.app`) |
| `APP_PUBLIC_BASE_URL` | URL publique de l’API (ex. `https://sociomed-senat.app/api`) |

## 6. Vérifications

- **Front** : ouvrir `https://sociomed-senat.app` → la SPA s’affiche.
- **API** : `curl -s https://sociomed-senat.app/api/health` → doit renvoyer `ok`.
- **Tunnel** : le conteneur `pec-cloudflared` doit être en état **Up** (`docker compose ps`).

## 7. Dépannage

- **404 ou API inaccessible** : vérifier que la route publiée utilise bien **`http://caddy:80`** et non `http://frontend:80`.
- **Tunnel ne démarre pas** : vérifier que `CLOUDFLARED_TUNNEL_TOKEN` est défini dans `.env` et que le token est valide dans Cloudflare.
- **Caddy / backend** : les conteneurs `pec-caddy` et `pec-backend` doivent être sur le même réseau que `pec-cloudflared` (réseau `pec-net`).
