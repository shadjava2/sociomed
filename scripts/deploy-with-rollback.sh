#!/usr/bin/env bash
# Déploiement PEC (sociomed) avec rollback automatique — sans arrêter le service
#
# Usage:
#   ./scripts/deploy-with-rollback.sh
#   ./scripts/deploy-with-rollback.sh --prod
#
# Points clés:
# - Charge toujours .env à la racine du projet
# - Sauvegarde les images actuelles (pec-backend:latest, pec-frontend:latest) en :rollback
# - Build avec retries ; en cas d'échec → rollback (retag :rollback → :latest + compose up -d)
# - Health check sur le backend via Caddy (/api/health) ; si KO → rollback
# - Pas d’arrêt du service : build d’abord, puis compose up -d (bascule courte)
# - Log optionnel dans ./logs ou chemin défini par DEPLOY_LOG

set -Eeo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$REPO_ROOT"

MODE="${1:-}"
if [ "$MODE" = "--prod" ]; then
  COMPOSE_FILE="${COMPOSE_FILE:-$REPO_ROOT/docker-compose.prod.yml}"
  COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-pec-prod}"
else
  COMPOSE_FILE="${COMPOSE_FILE:-$REPO_ROOT/docker-compose.yml}"
  COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-pec}"
fi

# Si pas de fichier prod, utiliser le compose par défaut
if [ ! -f "$COMPOSE_FILE" ]; then
  COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"
fi

ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
ROLLBACK_FILE="$REPO_ROOT/.deploy-rollback"
CURRENT_FILE="$REPO_ROOT/.deploy-current"

# Health via Caddy (port 80) → /api/* → backend:8085
BACKEND_HEALTH_URL="${BACKEND_HEALTH_URL:-http://127.0.0.1/api/health}"

BUILD_MAX_ATTEMPTS="${BUILD_MAX_ATTEMPTS:-3}"
BUILD_RETRY_DELAY="${BUILD_RETRY_DELAY:-15}"
HEALTH_MAX_ATTEMPTS="${HEALTH_MAX_ATTEMPTS:-36}"
HEALTH_RETRY_DELAY="${HEALTH_RETRY_DELAY:-5}"
BUILD_NO_CACHE="${BUILD_NO_CACHE:-0}"

export PEC_REPO_ROOT="$REPO_ROOT"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
  chmod 600 "$ENV_FILE" 2>/dev/null || true
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_BIN="docker compose"
else
  COMPOSE_BIN="docker-compose"
fi

compose() {
  $COMPOSE_BIN \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    -p "$COMPOSE_PROJECT_NAME" \
    --project-directory "$REPO_ROOT" \
    "$@"
}

log_line() {
  echo "$@"
  if [ -n "${DEPLOY_LOG:-}" ]; then
    if [ "$DEPLOY_LOG" = "1" ]; then
      mkdir -p "$REPO_ROOT/logs"
      echo "$(date -Iseconds) $*" >> "$REPO_ROOT/logs/deploy-$(date +%Y%m%d).log"
    else
      mkdir -p "$(dirname "$DEPLOY_LOG")" 2>/dev/null || true
      echo "$(date -Iseconds) $*" >> "$DEPLOY_LOG"
    fi
  fi
}

# Sauvegarder les images actuelles pour rollback
save_rollback_images() {
  for img in pec-backend:latest pec-frontend:latest; do
    if docker image inspect "$img" >/dev/null 2>&1; then
      docker tag "$img" "${img%:latest}:rollback"
      log_line "[DEPLOY] Sauvegarde image rollback: ${img%:latest}:rollback"
    fi
  done
}

