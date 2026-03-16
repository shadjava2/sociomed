# Déploiement sur le serveur (VPS)

## 1. Créer la base de données MySQL

**Spring Boot ne crée pas la base** : il crée les tables (avec `ddl-auto: update`) mais la **base (schema) doit exister** sur le serveur MySQL.

Sur le serveur MySQL (ex. `164.68.101.130` ou en local), connectez-vous et exécutez :

```sql
CREATE DATABASE IF NOT EXISTS senat_courriers_audiences
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Si l'utilisateur n'existe pas (adapter le nom/mot de passe selon votre .env) :
-- CREATE USER 'sociomeduser'@'%' IDENTIFIED BY 'VOTRE_MOT_DE_PASSE';
-- GRANT ALL PRIVILEGES ON senat_courriers_audiences.* TO 'sociomeduser'@'%';
-- FLUSH PRIVILEGES;
```

Puis vérifiez que dans votre `.env` vous avez bien :

- `SPRING_DATASOURCE_URL=jdbc:mysql://IP_MYSQL:3306/senat_courriers_audiences?useSSL=...`
- `SPRING_DATASOURCE_USERNAME=...`
- `SPRING_DATASOURCE_PASSWORD=...`

---

## 2. Accès par IP (pourquoi « même avec l’IP ça marche pas »)

- **164.68.101.130** dans votre config est l’**IP du serveur MySQL** (base de données), pas celle du serveur où tourne Docker/Caddy.
- Pour ouvrir l’application dans le navigateur, il faut utiliser l’**IP du VPS où Docker tourne** (ex. `vmi3151044` = une autre IP publique).

À faire :

1. **Trouver l’IP publique du VPS** (là où vous faites `docker compose up`) :
   ```bash
   curl -s ifconfig.me
   ```
   ou regarder dans le panel de votre hébergeur (OVH, etc.).

2. **Ouvrir le port 80** sur ce VPS (pare-feu) :
   ```bash
   # Si vous utilisez ufw (Ubuntu/Debian) :
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   sudo ufw status
   sudo ufw reload
   ```

3. **Tester** : dans le navigateur, allez sur `http://IP_DU_VPS` (pas 164.68.101.130 si c’est le MySQL).

---

## 3. Récap

| Problème | Solution |
|----------|----------|
| Backend ne crée pas la base | Créer la base `senat_courriers_audiences` (et l’utilisateur) sur MySQL (voir §1). |
| Accès par IP en timeout | Utiliser l’IP du **VPS Docker**, pas l’IP MySQL ; ouvrir le port 80 (voir §2). |
| Tunnel Cloudflare 1033 / token non reçu | Vérifier que `CLOUDFLARED_TUNNEL_TOKEN` est bien dans `.env` et que la route pointe vers `http://caddy:80`. |
