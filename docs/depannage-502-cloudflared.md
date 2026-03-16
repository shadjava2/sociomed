# Dépannage 502 et cloudflared

## 502 Bad Gateway — backend "Connection refused"

Caddy joint le **frontend** (OK) mais pas le **backend** (port 8085). Causes fréquentes :

### 1. Backend pas encore démarré (Spring Boot lent)

Attendre 30–60 s après `docker compose up`, puis :

```bash
docker logs pec-backend 2>&1 | tail -40
```

Si vous voyez "Started ... Application" à la fin, le backend écoute. Retester :

```bash
curl -s http://127.0.0.1/api/health
```

### 2. Backend en erreur (MySQL, config)

Si les logs montrent une exception (connexion MySQL, etc.) :

- Vérifier que la base `senat_courriers_audiences` existe sur le serveur MySQL.
- Vérifier `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` dans `.env`.

### 3. Vérifier que le backend écoute sur 8085

```bash
docker exec pec-backend sh -c "nc -z 127.0.0.1 8085 && echo OK || echo FAIL"
```

Si FAIL, le backend n’écoute pas (crash ou pas encore prêt).

---

## Cloudflared — "You did not specify any valid additional argument"

Le conteneur ne reçoit pas le token. À vérifier :

### 1. Token présent dans `.env`

```bash
grep CLOUDFLARED_TUNNEL_TOKEN /opt/sociomed/.env
```

- Une ligne du type `CLOUDFLARED_TUNNEL_TOKEN=eyJ...` doit apparaître.
- Pas d’espace autour du `=`, pas de guillemets autour de la valeur, token sur une seule ligne.

### 2. Token bien passé au conteneur

Quand **pec-cloudflared** est en état **Up** (pas en Restarting) :

```bash
docker exec pec-cloudflared sh -c 'echo "TUNNEL_TOKEN length: ${#TUNNEL_TOKEN}"'
```

Si la longueur est 0, la variable n’est pas définie dans le conteneur. Vérifier que vous lancez bien depuis le répertoire du projet :

```bash
cd /opt/sociomed
docker compose --profile tunnel config | grep -A2 cloudflared
```

Regarder si `TUNNEL_TOKEN` apparaît avec une valeur (ou une valeur masquée).

### 3. Passer le token en ligne de commande (test)

Pour vérifier que le token fonctionne, lancer cloudflared à la main une fois (remplacer `VOTRE_TOKEN` par la valeur de `.env`) :

```bash
docker run --rm -e TUNNEL_TOKEN="VOTRE_TOKEN" docker.io/cloudflare/cloudflared:latest tunnel --no-autoupdate run --token "$TUNNEL_TOKEN"
```

Si la connexion au tunnel s’établit, le token est bon et le souci vient du passage via compose.
