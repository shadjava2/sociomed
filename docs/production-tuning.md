# Réglages production — Cloud VPS 50 (16 vCPU / 64 Go RAM)

Ce document décrit les optimisations pour un serveur **16 cœurs / 64 Go RAM** (ex. Cloud VPS 50), pour une exécution stable et rapide.

## Répartition mémoire (64 Go)

| Composant   | Limite / usage      | Rôle                          |
|------------|----------------------|-------------------------------|
| Backend JVM| 24 Go heap (28 Go max)| Spring Boot, PDF, API         |
| Redis      | 1 Go (maxmemory)     | Session / cache               |
| Caddy      | ~100 Mo             | Reverse proxy                 |
| Frontend   | ~50 Mo              | Nginx statique                |
| OS + autres| ~4–6 Go             | Système, buffers, MySQL si présent |

Si **MySQL est sur le même serveur**, réduire le heap backend (ex. `JAVA_OPTS=-Xms2g -Xmx16g ...`) et laisser 8–16 Go à MySQL.

## Backend (Spring Boot)

- **Profil** : `spring.profiles.active=production` (déjà utilisé dans le compose).
- **JVM** (défaut dans le compose, modifiable via `.env`) :
  - `-Xms2g -Xmx24g` : heap 24 Go
  - `-XX:+UseG1GC` : garbage collector adapté aux gros heaps
  - `-XX:MaxGCPauseMillis=200` : objectif de pause courte
  - `-XX:+UseStringDeduplication` : moins de mémoire pour les chaînes
- **HikariCP** (dans `application-production.yml`) :
  - `maximum-pool-size: 80`, `minimum-idle: 25`
- **Tomcat** :
  - `max threads: 400`, `min-spare: 50`, `max-connections: 10000`

Pour ajuster le heap sans toucher au code, dans `.env` :

```bash
# Exemple : réduire à 16 Go si MySQL est sur la même machine
JAVA_OPTS=-Xms2g -Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom
```

## Redis

- **maxmemory 1gb** : évite une consommation excessive.
- **maxmemory-policy allkeys-lru** : éviction LRU si la mémoire est pleine.
- **appendonly yes** : persistance activée.

## Caddy

Aucun réglage particulier. Les timeouts par défaut conviennent ; les requêtes longues (ex. génération PDF) sont gérées par le backend.

## Docker Compose

- **Backend** : `mem_limit: 28g`, `mem_reservation: 1g`.
- **Redis** : `mem_limit: 1536m`.

Cela évite qu’un conteneur consomme toute la RAM au détriment des autres.

## Vérifications rapides

```bash
# Mémoire utilisée par les conteneurs
docker stats --no-stream

# Santé backend
curl -s http://127.0.0.1/api/health

# Redis
docker exec pec-redis redis-cli INFO memory
```

## En cas de charge très forte

- Augmenter `server.tomcat.threads.max` (ex. 500) dans `application-production.yml` si la CPU reste sous-utilisée.
- Augmenter `spring.datasource.hikari.maximum-pool-size` si la base devient le goulot (en restant cohérent avec les connexions max MySQL).
- Garder `-Xmx` en dessous de la RAM disponible moins (Redis + MySQL + OS).
