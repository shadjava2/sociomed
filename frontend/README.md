# Frontend React - Gestion du Personnel Médical

Application React complète pour la gestion des agents, conjoints et enfants, intégrant avec votre backend Spring Boot.

## 🚀 Fonctionnalités

### Authentification
- Connexion avec JWT
- Gestion des sessions
- Protection des routes
- Rôles utilisateurs (ADMIN, USER, MANAGER)

### Gestion des Agents
- Création, modification, suppression d'agents
- Upload de photos
- Formulaires avec validation
- Affichage des détails complets

### Gestion des Conjoints
- Ajout d'un conjoint par agent
- Upload de photos
- Modification et suppression

### Gestion des Enfants
- Ajout multiple d'enfants par agent
- Catégories (Légitime/Adoptif)
- Upload de photos
- Liste complète avec actions

### Gestion des Utilisateurs (Admin uniquement)
- Liste paginée des utilisateurs
- Recherche et filtres
- Activation/désactivation
- Gestion des rôles

## 📋 Prérequis

- Node.js 18+ et npm
- Backend Spring Boot en cours d'exécution sur `http://localhost:8080`

## 🔧 Installation

1. **Installer les dépendances**
```bash
npm install
```

2. **Configurer l'API**
Créez un fichier `.env` à la racine :
```bash
VITE_API_BASE_URL=http://localhost:8080
```

3. **Démarrer l'application**
```bash
npm run dev
```

L'application sera accessible sur `http://localhost:5173`

## 🏗️ Structure du Projet

```
src/
├── components/          # Composants réutilisables
│   ├── AgentDetails.tsx    # Détails agent + conjoint + enfants
│   ├── AgentForm.tsx       # Formulaire agent
│   ├── ConjointForm.tsx    # Formulaire conjoint
│   ├── EnfantForm.tsx      # Formulaire enfant
│   ├── Layout.tsx          # Layout principal avec navigation
│   ├── Login.tsx           # Page de connexion
│   ├── ProtectedRoute.tsx  # Protection des routes
│   └── UserForm.tsx        # Formulaire utilisateur
├── pages/               # Pages principales
│   ├── AgentsList.tsx      # Liste des agents
│   └── UsersList.tsx       # Liste des utilisateurs (admin)
├── services/            # Services API
│   ├── agentService.ts     # API agents
│   ├── authService.ts      # Authentification
│   ├── conjointService.ts  # API conjoints
│   ├── enfantService.ts    # API enfants
│   └── userService.ts      # API utilisateurs
├── contexts/            # Contextes React
│   └── AuthContext.tsx     # Contexte authentification
├── config/              # Configuration
│   └── api.ts             # Configuration Axios
└── App.tsx              # Point d'entrée + routing
```

## 🔐 Authentification

L'application utilise JWT pour l'authentification :
- Token stocké dans `localStorage`
- Intercepteur Axios pour ajouter le token aux requêtes
- Redirection automatique vers `/login` si non authentifié

## 📡 API Backend

### Endpoints utilisés :

**Auth**
- `POST /api/auth/signin` - Connexion
- `POST /api/auth/signout` - Déconnexion

**Agents**
- `POST /api/agents` - Créer un agent (multipart/form-data)
- `GET /api/agents/{id}` - Récupérer un agent
- `PUT /api/agents/{id}` - Modifier un agent (multipart/form-data)
- `DELETE /api/agents/{id}` - Supprimer un agent

**Conjoints**
- `POST /api/agents/{agentId}/conjoint` - Créer un conjoint (multipart/form-data)
- `GET /api/conjoints/{id}` - Récupérer un conjoint
- `PUT /api/conjoints/{id}` - Modifier un conjoint (multipart/form-data)
- `DELETE /api/conjoints/{id}` - Supprimer un conjoint

**Enfants**
- `POST /api/agents/{agentId}/enfants` - Créer un enfant (multipart/form-data)
- `GET /api/agents/{agentId}/enfants` - Liste des enfants d'un agent
- `GET /api/enfants/{id}` - Récupérer un enfant
- `PUT /api/enfants/{id}` - Modifier un enfant (multipart/form-data)
- `DELETE /api/enfants/{id}` - Supprimer un enfant

**Utilisateurs** (ADMIN uniquement)
- `GET /api/users` - Liste paginée avec filtres
- `GET /api/users/{id}` - Récupérer un utilisateur
- `POST /api/users` - Créer un utilisateur
- `PUT /api/users/{id}` - Modifier un utilisateur
- `DELETE /api/users/{id}` - Supprimer un utilisateur
- `PUT /api/users/{id}/toggle-status` - Activer/désactiver
- `GET /api/users/stats` - Statistiques

## 🎨 Design

- Framework CSS : **Tailwind CSS**
- Icônes : **Lucide React**
- Design moderne et responsive
- Thème professionnel (couleurs neutres)
- Animations et transitions

## 📦 Build pour Production

```bash
npm run build
```

Les fichiers optimisés seront dans le dossier `dist/`

## 🔍 Format des Données

### Dates
Les dates sont au format `YYYY-MM-DD` (ISO 8601) depuis le frontend.
Si votre backend attend `dd/MM/yyyy`, vous devrez adapter les services.

### Upload de Photos
- Les photos sont envoyées en `multipart/form-data`
- Le backend retourne le nom de fichier
- Les photos sont affichées via : `${API_BASE_URL}/uploads/photos/${filename}`

### Format Agent
```json
{
  "nom": "DUPONT",
  "postnom": "MARTIN",
  "prenom": "JEAN",
  "genre": "M",
  "datenaiss": "1985-03-15",
  "lnaiss": "KINSHASA",
  "telephone": "+243123456789",
  "email": "jean.dupont@example.com",
  "stat": "ACTIF"
}
```

## 🛡️ Sécurité

- Tokens JWT avec expiration
- Routes protégées par rôle
- CORS configuré sur le backend
- Validation côté client et serveur
- Gestion sécurisée des mots de passe

## 🐛 Dépannage

### L'API ne répond pas
Vérifiez que :
1. Le backend Spring Boot est démarré
2. L'URL dans `.env` est correcte
3. CORS est configuré sur le backend avec `@CrossOrigin(origins = "*")`

### Problèmes d'authentification
- Vérifiez que le JWT est valide
- Consultez la console navigateur pour les erreurs
- Vérifiez les credentials de connexion

### Upload de photos échoue
- Vérifiez que le dossier `uploads/photos/` existe sur le serveur
- Vérifiez les permissions d'écriture
- Vérifiez la taille maximale configurée dans Spring Boot

## 📝 Scripts Disponibles

- `npm run dev` - Démarrer en mode développement
- `npm run build` - Build pour production
- `npm run preview` - Preview du build
- `npm run lint` - Vérifier le code
- `npm run typecheck` - Vérifier les types TypeScript

## 🤝 Contribution

Cette application est prête pour la production. Pour toute modification :
1. Testez localement
2. Vérifiez la compatibilité avec le backend
3. Testez tous les rôles utilisateurs

## 📄 Licence

Application développée pour la gestion du personnel médical.