# Restaurer les images rollback et relancer
rollback() {
  set +e
  log_line "[DEPLOY] ROLLBACK -> restauration des images :rollback"

  for img in pec-backend pec-frontend; do
    if docker image inspect "${img}:rollback" >/dev/null 2>&1; then
      docker tag "${img}:rollback" "${img}:latest"
      log_line "[DEPLOY] Restaure ${img}:latest depuis :rollback"
    fi
  done

  compose up -d || true
  log_line "[DEPLOY] Rollback terminé"
  log_line "[DEPLOY] État courant:"
  compose ps || true
  exit 1
}

trap 'log_line "[DEPLOY] Erreur détectée -> rollback"; rollback' ERR

log_line "[DEPLOY] Compose: ${COMPOSE_FILE#$REPO_ROOT/}"
log_line "[DEPLOY] Sauvegarde des images actuelles pour rollback..."
save_rollback_images

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  GIT_SHA="$(git rev-parse --short HEAD)"
else
  GIT_SHA="manual"
fi
NEW_TAG="deploy-${GIT_SHA}-$(date +%Y%m%d%H%M)"
echo "$NEW_TAG" > "$ROLLBACK_FILE"

BUILD_EXTRA_ARGS=()
if [ "$BUILD_NO_CACHE" = "1" ]; then
  BUILD_EXTRA_ARGS+=(--no-cache)
fi

attempt=1
while [ "$attempt" -le "$BUILD_MAX_ATTEMPTS" ]; do
  log_line "[DEPLOY] Build tentative $attempt/$BUILD_MAX_ATTEMPTS..."
  if compose build "${BUILD_EXTRA_ARGS[@]}"; then
    break
  fi

  if [ "$attempt" -ge "$BUILD_MAX_ATTEMPTS" ]; then
    log_line "[DEPLOY] Build échoué après $BUILD_MAX_ATTEMPTS tentatives"
    rollback
  fi

  log_line "[DEPLOY] Build échoué, nouvelle tentative dans ${BUILD_RETRY_DELAY}s..."
  sleep "$BUILD_RETRY_DELAY"
  attempt=$((attempt + 1))
done

log_line "[DEPLOY] Démarrage stack (sans arrêt prolongé)..."
compose up -d

log_line "[DEPLOY] Health backend: $BACKEND_HEALTH_URL"

backend_ok=0
for i in $(seq 1 "$HEALTH_MAX_ATTEMPTS"); do
  if curl -fsS -m 10 "$BACKEND_HEALTH_URL" >/dev/null 2>&1; then
    backend_ok=1
    log_line "[DEPLOY] Backend OK"
    break
  fi
  sleep "$HEALTH_RETRY_DELAY"
done

if [ "$backend_ok" -ne 1 ]; then
  log_line "[DEPLOY] Backend KO après vérification"
  rollback
fi

echo "$NEW_TAG" > "$CURRENT_FILE"
log_line "[DEPLOY] Déploiement réussi. Tag: $NEW_TAG"

# Recréer cloudflared si le tunnel est configuré (route doit pointer vers http://caddy:80)
if [ -n "${CLOUDFLARED_TUNNEL_TOKEN:-}" ]; then
  if $COMPOSE_BIN -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" --project-directory "$REPO_ROOT" config --services 2>/dev/null | grep -qx "cloudflared"; then
    log_line "[DEPLOY] Recréation du tunnel Cloudflare..."
    $COMPOSE_BIN --env-file "$ENV_FILE" -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" --project-directory "$REPO_ROOT" --profile tunnel up -d --force-recreate cloudflared 2>/dev/null || true
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "cloudflared"; then
      log_line "[DEPLOY] Cloudflared OK"
    else
      log_line "[DEPLOY] WARNING: cloudflared non démarré (vérifier CLOUDFLARED_TUNNEL_TOKEN et profile tunnel)"
    fi
  fi
fi

log_line "[DEPLOY] Nettoyage Docker..."
docker builder prune -f >/dev/null 2>&1 || true
docker image prune -f >/dev/null 2>&1 || true

log_line "[DEPLOY] État final:"
compose ps || true

log_line "[DEPLOY] Terminé"
exit 0
