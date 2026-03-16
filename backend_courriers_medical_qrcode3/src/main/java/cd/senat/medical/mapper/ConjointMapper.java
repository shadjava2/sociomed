// src/main/java/cd/senat/medical/mapper/ConjointMapper.java
package cd.senat.medical.mapper;

import cd.senat.medical.dto.AgentsDTO;
import cd.senat.medical.dto.ConjointDTO;
import cd.senat.medical.entity.Conjoint;

public final class ConjointMapper {

  private ConjointMapper() {}

  /* ===================== LISTE (Summary) ===================== */
  public static ConjointDTO.Summary toSummary(Conjoint c) {
    if (c == null) return null;
    return new ConjointDTO.Summary(
        c.getId(),
        c.getNom(),
        c.getPostnom(),
        c.getPrenom(),
        c.getGenre(),
        c.getDatenaiss(),
        /* profession */ c.getProfession(),   // ✅ mappé
        c.getPhoto(),
        (c.getAgent() != null ? c.getAgent().getId() : null),
        (c.getSenateur() != null ? c.getSenateur().getId() : null)
    );
  }

  /* ===================== DÉTAIL (Detail) ===================== */
  public static ConjointDTO.Detail toDetail(Conjoint c) {
    if (c == null) return null;
    return new ConjointDTO.Detail(
        c.getId(),
        c.getNom(),
        c.getPostnom(),
        c.getPrenom(),
        c.getGenre(),
        c.getDatenaiss(),
        /* profession */ c.getProfession(),   // ✅ mappé
        c.getPhoto(),
        (c.getAgent() != null ? c.getAgent().getId() : null),
        (c.getSenateur() != null ? c.getSenateur().getId() : null)
    );
  }

  /* ========== Helper pour AgentsDTO.Detail.Conjoint (fiche Agent) ========== */
  public static AgentsDTO.Detail.Conjoint toAgentDetailConjoint(Conjoint c) {
    if (c == null) return null;
    return new AgentsDTO.Detail.Conjoint(
        c.getId(),
        c.getNom(),
        c.getPostnom(),
        c.getPrenom(),
        c.getDatenaiss(),
        c.getLnaiss(),     // AgentsDTO.Detail.Conjoint attend lnaiss
        c.getPhoto()
    );
  }

  /* ===================== CREATE (DTO -> Entity) ===================== */
  public static Conjoint fromCreate(ConjointDTO.CreateRequest r) {
    if (r == null) return null;
    Conjoint c = new Conjoint();
    c.setNom(r.nom());
    c.setPostnom(r.postnom());
    c.setPrenom(r.prenom());
    c.setGenre(r.genre());
    c.setDatenaiss(r.datenaiss());
    c.setProfession(r.profession());  // ✅ mappé
    c.setPhoto(r.photo());
    // c.setLnaiss(...); // si tu exposes lnaiss dans un DTO ultérieurement
    return c;
  }

  /* ===================== UPDATE (apply champ par champ) ===================== */
  public static void applyUpdate(Conjoint target, ConjointDTO.UpdateRequest r) {
    if (target == null || r == null) return;
    if (r.nom() != null)         target.setNom(r.nom());
    if (r.postnom() != null)     target.setPostnom(r.postnom());
    if (r.prenom() != null)      target.setPrenom(r.prenom());
    if (r.genre() != null)       target.setGenre(r.genre());
    if (r.datenaiss() != null)   target.setDatenaiss(r.datenaiss());
    if (r.profession() != null)  target.setProfession(r.profession()); // ✅ mappé
    if (r.photo() != null)       target.setPhoto(r.photo());
    // NB : le (re)rattachement agentId/senateurId reste géré dans le service
  }
}
