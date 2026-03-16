package cd.senat.medical.service;

import cd.senat.medical.dto.AgentsDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.Agent;
import cd.senat.medical.entity.AttachesAgent;
import cd.senat.medical.entity.Genre;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.mapper.AgentMapper;
import cd.senat.medical.repository.AgentRepository;
import cd.senat.medical.repository.AttachesAgentRepository;
import cd.senat.medical.repository.PriseEnChargeRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class AgentAppServiceImpl implements AgentAppService {

  private final AgentRepository repo;
  private final AttachesAgentRepository attachesAgentRepo;
  private final PriseEnChargeRepository priseEnChargeRepo;
  private final EntityManager entityManager;

  public AgentAppServiceImpl(AgentRepository repo,
                             AttachesAgentRepository attachesAgentRepo,
                             PriseEnChargeRepository priseEnChargeRepo,
                             EntityManager entityManager) {
    this.repo = repo;
    this.attachesAgentRepo = attachesAgentRepo;
    this.priseEnChargeRepo = priseEnChargeRepo;
    this.entityManager = entityManager;
  }

  /* ===================== LISTE (Summary) ===================== */
  @Override
  @Transactional(Transactional.TxType.SUPPORTS)
  public PageResponse<AgentsDTO.Summary> getAll(String q, Genre genre, String etat, String direction, String categorie, Pageable pageable) {
    boolean hasFilter =
        (q != null && !q.isBlank()) ||
        genre != null ||
        (etat != null && !etat.isBlank()) ||
        (direction != null && !direction.isBlank()) ||
        (categorie != null && !categorie.isBlank());

    Page<Agent> page = hasFilter
        ? repo.searchCombined(q, genre, etat, direction, categorie, pageable)
        : repo.findAll(pageable);

    return PageResponse.from(page.map(AgentMapper::toSummary));
  }

  /* ===================== DÉTAIL (Detail) ===================== */
  @Override
  @Transactional(Transactional.TxType.SUPPORTS)
  public AgentsDTO.Detail getDetails(Long id) {
    Agent a = repo.findByIdWithRelations(id)
        .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable: " + id));
    return AgentMapper.toDetail(a);
  }

  @Override
  @Transactional(Transactional.TxType.SUPPORTS)
  public AgentsDTO.Detail getById(Long id) {
    Agent a = repo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable: " + id));
    // Ici on renvoie aussi Detail; si tu veux un "light", crée un DTO spécifique.
    return AgentMapper.toDetail(a);
  }

  /* ===================== CREATE ===================== */
  @Override
  public AgentsDTO.Detail create(AgentsDTO.CreateRequest r) {
    // anti-doublon identité
    if (existsByIdentite(r.nom(), r.postnom(), r.prenom(), r.datenaiss(), r.lnaiss())) {
      throw new BusinessException("Doublon AGENT (identité déjà enregistrée)");
    }
    // anti-doublon contacts
    if (r.telephone() != null && repo.existsByTelephone(r.telephone()))
      throw new BusinessException("Téléphone déjà utilisé");
    if (r.email() != null && repo.existsByEmail(r.email()))
      throw new BusinessException("Email déjà utilisé");

    try {
      Agent saved = repo.save(AgentMapper.fromCreate(r));
      return AgentMapper.toDetail(saved);
    } catch (DataIntegrityViolationException ex) {
      throw new BusinessException("Contrainte d’unicité violée pour AGENT");
    }
  }

  /* ===================== UPDATE ===================== */
  @Override
  public AgentsDTO.Detail update(Long id, AgentsDTO.UpdateRequest r) {
    Agent current = repo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable: " + id));

    // identité modifiée ?
    boolean identiteChange =
        notEq(current.getNom(), r.nom()) ||
        notEq(current.getPostnom(), r.postnom()) ||
        notEq(current.getPrenom(), r.prenom()) ||
        notEq(current.getDatenaiss(), r.datenaiss()) ||
        notEq(current.getLnaiss(), r.lnaiss());

    if (identiteChange && existsByIdentiteExcludingId(
        r.nom() != null ? r.nom() : current.getNom(),
        r.postnom() != null ? r.postnom() : current.getPostnom(),
        r.prenom() != null ? r.prenom() : current.getPrenom(),
        r.datenaiss() != null ? r.datenaiss() : current.getDatenaiss(),
        r.lnaiss() != null ? r.lnaiss() : current.getLnaiss(),
        id
    )) {
      throw new BusinessException("Conflit identité AGENT (déjà existant)");
    }

    // contacts
    if (r.telephone() != null && !r.telephone().equals(current.getTelephone())
        && repo.existsByTelephone(r.telephone()))
      throw new BusinessException("Téléphone déjà utilisé");

    if (r.email() != null && !r.email().equals(current.getEmail())
        && repo.existsByEmail(r.email()))
      throw new BusinessException("Email déjà utilisé");

    // appliquer les modifs
    AgentMapper.applyUpdate(current, r);
    Agent saved = repo.save(current);
    return AgentMapper.toDetail(saved);
  }

  /* ===================== DELETE ===================== */
  @Override
  public void delete(Long id) {
    Agent a = repo.findByIdWithRelations(id)
        .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable: " + id));

    // 1. PEC où l’agent est bénéficiaire direct (agent_id)
    priseEnChargeRepo.deleteByAgent_Id(id);
    // 2. PEC référençant les enfants de cet agent
    List<AttachesAgent> enfants = attachesAgentRepo.findByAgent_Id(id);
    for (AttachesAgent enfant : enfants) {
      priseEnChargeRepo.deleteByEnfant_Id(enfant.getId());
    }
    // 3. PEC référençant le conjoint s’il existe
    if (a.getConjoint() != null) {
      priseEnChargeRepo.deleteByConjoint_Id(a.getConjoint().getId());
    }
    entityManager.flush();

    repo.delete(a);
  }

  /* ===================== COUNTS ===================== */
  @Override
  @Transactional(Transactional.TxType.SUPPORTS)
  public long countByGenre(Genre genre) {
    return repo.countByGenre(genre);
  }

  @Override
  @Transactional(Transactional.TxType.SUPPORTS)
  public long countByEtat(String etat) {
    return repo.countByEtatIgnoreCase(etat);
  }

  /* ===================== PRIVÉS ===================== */
  private boolean existsByIdentite(String nom, String postnom, String prenom, Date datenaiss, String lnaiss) {
    return repo.existsByNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaissAndLnaiss(
        nom, postnom, prenom, datenaiss, lnaiss
    );
  }

  /** Même vérification en excluant l’id donné (pour update : ne pas considérer l’agent courant comme doublon). */
  private boolean existsByIdentiteExcludingId(String nom, String postnom, String prenom, Date datenaiss, String lnaiss, Long excludeId) {
    return repo.existsByNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaissAndLnaissAndIdNot(
        nom, postnom, prenom, datenaiss, lnaiss, excludeId
    );
  }

  private static boolean notEq(Object a, Object b) {
    return (a == null && b != null) || (a != null && !a.equals(b));
  }
}
