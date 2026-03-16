// src/main/java/cd/senat/medical/dto/HopitalDTO.java
package cd.senat.medical.dto;

import cd.senat.medical.entity.CategorieHopital;
import cd.senat.medical.entity.Hopital;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class HopitalDTO {

    /* ===================== READ ===================== */

    /** Pour les listes (léger). */
    public record Summary(
            Long id,
            String code,
            String nom,
            CategorieHopital categorie,
            String ville,
            Boolean actif
    ) {}

    /** Pour l’écran de détail. */
    public record Detail(
            Long id,
            String code,
            String nom,
            CategorieHopital categorie,
            String adresse,
            String commune,
            String ville,
            String province,
            String pays,
            String contactNom,
            String contactTelephone,
            String email,
            String siteWeb,
            Boolean actif,
            String numeroConvention,
            LocalDateTime conventionDebut,
            LocalDateTime conventionFin,
           
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    /* ===================== WRITE ===================== */

    public record CreateRequest(
            String code,
            String nom,
            CategorieHopital categorie,
            String adresse,
            String commune,
            String ville,
            String province,
            String pays,
            String contactNom,
            String contactTelephone,
            String email,
            String siteWeb,
            Boolean actif,
            String numeroConvention,
            LocalDateTime conventionDebut,
            LocalDateTime conventionFin
           
    ) {}

    public record UpdateRequest(
            String code,
            String nom,
            CategorieHopital categorie,
            String adresse,
            String commune,
            String ville,
            String province,
            String pays,
            String contactNom,
            String contactTelephone,
            String email,
            String siteWeb,
            Boolean actif,
            String numeroConvention,
            LocalDateTime conventionDebut,
            LocalDateTime conventionFin
           
    ) {}

    /* ===================== MAPPERS ===================== */

    public static Summary toSummary(Hopital h) {
        if (h == null) return null;
        return new Summary(
                h.getId(),
                h.getCode(),
                h.getNom(),
                h.getCategorie(),
                h.getVille(),
                h.getActif()
        );
    }

    public static Detail toDetail(Hopital h) {
        if (h == null) return null;
        return new Detail(
                h.getId(),
                h.getCode(),
                h.getNom(),
                h.getCategorie(),
                h.getAdresse(),
                h.getCommune(),
                h.getVille(),
                h.getProvince(),
                h.getPays(),
                h.getContactNom(),
                h.getContactTelephone(),
                h.getEmail(),
                h.getSiteWeb(),
                h.getActif(),
                h.getNumeroConvention(),
                h.getConventionDebut(),
                h.getConventionFin(),
                h.getCreatedAt(),
                h.getUpdatedAt()
        );
    }

    /* ===================== APPLY (DTO -> Entity) ===================== */

    public static void apply(Hopital target, CreateRequest r) {
        if (target == null || r == null) return;
        target.setCode(r.code());
        target.setNom(r.nom());
        target.setCategorie(r.categorie());
        target.setAdresse(r.adresse());
        target.setCommune(r.commune());
        target.setVille(r.ville());
        target.setProvince(r.province());
        target.setPays(r.pays());
        target.setContactNom(r.contactNom());
        target.setContactTelephone(r.contactTelephone());
        target.setEmail(r.email());
        target.setSiteWeb(r.siteWeb());
        target.setActif(r.actif() != null ? r.actif() : Boolean.TRUE);
        target.setNumeroConvention(r.numeroConvention());
        target.setConventionDebut(r.conventionDebut());
        target.setConventionFin(r.conventionFin());
        
        // createdAt/updatedAt sont gérés par @PrePersist/@PreUpdate dans l’entité
    }

    public static void apply(Hopital target, UpdateRequest r) {
        if (target == null || r == null) return;
        if (r.code() != null) target.setCode(r.code());
        if (r.nom() != null) target.setNom(r.nom());
        if (r.categorie() != null) target.setCategorie(r.categorie());
        if (r.adresse() != null) target.setAdresse(r.adresse());
        if (r.commune() != null) target.setCommune(r.commune());
        if (r.ville() != null) target.setVille(r.ville());
        if (r.province() != null) target.setProvince(r.province());
        if (r.pays() != null) target.setPays(r.pays());
        if (r.contactNom() != null) target.setContactNom(r.contactNom());
        if (r.contactTelephone() != null) target.setContactTelephone(r.contactTelephone());
        if (r.email() != null) target.setEmail(r.email());
        if (r.siteWeb() != null) target.setSiteWeb(r.siteWeb());
        if (r.actif() != null) target.setActif(r.actif());
        if (r.numeroConvention() != null) target.setNumeroConvention(r.numeroConvention());
        if (r.conventionDebut() != null) target.setConventionDebut(r.conventionDebut());
        if (r.conventionFin() != null) target.setConventionFin(r.conventionFin());
      
        // updatedAt géré par @PreUpdate
    }

    private HopitalDTO() {}
}
