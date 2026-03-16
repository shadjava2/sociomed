package cd.senat.medical.dto;

import cd.senat.medical.entity.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

public final class PriseEnChargeDTO {

    /* === READ (liste) === */
    public record Item(
            Long id,
            String numero,
            BeneficiaireType type,
            String nom,
            String postnom,
            String prenom,
            Genre genre,
            String qualiteMalade,
            String etablissement,
            String motif,
            Date dateEmission,
            Date dateExpiration,
            String createdByFullname,
            String photo
    ) {}

    /* === READ (détail) === */
    public record Detail(
            Long id,
            String numero,
            BeneficiaireType type,
            Long agentId,
            Long senateurId,
            Long conjointId,
            Long enfantId,
            String photo,
            String nom,
            String postnom,
            String prenom,
            Genre genre,
            String age,
            String qualiteMalade,
            String adresseMalade,
            String etablissement,
            String motif,
            String actes,
            String remarque,
            Date dateEmission,
            Date dateExpiration,
            String createdBy,
            String createdByFullname
    ) {}

    /* === WRITE === */
    public record CreateRequest(
            Long hopitalId,
            Long agentId,
            Long senateurId,
            Long conjointId,
            Long enfantId,
            String qualiteMalade,
            String adresseMalade,
            String etablissement,
            String motif,
            String actes,
            String remarque
    ) {}

    public record UpdateRequest(
            String qualiteMalade,
            String adresseMalade,
            String etablissement,
            String motif,
            String actes,
            String remarque
    ) {}

    /* === Helpers pour extraire la photo du bénéficiaire === */
    private static String photoOf(PriseEnCharge p) {
        if (p.getAgent() != null && p.getAgent().getPhoto() != null) return p.getAgent().getPhoto();
        if (p.getSenateur() != null && p.getSenateur().getPhoto() != null) return p.getSenateur().getPhoto();
        if (p.getConjoint() != null && p.getConjoint().getPhoto() != null) return p.getConjoint().getPhoto();
        if (p.getEnfant() != null && p.getEnfant().getPhoto() != null) return p.getEnfant().getPhoto();
        return null;
    }

    /** Calcule l'âge en années à partir de la date de naissance (pour affichage Jasper). */
    private static String ageFrom(Date datenaiss) {
        if (datenaiss == null) return null;
        java.time.LocalDate birthDate;
        if (datenaiss instanceof java.sql.Date) {
            birthDate = ((java.sql.Date) datenaiss).toLocalDate();
        } else {
            birthDate = datenaiss.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        long years = java.time.temporal.ChronoUnit.YEARS.between(birthDate, java.time.LocalDate.now());
        return years >= 0 ? String.valueOf(years) : null;
    }

    private static Date dateNaissanceOf(PriseEnCharge p) {
        if (p.getAgent() != null) return p.getAgent().getDatenaiss();
        if (p.getSenateur() != null) return p.getSenateur().getDatenaiss();
        if (p.getConjoint() != null) return p.getConjoint().getDatenaiss();
        if (p.getEnfant() != null) return p.getEnfant().getDatenaiss();
        return null;
    }

    /* === Mapper === */
    public static Item toItem(PriseEnCharge p) {
        if (p == null) return null;
        return new Item(
                p.getId(),
                p.getNumero(),
                p.getType(),
                p.getNom(),
                p.getPostnom(),
                p.getPrenom(),
                p.getGenre(),
                p.getQualiteMalade(),
                p.getEtablissement(),
                p.getMotif(),
                p.getDateEmission(),
                p.getDateExpiration(),
                p.getCreatedByFullname(),
                photoOf(p)
        );
    }

    public static Detail toDetail(PriseEnCharge p) {
        if (p == null) return null;
        return new Detail(
                p.getId(),
                p.getNumero(),
                p.getType(),
                p.getAgent() != null ? p.getAgent().getId() : null,
                p.getSenateur() != null ? p.getSenateur().getId() : null,
                p.getConjoint() != null ? p.getConjoint().getId() : null,
                p.getEnfant() != null ? p.getEnfant().getId() : null,
                photoOf(p),
                p.getNom(),
                p.getPostnom(),
                p.getPrenom(),
                p.getGenre(),
                ageFrom(dateNaissanceOf(p)),
                p.getQualiteMalade(),
                p.getAdresseMalade(),
                p.getEtablissement(),
                p.getMotif(),
                p.getActes(),
                p.getRemarque(),
                p.getDateEmission(),
                p.getDateExpiration(),
                p.getCreatedBy(),
                p.getCreatedByFullname()
        );
    }

    private PriseEnChargeDTO() {}

    /* =========================================================
       === DTOs POUR LISTE PAR HOPITAL / IMPRESSION JASPER    ===
       ========================================================= */

    /** Filtre pour la liste */
    public record ListFilter(
            Long hopitalId,
            Date from,
            Date to
    ) {}

    /** Ligne pour tableau / PDF Jasper */
    public record ListByHopitalItem(
            Long id,
            String numero,
            Date dateEmission,
            Date dateExpiration,
            String statut,
            Long hopitalId,
            String hopitalNom,
            String beneficiaireNom,
            String beneficiaireQualite,
            String etablissement,
            BigDecimal montant
    ) {
        public static ListByHopitalItem fromRow(cd.senat.medical.repository.PecRow r) {
            return new ListByHopitalItem(
                    r.getId(),
                    r.getNumero(),
                    toDate(r.getCreatedAt()),
                    null,
                    "",
                    r.getHopitalId(),
                    r.getHopitalNom(),
                    r.getBeneficiaireNom(),
                    r.getBeneficiaireQualite(),
                    r.getEtablissement(),
                    r.getMontant()
            );
        }
    }

    /** Mini DTO pour titre / libellé hôpital */
    public record HopitalMini(
            Long id,
            String nom
    ) {}

    /* === util === */
    private static Date toDate(LocalDateTime ldt) {
        return (ldt == null) ? null : Date.from(Timestamp.valueOf(ldt).toInstant());
    }
}