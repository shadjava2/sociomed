# Déploiement en production (GitHub → serveur → Docker)

## Dev local (avant de pousser)

Les secrets ne sont plus dans `application.yml`. En local, créez un fichier **non versionné** :

```bash
cd src/main/resources
cp application-local.yml.example application-local.yml
# Éditer application-local.yml : URL MySQL, username, password, jwt.secret
```

Puis lancez l’app comme d’habitude (les valeurs de `application-local.yml` surchargent le reste).

---

## 1. Pousser le code sur GitHub

Sur votre machine (dans le dossier du projet, ex. `PROJETS` ou `backend_courriers_medical_qrcode3`) :

```bash
# Si le dépôt Git est à la racine PROJETS (backend + frontend)
cd C:\Users\HP\Videos\PROJETS
git add .
git status
git commit -m "Deploy: backend PEC + Docker production"
git remote add origin https://github.com/VOTRE_ORG/VOTRE_REPO.git
git push -u origin main
```

Si le dépôt ne contient que le backend :

```bash
cd C:\Users\HP\Videos\PROJETS\backend_courriers_medical_qrcode3
git init
git add .
git commit -m "Deploy: backend PEC + Docker production"
git remote add origin https://github.com/VOTRE_ORG/VOTRE_REPO.git
git branch -M main
git push -u origin main
```

**Important :** Ne jamais committer de mots de passe ou clés API. En prod tout passe par les variables d'environnement (profil `production`). En local, utilisez `application-local.yml` (voir `application-local.yml.example`).

---

## 2. Sur le serveur : récupérer le code et déployer Docker

Se connecter au serveur (SSH), puis :

```bash
# Aller dans le dossier du projet (ou le cloner si première fois)
cd /chemin/vers/projet
git pull origin main
```

Si première fois (clone) :

```bash
git clone https://github.com/VOTRE_ORG/VOTRE_REPO.git pec-backend
cd pec-backend
# Si le repo contient tout, aller dans le backend
cd backend_courriers_medical_qrcode3
```

Créer le fichier d'environnement (une seule fois) :

```bash
cp .env.production.example .env.production
nano .env.production   # ou vim : remplir APP_PUBLIC_BASE_URL, APP_FRONT_BASE_URL, SPRING_DATASOURCE_*, JWT_SECRET
```

Lancer le déploiement Docker :

```bash
docker compose --env-file .env.production build --no-cache
docker compose --env-file .env.production up -d
```

Vérifier les logs :

```bash
docker compose logs -f backend
```

Vérifier que l’app répond (si actuator est activé) :

```bash
curl -s http://localhost:8085/actuator/health
```

---

## 3. Mises à jour ultérieures

Sur le serveur, après chaque `git pull` :

```bash
cd /chemin/vers/backend_courriers_medical_qrcode3
git pull origin main
docker compose --env-file .env.production build --no-cache
docker compose --env-file .env.production up -d
```

Ou en une ligne :

```bash
git pull origin main && docker compose --env-file .env.production build --no-cache && docker compose --env-file .env.production up -d
```

---

## 4. Prérequis serveur

- Docker et Docker Compose v2 installés
- MySQL accessible (même machine ou distant) ; base `senat_courriers_audiences` créée
- Port 8085 libre (ou mapper un autre port dans `docker-compose.yml`)
- Fichier `.env.production` rempli (URLs, DB, JWT_SECRET)

---

## 5. Profil Spring

Le conteneur utilise le profil **production** (`SPRING_PROFILES_ACTIVE=production`). La config est dans `application-production.yml` (URLs, pool Hikari, etc.). Les secrets viennent uniquement des variables d’environnement.
