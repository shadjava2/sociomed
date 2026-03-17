# Diagnostic : impression PDF / report (erreur 500)

## 1. Voir les logs backend

Sur le serveur (après une tentative d’impression qui échoue) :

```bash
docker logs pec-backend 2>&1 | tail -80
```

Ou en continu pendant que vous cliquez sur « Imprimer la liste » ou « PDF » :

```bash
docker logs -f pec-backend 2>&1
```

Recherchez une ligne du type :

- `Erreur impression listing PEC: <Exception> — <message>`
- `Erreur print PEC: <Exception> — <message>`

Le **message** et la **stack trace** juste en dessous indiquent la cause.

---

## 2. Causes fréquentes

| Log / symptôme | Cause probable | Action |
|----------------|----------------|--------|
| `ClassPathResource ... not found` / `NoSuchFile` | Fichier rapport ou image manquant dans le JAR | Vérifier que `reports/pec_listing.jrxml`, `reports/pec_note.jrxml` et éventuellement `reports/fond_*.png` sont dans `src/main/resources/` et inclus dans le build. |
| `Font ... is not available` / `DejaVu` | Police absente dans le conteneur | Le Dockerfile doit installer les polices (ex. DejaVu) ou utiliser une image avec polices ; ou modifier le rapport pour une police fournie par l’image. |
| `JasperReport` / `JRException` / `fillReport` | Données ou paramètres incompatibles avec le rapport | Vérifier que les noms des champs dans `pec_listing.jrxml` (numero, dateEmissionStr, etc.) correspondent exactement à ceux passés par le controller. |
| `NullPointerException` dans le controller | Paramètre ou ressource null | Vérifier `loadLogoBytes()` / `loadBackgroundImageBytes()` : au moins un logo ou fond doit être présent dans les ressources, ou le rapport doit gérer un paramètre null. |
| Erreur SQL / `listForListing` | Problème base de données ou requête | Vérifier les logs SQL et la méthode `listForListing` (connexion MySQL, droits, colonnes). |

---

## 3. Vérifications rapides

- **Ressources dans le JAR** (sur le serveur, dans le conteneur) :
  ```bash
  docker exec pec-backend sh -c 'cd /app && jar tf *.jar 2>/dev/null | grep -E "reports/|static/logo"' | head -20
  ```
  Vous devez voir au moins `reports/pec_listing.jrxml`, `reports/pec_note.jrxml` et si possible les `fond_*.png` ou `static/logo*.png`.

- **Test direct de l’endpoint** (listing) depuis le serveur :
  ```bash
  curl -v -H "Authorization: Bearer <VOTRE_JWT>" "http://127.0.0.1:9080/api/pec/print-listing?limit=5" -o /tmp/listing.pdf
  ```
  Si ça retourne 500, les logs backend (étape 1) montreront l’exception.

---

## 4. Après correction

Rebuild du backend et redémarrage :

```bash
cd /opt/sociomed
docker compose -p pec-prod build backend && docker compose -p pec-prod up -d backend
```

Puis refaire une impression et revérifier les logs si l’erreur persiste.
