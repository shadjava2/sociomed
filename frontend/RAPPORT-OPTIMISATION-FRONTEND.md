# Rapport d’optimisation frontend — Robustesse, rapidité, réseau faible

**Projet :** PRISE EN CHARGE MEDICAL (Sénat)  
**Stack :** React 18, Vite 5, TypeScript, Tailwind, React Router, Axios  
**Date :** 13 mars 2026

---

## 1. État des lieux (analyse actuelle)

### 1.1 Points positifs déjà en place
- **Vite** : build moderne, HMR, tree-shaking par défaut.
- **Tailwind** : CSS utilitaires, purge en production.
- **Auth** : `AuthContext` + `ProtectedRoute` avec état `isLoading`.
- **Loading local** : spinners (`Loader2`) sur listes (PecList, AgentsList, DashboardPec, etc.) et actions (boutons désactivés pendant requêtes).
- **Images** : `loading="lazy"` sur certaines images (SenateurDetails, SenateursList).
- **Annulation de requête** : `cancelled` dans `PublicPecVerifyPage` pour éviter setState après unmount.
- **API** : intercepteurs Axios (JWT, 401/403 → redirect login), gestion FormData.

### 1.2 Manques identifiés
| Domaine | Constat |
|--------|---------|
| **PWA** | Aucun manifest, aucun service worker → pas d’installabilité, pas d’offline. |
| **Skeletons** | Uniquement spinners ou texte "Chargement…" → pas de mise en page progressive. |
| **Routes** | Toutes importées en statique dans `App.tsx` → gros bundle initial, pas de lazy. |
| **Réseau** | Pas de timeout Axios, pas de retry, pas de feedback "réseau lent / hors ligne". |
| **Erreurs** | Pas d’Error Boundary → une erreur React peut faire écran blanc. |
| **Cache** | Pas de stratégie cache HTTP (headers), pas de cache côté client pour les données. |
| **Critical / LCP** | Logo et above-the-fold non optimisés (pas de preload, pas de skeleton shell). |

---

## 2. Skeleton progressif

### 2.1 Objectif
Remplacer les spinners par des **skeletons** qui reprennent la forme des blocs réels (listes, cartes, tableaux), pour une perception de rapidité et de stabilité même sur réseau lent.

### 2.2 Recommandations

- **Composant réutilisable `Skeleton`**
  - Un primitif (ex. `div` avec `animate-pulse` + `bg-slate-200 rounded`), utilisable en ligne ou en composants dédiés.
  - Variantes : `SkeletonText`, `SkeletonCard`, `SkeletonTable`, `SkeletonAvatar` pour garder cohérence visuelle.

- **Skeletons par écran**
  - **ProtectedRoute / auth** : skeleton de la structure globale (header + sidebar + zone contenu) au lieu de "Chargement...".
  - **Listes (PecList, AgentsList, SenateursList, HopitauxList)** : tableau avec N lignes skeleton (même nombre de colonnes), barre de filtres en place avec champs "grisés".
  - **DashboardPec** : grille de cartes (même layout que les stats) avec rectangles pulse.
  - **PublicPecVerifyPage** : bloc carte avec lignes de texte skeleton + badge.
  - **Détails (AgentDetails, SenateurDetails)** : en-tête + sections (photo, infos, onglets) en skeleton.

- **Stratégie d’affichage**
  - Afficher le skeleton dès le montage (structure immédiate).
  - Remplacer le skeleton par le contenu réel quand les données sont prêtes (sans flash : même hauteur/layout si possible).
  - Optionnel : garder un petit skeleton sur les zones qui chargent en différé (ex. onglet secondaire).

- **Implémentation technique**
  - Créer `src/components/ui/Skeleton.tsx` (et variantes).
  - Pour chaque page/liste : état `loading` → rendre le composant skeleton au lieu du spinner plein écran ou d’une seule ligne "Chargement…".

---

## 3. PWA robuste

### 3.1 Objectif
Rendre l’app installable, résiliente au réseau faible et partiellement utilisable hors ligne (au moins shell + page de fallback).

