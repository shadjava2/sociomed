# Déploiement — Git, Docker, Redis, Caddy, Cloudflare

## Prérequis

- Serveur (VPS) avec Docker et Docker Compose
- Domaine pointé vers Cloudflare (ex : `medical.senat.cd`)
- Base MySQL accessible depuis le serveur (locale ou distante)

## 1. Git

```bash
# Sur votre machine (première fois)
cd /chemin/vers/PROJETS
git init
git add .
git commit -m "Initial: app + Docker + Caddy + Redis"

# Créer un dépôt distant (GitHub, GitLab, etc.) puis :
git remote add origin https://github.com/votre-org/pec-medical.git
git push -u origin main
```

Sur le serveur :

```bash
git clone https://github.com/votre-org/pec-medical.git
cd pec-medical
```

## 2. Variables d'environnement

```bash
cp .env.example .env
# Éditer .env : DOMAIN, URLs, MySQL, JWT_SECRET
nano .env
```

- **DOMAIN** : domaine public (ex. `medical.senat.cd`).
- **APP_PUBLIC_BASE_URL** : URL de l’API (ex. `https://medical.senat.cd/api`).
- **APP_FRONT_BASE_URL** : URL du front (ex. `https://medical.senat.cd`).
- **SPRING_DATASOURCE_*** : accès à la base MySQL.
- **JWT_SECRET** : `openssl rand -base64 32`.

## 3. MySQL

Soit une base déjà hébergée, soit un conteneur à part :

```yaml
# À ajouter dans docker-compose.yml si besoin
  mysql:
    image: mysql:8
    environment:
      MYSQL_DATABASE: senat_courriers_audiences
      MYSQL_USER: pec
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - pec-net
```

Puis dans `.env` :  
`SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/senat_courriers_audiences?...`

## 4. Lancement Docker

```bash
docker compose up -d --build
```

Vérifications :

- Frontend : `http://IP_SERVEUR` (ou le domaine une fois DNS configuré).
- API : `http://IP_SERVEUR/api/...`.
- Redis : utilisé en interne par l’app (optionnel selon votre code).

## 5. Cloudflare

1. **DNS**  
   Dans Cloudflare → DNS → Enregistrements :
   - Type **A** (ou **AAAA**), nom `medical` (ou `@` pour la racine), valeur = **IP publique du serveur**, proxy activé (nuage orange).

2. **SSL/TLS**  
   - **Mode** : « Full » ou « Full (strict) ».
   - Si **Full (strict)** : sur le serveur, Caddy doit avoir un certificat (Let’s Encrypt ou certificat d’origine Cloudflare). Soit vous activez HTTPS dans le Caddyfile (voir ci‑dessous), soit vous installez le certificat d’origine Cloudflare sur Caddy.

3. **Real IP**  
   Le `Caddyfile` fourni utilise déjà l’en-tête `CF-Connecting-IP` pour l’adresse réelle du client.

### Caddy en HTTPS (Let’s Encrypt)

Pour que Caddy gère le HTTPS sur le serveur (recommandé avec Cloudflare « Full » ou « Full (strict) ») :

1. Dans le `Caddyfile`, remplacer le bloc `:80` par un bloc avec votre domaine et laisser Caddy obtenir le certificat :

```caddy
{$DOMAIN} {
    realip { header CF-Connecting-IP }
    handle_path /api/* {
        reverse_proxy backend:8085
    }
    handle {
        reverse_proxy frontend:80
    }
}
```

2. Ouvrir les ports 80 et 443 vers le serveur (pour Let’s Encrypt et le trafic).

3. Redémarrer Caddy :  
   `docker compose up -d caddy`

## 6. Mises à jour

```bash
git pull
docker compose up -d --build
```

## Résumé des services

| Service   | Rôle                          | Port exposé |
|----------|-------------------------------|-------------|
| Caddy    | Reverse proxy, TLS (optionnel) | 80, 443     |
| Frontend | SPA (Vite build)              | interne     |
| Backend  | API Spring Boot               | interne     |
| Redis    | Cache / session (optionnel)   | interne     |
