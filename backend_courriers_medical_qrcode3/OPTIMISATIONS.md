# Optimisations — Backend & Frontend (système rapide et robuste)

## Backend (application.yml + Hikari)

### HikariCP (pool de connexions)
- **minimum-idle** : 5 — connexions prêtes, moins de latence au premier appel.
- **maximum-pool-size** : 20 — évite la surcharge DB tout en gardant du débit.
- **connection-timeout** : 20 s — délai max pour obtenir une connexion.
- **idle-timeout** / **max-lifetime** : recyclage des connexions pour éviter les timeouts MySQL.
- **connection-test-query** : `SELECT 1` — validation des connexions du pool.
- **leak-detection-threshold** : 60 s — alerte si une connexion n’est pas rendue (debug).

### MySQL (URL)
- **cachePrepStmts**, **useServerPrepStmts** : cache des requêtes préparées côté driver.
- **prepStmtCacheSize** / **prepStmtCacheSqlLimit** : limite du cache de requêtes préparées.

### JPA / Hibernate
- **batch_size** : 25 — regroupement des inserts/updates en batch.
- **order_inserts** / **order_updates** : true — meilleur usage du batch.
- **open-in-view** : false — évite de garder une session Hibernate ouverte pendant tout le rendu (recommandé en API).

### Server
- **compression.enabled** : true — réponses gzip (JSON, HTML, etc.) pour moins de latence réseau.
- **tomcat.connection-timeout** : 20 s — cohérent avec les requêtes longues (ex. PDF).

### Jackson
- **default-property-inclusion** : non_null — JSON plus léger.
- **write-dates-as-timestamps** : false — dates en ISO.

### Profil production
- Fichier **application-production.yml** : pool plus grand (30), pas de `show-sql`.  
- Activer avec : `spring.profiles.active=production`.

---

## Frontend (Vite + API)

### Build (vite.config.ts)
- **target** : es2020 — code moderne et plus compact.
- **minify** : esbuild — build rapide.
- **manualChunks** : séparation vendor / UI pour meilleur cache navigateur.
- **chunkFileNames** / **assetFileNames** : noms avec hash pour cache long terme.

### API (config/api.ts)
- **timeout** : 15 s.
- **retry** : 2 tentatives sur GET en cas d’erreur réseau ou 5xx.
- **intercepteur** : 401/403 → déconnexion et redirection login.

---

## Pistes supplémentaires (optionnel)

- **Backend** : Spring Cache (`@Cacheable`) sur les lectures fréquentes (ex. listes de référence).
- **Backend** : Redis pour cache distribué en multi-instances.
- **Frontend** : PWA (vite-plugin-pwa) déjà prévu — activer pour mode hors-ligne / installable.
- **Infra** : HTTP/2, CDN pour les assets statiques, mise en cache des réponses API (Cache-Control) sur les endpoints idempotents.
