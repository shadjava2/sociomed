// src/main/java/cd/senat/medical/dto/AgentsDTO.java
package cd.senat.medical.dto;

import cd.senat.medical.entity.CategorieEnfant;
import cd.senat.medical.entity.Genre;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

/**
 * Regroupe les DTO liés aux Agents (liste, détail, création, mise à jour).
 * Champs alignés avec l'entité Agent et le mapper AgentMapper.
 */
public final class AgentsDTO {

  /* ===================== LISTE (léger) ===================== */
  public record Summary(
      Long id,
      String nom,
      String postnom,
      String prenom,
      Genre genre,
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      Date datenaiss,
      String direction,
      String etat,
      String telephone,
      String email,
      String photo,
      String categorie
  ) {}

  /* ===================== DÉTAIL (complet) ===================== */
  public record Detail(
      Long id,
      String nom,
      String postnom,
      String prenom,
      Genre genre,
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      Date datenaiss,
      String lnaiss,
      String etatc,
      String village,
      String groupement,
      String secteur,
      String territoire,
      String district,
      String province,
      String nationalite,
      String telephone,
      String email,
      String adresse,
      String direction,
      String etat,
      String stat,
      String photo,               // photo de l'AGENT (ex: /uploads/photos/agent_12.jpg)
      String categorie,
      Conjoint conjoint,          // ✅ inclut désormais la photo du conjoint
      List<Enfant> enfants
  ) {

    /** Conjoint (champs utilisés par le mapper dans le détail d'un Agent) */
    public record Conjoint(
        Long id,
        String nom,
        String postnom,
        String prenom,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        Date datenaiss,
        String lnaiss,
        String photo            // ✅ ajout de la photo du CONJOINT
    ) {}

    /** Enfant (aligné sur AttachesAgent) */
    public record Enfant(
        Long id,
        String nomEnfant,
        String postnomEnfant,
        String prenomEnfant,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        Date datenaiss,
        Genre genre,
        CategorieEnfant categorie, // LEGITIME / ADOPTIF
        String stat,
        String photo,
        String reference
    ) {}
  }

  /* ===================== CRÉATION ===================== */
  public record CreateRequest(
      @NotBlank String nom,
      @NotBlank String postnom,
      @NotBlank String prenom,
      @NotNull  Genre genre,
      @NotNull  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      Date datenaiss,
      @NotBlank String lnaiss,
      String etatc,
      String village,
      String groupement,
      String secteur,
      String territoire,
      String district,
      String province,
      String nationalite,
      String telephone,
      String email,
      String adresse,
      String direction,
      String etat,
      @NotBlank String stat,
      String photo,               // ex: /uploads/photos/agent_12.jpg
      String categorie
  ) {}

  /* ===================== MISE À JOUR (tous champs optionnels) ===================== */
  public record UpdateRequest(
      String nom,
      String postnom,
      String prenom,
      Genre genre,
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      Date datenaiss,
      String lnaiss,
      String etatc,
      String village,
      String groupement,
      String secteur,
      String territoire,
      String district,
      String province,
      String nationalite,
      String telephone,
      String email,
      String adresse,
      String direction,
      String etat,
      String stat,
      String photo,               // ex: /uploads/photos/agent_12.jpg
      String categorie
  ) {}

  private AgentsDTO() {}
}
