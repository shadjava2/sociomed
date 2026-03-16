// src/main/java/cd/senat/medical/dto/SenateurDTO.java
package cd.senat.medical.dto;

import cd.senat.medical.entity.Genre;
import cd.senat.medical.entity.Senateur;
import cd.senat.medical.entity.StatutSenateur;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** DTOs + mapper pour Senateur (aligné sur l'entité fournie). */
public final class SenateurDTO {

    /* ------------ READ ------------ */
    /** Pour les listes (léger). */
    public record Summary(
            Long id,
            String nom,
            String postnom,
            String prenom,
            Genre genre,
            StatutSenateur statut,
            String legislature,
            String telephone,
            String email,
            String photo
    ) {}

    /** Pour l’écran de détail (avec contact + relations). */
    public record Detail(
            Long id,
            String nom,
            String postnom,
            String prenom,
            Genre genre,
            Date datenaiss,
            StatutSenateur statut,
            String telephone,
            String legislature,
            String email,
            String adresse,
            String photo,         // photo du sénateur
            Conjoint conjoint,
            List<Enfant> enfants
    ) {}

    /** Conjoint du sénateur (avec photo pour l’affichage front). */
    public record Conjoint(
            Long id,
            String nom,
            String postnom,
            String prenom,
            Date datenaiss,
            Genre genre,
            String photo          // <-- ajouté pour l’affichage
    ) {}

    /** Enfant (version légère pour le détail du sénateur). */
    public record Enfant(
            Long id,
            String nom,
            String prenom,
            Date datenaiss,
            Genre genre
            // String photo  // décommente si tu veux les photos aussi dans le détail
    ) {}

    /* ------------ WRITE (payloads venant du front) ------------ */
    public record CreateRequest(
            String nom,
            String postnom,
            String prenom,
            Genre genre,
            Date datenaiss,
            StatutSenateur statut,   // EN_ACTIVITE / HONORAIRE
            String telephone,
            String legislature,
            String email,
            String adresse,
            String photo
    ) {}

    public record UpdateRequest(
            String nom,
            String postnom,
            String prenom,
            Genre genre,
            Date datenaiss,
            StatutSenateur statut,
            String telephone,
            String legislature,
            String email,
            String adresse,
            String photo
    ) {}

    /* ------------ Mapper : Entity -> DTO ------------ */
    public static Summary toSummary(Senateur s) {
        if (s == null) return null;
        return new Summary(
                s.getId(),
                s.getNom(),
                s.getPostnom(),
                s.getPrenom(),
                s.getGenre(),
                s.getStatut(),
                s.getLegislature(),
                s.getTelephone(),
                s.getEmail(),
                s.getPhoto()
        );
    }

    public static Detail toDetail(Senateur s) {
        if (s == null) return null;

        Conjoint cj = null;
        if (s.getConjoint() != null) {
            cj = new Conjoint(
                    s.getConjoint().getId(),
                    s.getConjoint().getNom(),
                    s.getConjoint().getPostnom(),
                    s.getConjoint().getPrenom(),
                    s.getConjoint().getDatenaiss(),
                    s.getConjoint().getGenre(),
                    s.getConjoint().getPhoto() // map de la photo du conjoint
            );
        }

        List<Enfant> kids = null;
        if (s.getEnfants() != null) {
            kids = s.getEnfants().stream()
                    .filter(Objects::nonNull)
                    .map(e -> new Enfant(
                            e.getId(),
                            e.getNomEnfant(),
                            e.getPrenomEnfant(),
                            e.getDatenaiss(),
                            e.getGenre()
                            // e.getPhoto()
                    ))
                    .collect(Collectors.toList());
        }

        return new Detail(
                s.getId(),
                s.getNom(),
                s.getPostnom(),
                s.getPrenom(),
                s.getGenre(),
                s.getDatenaiss(),
                s.getStatut(),
                s.getTelephone(),
                s.getLegislature(),
                s.getEmail(),
                s.getAdresse(),
                s.getPhoto(),
                cj,
                kids
        );
    }

    /* ------------ Mapper : DTO -> Entity (apply) ------------ */
    public static void apply(Senateur target, CreateRequest r) {
        if (target == null || r == null) return;
        target.setNom(r.nom());
        target.setPostnom(r.postnom());
        target.setPrenom(r.prenom());
        target.setGenre(r.genre());
        target.setDatenaiss(r.datenaiss());
        target.setStatut(r.statut());
        target.setTelephone(r.telephone());
        target.setLegislature(r.legislature());
        target.setEmail(r.email());
        target.setAdresse(r.adresse());
        target.setPhoto(r.photo());
    }

    public static void apply(Senateur target, UpdateRequest r) {
        if (target == null || r == null) return;
        target.setNom(r.nom());
        target.setPostnom(r.postnom());
        target.setPrenom(r.prenom());
        target.setGenre(r.genre());
        target.setDatenaiss(r.datenaiss());
        target.setStatut(r.statut());
        target.setTelephone(r.telephone());
        target.setLegislature(r.legislature());
        target.setEmail(r.email());
        target.setAdresse(r.adresse());
        target.setPhoto(r.photo());
    }

    private SenateurDTO() {}
}