### 3.2 Recommandations

- **Manifest**
  - Fichier `manifest.webmanifest` (ou équivalent) : `name`, `short_name`, `theme_color` (#800020 pour coller au header), `background_color`, `display: "standalone"` ou `"minimal-ui"`.
  - Icônes : au moins 192×192 et 512×512 (PNG ou SVG selon support).
  - Lien dans `index.html` : `<link rel="manifest" href="/manifest.webmanifest">`.

- **Service Worker (Workbox recommandé)**
  - Utiliser **vite-plugin-pwa** (Workbox sous le capot) pour ne pas gérer le SW à la main.
  - Stratégies proposées :
    - **Precache** : `index.html` + chunk critique (entry + route initiale) pour affichage immédiat après installation.
    - **Runtime** : API et assets en "Network first, fallback cache" (ou "Stale while revalidate") pour limiter l’impact du réseau lent et permettre un fallback cache.
  - **Offline fallback** : page HTML minimal (ex. "Vous êtes hors ligne. Réessayez plus tard.") pour les navigations hors ligne au lieu d’écran blanc.

- **Robustesse**
  - **Mise à jour** : à chaque chargement, vérifier une nouvelle version du SW ; proposer un bandeau "Nouvelle version disponible – Recharger" au lieu de forcer le rechargement immédiat.
  - **Compatibilité** : désactiver ou adapter le SW en dev (vite-plugin-pwa gère le mode dev).
  - **Cache API** : optionnellement mettre en cache les réponses GET importantes (ex. liste PEC, stats) avec TTL court pour réduire les requêtes sur réseau faible.

- **Installation**
  - S’assurer que les critères PWA (HTTPS, manifest, SW, icônes) sont remplis pour que "Ajouter à l’écran d’accueil" apparaisse.
  - Optionnel : écouter `beforeinstallprompt` et afficher un bouton "Installer l’app" personnalisé.

---

## 4. Autres optimisations (rapidité, stabilité, réseau faible)

### 4.1 Code splitting et chargement des routes

- **Problème** : Toutes les pages sont importées en statique dans `App.tsx` → bundle JS initial lourd, premier affichage plus lent sur réseau faible.
- **Action** : Utiliser `React.lazy()` pour chaque route (AgentsList, SenateursList, PecList, DashboardPec, HopitauxList, UsersList, Parametres, PublicPecVerifyPage, etc.).
- **Pattern** :
  - `const PecList = React.lazy(() => import('./pages/PecList'));`
  - Envelopper les `<Route>` dans `<Suspense fallback={...}>` avec un fallback commun (ex. skeleton de layout ou spinner centré).
- **Bénéfice** : Réduction du bundle initial ; les écrans non visités ne sont pas téléchargés tout de suite.

### 4.2 API : timeout et retry

- **Problème** : Aucun `timeout` ni retry sur Axios → sur réseau très lent ou instable, l’utilisateur reste sans retour.
- **Actions** :
  - **Timeout** : `timeout` sur l’instance Axios (ex. 30 s pour les requêtes lourdes, 15 s par défaut). Gérer l’erreur (message "Délai dépassé. Vérifiez votre connexion.").
  - **Retry** : Pour les requêtes GET (listes, stats), retry limité (ex. 2 tentatives avec backoff court) avant d’afficher l’erreur.
  - **AbortController** : Associer un `signal` aux requêtes par écran et annuler les requêtes en vol au démontage (éviter warnings et états incohérents).

### 4.3 Indication réseau (offline / lent)

- **Problème** : L’utilisateur ne sait pas si l’échec vient du réseau ou du serveur.
- **Actions** :
  - Écouter `navigator.onLine` et événements `online` / `offline` pour afficher un bandeau discret ("Pas de connexion" / "Connexion rétablie").
  - Optionnel : détecter les réponses lentes (ex. > 3 s) et afficher "Connexion lente" ou activer un mode "données limitées" (moins de préchargements, pas d’images non critiques).

### 4.4 Error Boundary global

- **Problème** : Une erreur non gérée dans un composant peut faire un écran blanc sans message.
- **Action** : Un Error Boundary au niveau racine (ex. autour du contenu de `App` ou des routes) qui affiche une UI de repli ("Une erreur est survenue. Recharger la page.") et log l’erreur (console ou service).
- **Optionnel** : Error Boundary par zone (ex. par page ou par bloc critique) pour isoler les pannes et garder le reste de l’app utilisable.

### 4.5 Cache et données

- **Stale-while-revalidate** : Pour les listes et stats, afficher la dernière donnée en cache (ex. localStorage ou mémoire) pendant le rechargement en arrière-plan, puis mettre à jour l’UI.
- **Cache HTTP** : Côté backend, headers `Cache-Control` adaptés pour les GET peu volatils (ex. liste hôpitaux) ; côté front, ne pas désactiver le cache sans raison.

### 4.6 Images et LCP

- **Logo** : Preload du logo above-the-fold (`<link rel="preload" as="image" href="/assets/senat-logo.png">`) pour améliorer le LCP sur Login/Layout.
- **Priorité** : Garder `loading="eager"` pour le logo principal, `loading="lazy"` pour les images below-the-fold (déjà en place sur SenateurDetails / listes).
- **Dimensions** : Définir `width`/`height` ou `aspect-ratio` pour éviter les layout shifts (CLS).

### 4.7 Build Vite

- **Chunking** : Vérifier que `build.rollupOptions.output.manualChunks` sépare bien vendor (react, react-dom, react-router) des routes (ou laisser Vite le faire par défaut et mesurer).
- **Compression** : Servir les assets en gzip/brotli en production (souvent géré par l’hébergeur).
- **Analyse** : `rollup-plugin-visualizer` ou `vite-bundle-visualizer` pour identifier les gros modules (ex. lucide-react, axios) et décider d’importer les icônes une par une si besoin.

### 4.8 Accessibilité et stabilité perçue

- **Focus** : Après chargement asynchrone (liste, détail), restaurer le focus ou l’annoncer (aria-live) pour lecteurs d’écran et navigation clavier.
- **Désactivation** : Pendant les requêtes, désactiver les boutons/filtres concernés (déjà en place) et éviter les double-soumissions.

---

## 5. Synthèse des priorités

| Priorité | Action | Impact réseau faible / stabilité |
|----------|--------|-----------------------------------|
| **Haute** | Skeletons progressifs (listes, dashboard, auth) | Meilleure perception de rapidité, moins de "vide" pendant le chargement. |
| **Haute** | PWA (manifest + SW + offline fallback) | Installable, résilience hors ligne et cache. |
| **Haute** | Lazy des routes + Suspense | Réduction du premier chargement, meilleur TTI. |
| **Moyenne** | Timeout + retry API + bandeau offline | Comportement prévisible sur réseau lent ou coupé. |
| **Moyenne** | Error Boundary global | Éviter écran blanc en cas d’erreur React. |
| **Moyenne** | Preload logo + dimensions images | Meilleur LCP/CLS. |
| **Basse** | Cache données (stale-while-revalidate) | Moins de requêtes, expérience plus fluide. |
| **Basse** | Analyse de bundle + manualChunks | Affiner le découpage si besoin. |

---

## 6. Fichiers / zones concernés (sans modification immédiate)

- **Skeletons** : Nouveaux composants `src/components/ui/Skeleton*.tsx` ; adaptation de chaque page liste/dashboard/détail/public.
- **PWA** : `index.html` (lien manifest), `manifest.webmanifest`, `vite.config.ts` (vite-plugin-pwa), optionnel `src` pour page offline.
- **Lazy** : `App.tsx` (imports + Suspense autour des Routes).
- **API** : `src/config/api.ts` (timeout, retry, optionnel AbortController).
- **Réseau** : Nouveau composant ou hook `useOnline` + bandeau dans `Layout` ou `App`.
- **Erreur** : Nouveau `ErrorBoundary.tsx` et enrobage dans `App.tsx` ou `main.tsx`.

Aucune modification n’a été appliquée dans le code ; ce rapport sert de base pour validation et planification des implémentations.
