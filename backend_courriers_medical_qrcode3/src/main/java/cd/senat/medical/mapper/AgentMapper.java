// src/main/java/cd/senat/medical/mapper/AgentMapper.java
package cd.senat.medical.mapper;

import cd.senat.medical.dto.AgentsDTO;
import cd.senat.medical.entity.Agent;
import cd.senat.medical.entity.AttachesAgent;
import cd.senat.medical.entity.Conjoint;

import java.util.List;
import java.util.stream.Collectors;

public final class AgentMapper {

  private AgentMapper() {}

  /* ===================== LISTE (Summary) ===================== */
  public static AgentsDTO.Summary toSummary(Agent a) {
    if (a == null) return null;
    return new AgentsDTO.Summary(
        a.getId(),
        a.getNom(),
        a.getPostnom(),
        a.getPrenom(),
        a.getGenre(),
        a.getDatenaiss(),
        a.getDirection(),
        a.getEtat(),
        a.getTelephone(),
        a.getEmail(),
        a.getPhoto(),
        a.getCategorie()
    );
  }

  /* ===================== DÉTAIL (Detail) ===================== */
  public static AgentsDTO.Detail toDetail(Agent a) {
    if (a == null) return null;

    // Conjoint
    AgentsDTO.Detail.Conjoint conjointDTO = null;
    Conjoint c = a.getConjoint();
    if (c != null) {
      conjointDTO = new AgentsDTO.Detail.Conjoint(
          c.getId(),
          c.getNom(),
          c.getPostnom(),
          c.getPrenom(),
          c.getDatenaiss(),
          c.getLnaiss(),
          c.getPhoto() // ✅ ajout/MAJ : on mappe la photo du conjoint
      );
    }

    // Enfants
    List<AgentsDTO.Detail.Enfant> enfantsDTO =
        (a.getEnfants() == null) ? List.of()
            : a.getEnfants().stream()
                .map(AgentMapper::toEnfant)
                .collect(Collectors.toList());

    return new AgentsDTO.Detail(
        a.getId(),
        a.getNom(),
        a.getPostnom(),
        a.getPrenom(),
        a.getGenre(),
        a.getDatenaiss(),
        a.getLnaiss(),
        a.getEtatc(),
        a.getVillage(),
        a.getGroupement(),
        a.getSecteur(),
        a.getTerritoire(),
        a.getDistrict(),
        a.getProvince(),
        a.getNationalite(),
        a.getTelephone(),
        a.getEmail(),
        a.getAdresse(),
        a.getDirection(),
        a.getEtat(),
        a.getStat(),
        a.getPhoto(),
        a.getCategorie(),
        conjointDTO,
        enfantsDTO
    );
  }

  private static AgentsDTO.Detail.Enfant toEnfant(AttachesAgent e) {
    if (e == null) return null;
    return new AgentsDTO.Detail.Enfant(
        e.getId(),
        e.getNomEnfant(),
        e.getPostnomEnfant(),
        e.getPrenomEnfant(),
        e.getDatenaiss(),
        e.getGenre(),
        e.getCategorie(),
        e.getStat(),
        e.getPhoto(),     // déjà présent
        e.getReference()
    );
  }

  /* ===================== CREATE ===================== */
  public static Agent fromCreate(AgentsDTO.CreateRequest r) {
    if (r == null) return null;
    Agent a = new Agent();
    a.setNom(r.nom());
    a.setPostnom(r.postnom());
    a.setPrenom(r.prenom());
    a.setGenre(r.genre());
    a.setDatenaiss(r.datenaiss());
    a.setLnaiss(r.lnaiss());
    a.setEtatc(r.etatc());
    a.setVillage(r.village());
    a.setGroupement(r.groupement());
    a.setSecteur(r.secteur());
    a.setTerritoire(r.territoire());
    a.setDistrict(r.district());
    a.setProvince(r.province());
    a.setNationalite(r.nationalite());
    a.setTelephone(r.telephone());
    a.setEmail(r.email());
    a.setAdresse(r.adresse());
    a.setDirection(r.direction());
    a.setEtat(r.etat());
    a.setStat(r.stat());
    a.setPhoto(r.photo());
    a.setCategorie(r.categorie());
    return a;
  }

  /* ===================== UPDATE (champ par champ) ===================== */
  public static void applyUpdate(Agent a, AgentsDTO.UpdateRequest r) {
    if (a == null || r == null) return;

    if (r.nom() != null) a.setNom(r.nom());
    if (r.postnom() != null) a.setPostnom(r.postnom());
    if (r.prenom() != null) a.setPrenom(r.prenom());
    if (r.genre() != null) a.setGenre(r.genre());
    if (r.datenaiss() != null) a.setDatenaiss(r.datenaiss());
    if (r.lnaiss() != null) a.setLnaiss(r.lnaiss());

    if (r.etatc() != null) a.setEtatc(r.etatc());
    if (r.village() != null) a.setVillage(r.village());
    if (r.groupement() != null) a.setGroupement(r.groupement());
    if (r.secteur() != null) a.setSecteur(r.secteur());
    if (r.territoire() != null) a.setTerritoire(r.territoire());
    if (r.district() != null) a.setDistrict(r.district());
    if (r.province() != null) a.setProvince(r.province());
    if (r.nationalite() != null) a.setNationalite(r.nationalite());

    if (r.telephone() != null) a.setTelephone(r.telephone());
    if (r.email() != null) a.setEmail(r.email());
    if (r.adresse() != null) a.setAdresse(r.adresse());

    if (r.direction() != null) a.setDirection(r.direction());
    if (r.etat() != null) a.setEtat(r.etat());
    if (r.stat() != null) a.setStat(r.stat());
    if (r.photo() != null) a.setPhoto(r.photo());
    if (r.categorie() != null) a.setCategorie(r.categorie());
  }
}
