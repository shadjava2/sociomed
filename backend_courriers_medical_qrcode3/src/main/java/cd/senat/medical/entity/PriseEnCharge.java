// src/main/java/cd/senat/medical/entity/PriseEnCharge.java
package cd.senat.medical.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Calendar;
import java.util.Date;

@Data
@Entity
@Table(
    name = "prise_en_charge",
    indexes = {
        @Index(name = "idx_pec_numero",          columnList = "numero",        unique = true),
        @Index(name = "idx_pec_type",            columnList = "type"),
        @Index(name = "idx_pec_agent",           columnList = "agent_id"),
        @Index(name = "idx_pec_senateur",        columnList = "senateur_id"),
        @Index(name = "idx_pec_conjoint",        columnList = "conjoint_id"),
        @Index(name = "idx_pec_enfant",          columnList = "enfant_id"),
        @Index(name = "idx_pec_hopital",         columnList = "hopital_id"),
        @Index(name = "idx_pec_date_emission",   columnList = "date_emission"),
        @Index(name = "idx_pec_date_expiration", columnList = "date_expiration")
    }
)
public class PriseEnCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numéro imprimé sur la note (unique). Ex: PEC-202510-000123 */
    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    /** Type de bénéficiaire (AGENT / SENATEUR / CONJOINT / ENFANT) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BeneficiaireType type;

    /** Lien vers le bénéficiaire (un seul des 4 est non-null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senateur_id")
    private Senateur senateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conjoint_id")
    private Conjoint conjoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enfant_id")
    private AttachesAgent enfant;

    /** Établissement destinataire (obligatoire) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hopital_id", nullable = false)
    private Hopital hopital;

    /** Snapshot identité pour impression (évite de relire l’arbre complet au moment d’imprimer) */
    @Column(nullable = false, length = 160)
    private String nom; // tu peux y mettre "NOM POSTNOM" si tu veux

    @Column(length = 80)
    private String postnom;

    @Column(length = 80)
    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Genre genre;

    /** Info libre : “Agent DOCUMENTATION”, “Conjoint(e) de …”, “Enfant de …” */
    @Column(length = 255)
    private String qualiteMalade;

    /** Adresse (ex. domicile / service) */
    @Column(length = 255)
    private String adresseMalade;

    /** Libellé imprimé “EST ADRESSÉ À LA …” (peut dupliquer hopital.nom si tu veux un texte libre) */
    @Column(length = 255)
    private String etablissement;

    /** Motif/diagnostic */
    @Column(length = 1000)
    private String motif;

    /** Dates : émission + expiration (validité par défaut 30 jours) */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_emission", nullable = false)
    private Date dateEmission;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_expiration", nullable = false)
    private Date dateExpiration;

    /** Actes et remarques (facultatif) */
    @Column(length = 1000)
    private String actes;

    @Column(length = 1000)
    private String remarque;

    /** Traçabilité (utilisateur qui a émis la PEC) */
    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;            // username (ex: "jdoe")

    @Column(name = "created_by_fullname", nullable = false, length = 255)
    private String createdByFullname;    // Nom complet à imprimer sur la note

    @PrePersist
    protected void prePersist() {
        // Garantir createdBy / createdByFullname avant tout INSERT
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = "system";
        }
        if (createdByFullname == null || createdByFullname.isBlank()) {
            createdByFullname = "SYSTEM";
        }
        final Date now = new Date();
        if (dateEmission == null) {
            dateEmission = now;
        }
        if (dateExpiration == null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateEmission);
            cal.add(Calendar.DAY_OF_MONTH, 30); // validité : 30 jours
            dateExpiration = cal.getTime();
        }
    }
}
