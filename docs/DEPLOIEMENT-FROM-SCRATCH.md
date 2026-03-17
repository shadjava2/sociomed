# Déploiement from scratch (serveur réinstallé)

Guide pour déployer SOCIO-MED / PEC sur un serveur neuf (Ubuntu/Debian).

---

## 1. Prérequis sur le serveur

```bash
# Mise à jour
sudo apt update && sudo apt upgrade -y

# Docker (ou Podman selon votre hébergeur)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
# Se déconnecter/reconnecter pour que le groupe soit pris en compte

# Docker Compose (si pas inclus avec Docker)
sudo apt install -y docker-compose-plugin
# ou : sudo apt install -y docker-compose

# Git
sudo apt install -y git
```

---

## 2. Cloner le projet

```bash
sudo mkdir -p /opt/sociomed
sudo chown $USER:$USER /opt/sociomed
cd /opt/sociomed
git clone https://github.com/shadjava2/sociomed.git .
# ou : copier le projet (zip, rsync, etc.) dans /opt/sociomed
```

---

## 3. Fichier .env

```bash
cd /opt/sociomed
cp .env.example .env
nano .env
```

Remplir **toutes** les variables (sans espace autour du `=`), par exemple :

```bash
# Domaine / URLs publiques (adapter à votre domaine ou IP)
DOMAIN=sociomed-senat.app
APP_PUBLIC_BASE_URL=https://sociomed-senat.app/api
APP_FRONT_BASE_URL=https://sociomed-senat.app

# MySQL (IP ou hostname du serveur MySQL, base et user déjà créés)
SPRING_DATASOURCE_URL=jdbc:mysql://VOTRE_IP_MYSQL:3306/senat_courriers_audiences?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=sociomeduser
SPRING_DATASOURCE_PASSWORD=VOTRE_MOT_DE_PASSE_MYSQL

# JWT (générer : openssl rand -base64 32)
JWT_SECRET=VOTRE_CLE_BASE64_32_OCTETS

# Optionnel — tunnel Cloudflare (si vous utilisez sociomed-senat.app via Cloudflare)
CLOUDFLARED_TUNNEL_TOKEN=eyJ...
```

Sauvegarder (Ctrl+O, Entrée, Ctrl+X).

---

## 4. Base de données MySQL

Sur le **serveur MySQL** (même machine ou distant), créer la base et l’utilisateur :

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS senat_courriers_audiences
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'sociomeduser'@'%' IDENTIFIED BY 'VOTRE_MOT_DE_PASSE';
GRANT ALL PRIVILEGES ON senat_courriers_audiences.* TO 'sociomeduser'@'%';
FLUSH PRIVILEGES;
EXIT;
```

Adapter le nom d’utilisateur et le mot de passe à ceux définis dans le `.env`.

---

## 5. Pare-feu (SSH + ports sociomed)

```bash
sudo ufw allow 22/tcp
sudo ufw allow 9080/tcp
sudo ufw allow 9443/tcp
sudo ufw enable
sudo ufw status
```

(Si besoin d’ouvrir aussi 80/443 pour un autre service sur le même serveur, les ajouter.)

---

## 6. Lancer l’application

```bash
cd /opt/sociomed
docker compose --profile tunnel up -d --build
```

Caddy est exposé sur les ports **9080** (HTTP) et **9443** (HTTPS) pour ne pas entrer en conflit avec un autre projet (ex. PACS sur 80/443). L’accès en production se fait via **https://sociomed-senat.app** (Cloudflare Tunnel → caddy:80 en interne).

Le premier build peut prendre plusieurs minutes (frontend + backend).

---

## 7. Vérifications

```bash
# Conteneurs
docker compose ps

# Backend (attendre 30–60 s que Spring Boot démarre)
docker logs pec-backend 2>&1 | tail -20
curl -s http://127.0.0.1:9080/api/health
# Attendu : ok

# Page d’accueil (Caddy sur 9080)
curl -sI http://127.0.0.1:9080
# Attendu : HTTP/1.1 200 ...
```

**Depuis votre PC** : ouvrir **https://sociomed-senat.app** (Cloudflare) ou `http://IP_DU_SERVEUR:9080` (IP = `curl -s ifconfig.me` sur le serveur).

---

## 8. Cloudflare (si vous utilisez le tunnel)

1. **Dashboard Cloudflare** → Zero Trust → Networks → Tunnels → SOCIO-MED.
2. **Published application routes** : pour `sociomed-senat.app` (path `*`), mettre **Service** = **`http://caddy:80`** (pas `http://pec-frontend:80`).
3. Vérifier que **CLOUDFLARED_TUNNEL_TOKEN** est bien dans `/opt/sociomed/.env` (copié depuis le tunnel dans Cloudflare).
4. Redémarrer le tunnel si besoin :  
   `docker compose --profile tunnel up -d --force-recreate cloudflared`  
   Puis : `docker logs pec-cloudflared 2>&1 | tail -20`.

---

## 9. En cas de problème

| Symptôme | À faire |
|----------|--------|
| Port 80 already in use | `sudo ss -tlnp \| grep :80` puis arrêter le service qui utilise le 80 ou `docker compose down` et relancer. |
| Backend ne démarre pas (AccessDeniedException /app/annexes_upload) | Rebuild backend : `docker compose build backend --no-cache && docker compose up -d backend`. |
| 502 Bad Gateway | Vérifier les logs backend : `docker logs pec-backend 2>&1 \| tail -50`. Vérifier MySQL et `.env`. |
| Tunnel cloudflared "no valid argument" | Vérifier que `CLOUDFLARED_TUNNEL_TOKEN` est dans `.env`, une seule ligne, sans guillemets. |
| Accès par IP en timeout | Vérifier ufw (ports 80/443) et pare-feu / groupe de sécurité chez l’hébergeur. |

---

## 10. Mises à jour (après un git pull)

```bash
cd /opt/sociomed
git pull
docker compose build --no-cache
docker compose up -d
# Si tunnel : docker compose --profile tunnel up -d
```

Ou utiliser le script avec rollback :  
`./scripts/deploy-with-rollback.sh`
