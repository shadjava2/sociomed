#!/usr/bin/env bash
# push.sh — Commit les changements si besoin, puis push vers le serveur
# Usage:
#   ./scripts/push.sh
#   ./scripts/push.sh "Message de commit"
#
# Pourquoi "Everything up-to-date" ? Soit aucun nouveau commit (rien n'a été committé),
# soit tout est déjà poussé. Ce script committe d'abord les changements puis push.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$REPO_ROOT"

echo "[PUSH] Dépôt: $REPO_ROOT"
echo ""

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
UPSTREAM="$(git rev-parse --abbrev-ref '@{upstream}' 2>/dev/null || true)"

echo "[PUSH] Branche: $BRANCH"
[ -n "$UPSTREAM" ] && echo "[PUSH] Remote: $UPSTREAM"
echo ""

# Fichiers modifiés ou non suivis ?
if ! git status --porcelain | grep -q .; then
  # Rien à committer — y a-t-il des commits à pousser ?
  if [ -z "$UPSTREAM" ]; then
    echo "[PUSH] Aucun upstream. Premier push ?"
    git push -u origin "$BRANCH" 2>/dev/null || git push
    exit $?
  fi
  LOCAL=$(git rev-parse HEAD)
  REMOTE=$(git rev-parse '@{upstream}' 2>/dev/null || true)
  BASE=$(git merge-base HEAD '@{upstream}' 2>/dev/null || true)
  if [ "$LOCAL" = "$REMOTE" ]; then
    echo "[PUSH] Rien à committer et rien à pousser (déjà à jour)."
    echo "[PUSH] Modifiez des fichiers puis relancez ce script, ou faites un commit à la main."
    exit 0
  fi
  if [ "$LOCAL" = "$BASE" ]; then
    echo "[PUSH] Le remote a des commits que vous n'avez pas. Faites: git pull puis relancez."
    exit 0
  fi
  echo "[PUSH] Push des commits existants..."
  git push
  echo "[PUSH] Terminé: commits poussés."
  exit 0
fi

# Il y a des changements
echo "[PUSH] Fichiers modifiés ou non suivis:"
git status -s
echo ""

MSG="${1:-Update}"
echo "[PUSH] Ajout de tous les fichiers et commit: $MSG"
git add -A
git commit -m "$MSG" || { echo "[PUSH] Commit annulé ou rien à committer."; exit 0; }
echo "[PUSH] Push vers le serveur..."
git push
echo "[PUSH] Terminé: changements committés et poussés."
