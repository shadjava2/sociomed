package cd.senat.medical.dto;

import cd.senat.medical.entity.Genre;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.AssertTrue;

import java.util.Date;

/**
 * DTO commun pour gérer les conjoints côté Agent ET côté Sénateur.
 * - CreateRequest : agentId XOR senateurId
 * - UpdateRequest : partiel (tous champs optionnels), même règle XOR si l’un des deux parentId est fourni
 */
public final class ConjointDTO {

    /* ============================ CREATE ============================ */
    public record CreateRequest(
        @NotBlank @Size(max = 45) String nom,
        @NotBlank @Size(max = 45) String postnom,
        @Size(max = 45) String prenom,

        @NotNull Genre genre,

        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        Date datenaiss,

        @Size(max = 100) String profession,
        @Size(max = 255) String photo,

        // Parent : EXACTEMENT un des deux
        Long agentId,
        Long senateurId
    ) {
        @AssertTrue(message = "Associer exactement un parent : soit agentId, soit senateurId (pas les deux).")
        public boolean isExactlyOneParent() {
            return (agentId != null) ^ (senateurId != null);
        }
    }

    /* ============================ UPDATE (partiel) ============================ */
    public record UpdateRequest(
        // Tous optionnels → partiel. L’API décidera de « conserver » l’existant si null.
        @Size(max = 45) String nom,
        @Size(max = 45) String postnom,
        @Size(max = 45) String prenom,
        Genre genre,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        Date datenaiss,
        @Size(max = 100) String profession,
        @Size(max = 255) String photo,

        // On peut éventuellement rebasculer de parent : même règle XOR SI on touche aux ids
        Long agentId,
        Long senateurId
    ) {
        @AssertTrue(message = "Si vous modifiez le rattachement, fournissez au plus un parent : agentId XOR senateurId.")
        public boolean isValidParentChange() {
            // Autoriser « aucun changement » (les deux null) OU un seul des deux
            if (agentId == null && senateurId == null) return true;
            return (agentId != null) ^ (senateurId != null);
        }
    }

    /* ============================ SUMMARY ============================ */
    public record Summary(
        Long id,
        String nom,
        String postnom,
        String prenom,
        Genre genre,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        Date datenaiss,
        String profession,
        String photo,

        // Info parent minimale (facultative pour l’affichage)
        Long agentId,
        Long senateurId
    ) {}

    /* ============================ DETAIL ============================ */
    public record Detail(
        Long id,
        String nom,
        String postnom,
        String prenom,
        Genre genre,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        Date datenaiss,
        String profession,
        String photo,

        // Info parent
        Long agentId,
        Long senateurId
    ) {}
}
