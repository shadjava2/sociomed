// src/main/java/cd/senat/medical/dto/AttachesAgentDTO.java
package cd.senat.medical.dto;

import cd.senat.medical.entity.AttachesAgent;
import cd.senat.medical.entity.CategorieEnfant;
import cd.senat.medical.entity.Genre;

import java.util.Date;

public final class AttachesAgentDTO {

    /* ----------- READ ----------- */
    /** Pour les listes / détails simples (sans relations parent). */
    public record Item(
            Long id,
            String nomEnfant,
            String postnomEnfant,
            String prenomEnfant,
            Genre genre,
            Date datenaiss,
            CategorieEnfant categorie,
            String stat,
            String reference,
            String photo
    ) {}

    /* ----------- WRITE (payloads) ----------- */
    /** Création d’un enfant pour un parent (Agent OU Sénateur). */
    public record CreateRequest(
            String nomEnfant,
            String postnomEnfant,
            String prenomEnfant,
            Genre genre,
            Date datenaiss,
            CategorieEnfant categorie,
            String stat,
            String reference
            // photo : gérée à part si upload multipart
    ) {}

    /** Mise à jour d’un enfant. */
    public record UpdateRequest(
            String nomEnfant,
            String postnomEnfant,
            String prenomEnfant,
            Genre genre,
            Date datenaiss,
            CategorieEnfant categorie,
            String stat,
            String reference,
            String photo // peut rester tel quel (ou upload à part)
    ) {}

    /* ----------- Mapper Entity -> DTO ----------- */
    public static Item toItem(AttachesAgent e) {
        if (e == null) return null;
        return new Item(
                e.getId(),
                e.getNomEnfant(),
                e.getPostnomEnfant(),
                e.getPrenomEnfant(),
                e.getGenre(),
                e.getDatenaiss(),
                e.getCategorie(),
                e.getStat(),
                e.getReference(),
                e.getPhoto()
        );
    }

    /* ----------- Mapper DTO -> Entity (apply) ----------- */
    public static void apply(AttachesAgent target, CreateRequest r) {
        if (target == null || r == null) return;
        target.setNomEnfant(r.nomEnfant());
        target.setPostnomEnfant(r.postnomEnfant());
        target.setPrenomEnfant(r.prenomEnfant());
        target.setGenre(r.genre());
        target.setDatenaiss(r.datenaiss());
        target.setCategorie(r.categorie());
        target.setStat(r.stat());
        target.setReference(r.reference());
    }

    public static void apply(AttachesAgent target, UpdateRequest r) {
        if (target == null || r == null) return;
        target.setNomEnfant(r.nomEnfant());
        target.setPostnomEnfant(r.postnomEnfant());
        target.setPrenomEnfant(r.prenomEnfant());
        target.setGenre(r.genre());
        target.setDatenaiss(r.datenaiss());
        target.setCategorie(r.categorie());
        target.setStat(r.stat());
        target.setReference(r.reference());
        target.setPhoto(r.photo());
    }

    private AttachesAgentDTO() {}
}
