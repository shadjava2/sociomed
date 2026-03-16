package cd.senat.medical.dto;

import java.util.Date;

/**
 * Réponse de l'API de vérification publique PEC (page après scan QR).
 * Contient toutes les infos de la note pour affichage HTML (photo, motif, médecin, etc.)
 * et la date d'expiration du lien (Actif / Expiré).
 */
public record PecVerifyResponse(
        String status,           // VALID | EXPIRED | INVALID | NOT_FOUND | ERROR
        String message,
        Long pecId,
        String numero,
        String nom,
        String postnom,
        String prenom,
        String genre,            // M, F, etc.
        String age,
        String adresseMalade,
        String qualiteMalade,
        String etablissement,
        String motif,
        Date dateEmission,
        Date dateExpiration,             // date expiration PEC si renseignée
        String createdByFullname,         // Médecin du Sénat / émis par
        String photoUrl,                  // URL publique pour afficher la photo
        Date tokenExpiresAt,              // expiration du lien QR → Actif / Expiré
        String pdfUrl                     // conservé pour usage interne (bouton masqué en front)
) {}
