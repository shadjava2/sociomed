# Backend Spring Boot - Gestion Courriers et Audiences

## Description
API REST pour la gestion des courriers et audiences du Sénat, développée avec Spring Boot 3.2.1 et Java 21.

## Technologies utilisées
- **Java 21**
- **Spring Boot 3.2.1**
- **Spring Data JPA**
- **MySQL 8.0**
- **Maven**
- **ModelMapper**

## Configuration

### Base de données
Configurez votre base de données MySQL dans `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/senat_courriers_audiences?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    username: root
    password: password
```

### Démarrage
```bash
cd backend
mvn spring-boot:run
```

L'API sera disponible sur `http://localhost:8080`

## Endpoints principaux

### Courriers
- `GET /api/courriers` - Liste paginée avec filtres
- `GET /api/courriers/{id}` - Détails d'un courrier
- `POST /api/courriers` - Créer un courrier
- `PUT /api/courriers/{id}` - Modifier un courrier
- `DELETE /api/courriers/{id}` - Supprimer un courrier
- `GET /api/courriers/stats` - Statistiques

### Audiences
- `GET /api/audiences` - Liste paginée avec filtres
- `GET /api/audiences/{id}` - Détails d'une audience
- `POST /api/audiences` - Créer une audience
- `PUT /api/audiences/{id}` - Modifier une audience
- `DELETE /api/audiences/{id}` - Supprimer une audience
- `GET /api/audiences/upcoming` - Audiences à venir
- `GET /api/audiences/today` - Audiences du jour
- `GET /api/audiences/stats` - Statistiques

## Fonctionnalités

### Pagination
Tous les endpoints de liste supportent la pagination:
- `page` : numéro de page (défaut: 0)
- `size` : taille de page (défaut: 10, max: 100)
- `sortBy` : champ de tri
- `sortDir` : direction (asc/desc)

### Filtres Courriers
- `searchTerm` : recherche dans expéditeur, destinataire, objet, référence
- `typeCourrier` : RECU/ENVOYE
- `traite` : true/false
- `priorite` : haute/normale/basse
- `dateDebut` / `dateFin` : période

### Filtres Audiences
- `searchTerm` : recherche dans titre, description, lieu
- `statut` : PLANIFIEE/EN_COURS/TERMINEE/ANNULEE
- `typeAudience` : PUBLIQUE/PRIVEE/COMMISSION
- `dateDebut` / `dateFin` : période

## Structure du projet
```
backend/
├── src/main/java/fr/senat/courriersaudiences/
│   ├── entity/          # Entités JPA
│   ├── repository/      # Repositories Spring Data
│   ├── service/         # Services métier
│   ├── controller/      # Contrôleurs REST
│   ├── dto/            # Data Transfer Objects
│   └── config/         # Configuration
└── src/main/resources/
    └── application.yml  # Configuration application
```

## Base de données
Le schéma est créé automatiquement avec Hibernate DDL.

### Tables principales:
- `courriers` - Courriers avec annexes
- `audiences` - Audiences avec participants
- `annexes` - Fichiers joints aux courriers
- `participants` - Participants aux audiences