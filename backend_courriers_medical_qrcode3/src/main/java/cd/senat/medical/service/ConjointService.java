package cd.senat.medical.service;

import cd.senat.medical.entity.Agent;
import cd.senat.medical.entity.Conjoint;
import cd.senat.medical.entity.Senateur;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.repository.AgentRepository;
import cd.senat.medical.repository.ConjointRepository;
import cd.senat.medical.repository.PriseEnChargeRepository;
import cd.senat.medical.repository.SenateurRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Transactional
public class ConjointService {

    private final ConjointRepository repo;
    private final AgentRepository agentRepo;
    private final SenateurRepository senRepo;
    private final PriseEnChargeRepository priseEnChargeRepo;

    public ConjointService(ConjointRepository repo, AgentRepository agentRepo, SenateurRepository senRepo,
                          PriseEnChargeRepository priseEnChargeRepo) {
        this.repo = repo;
        this.agentRepo = agentRepo;
        this.senRepo = senRepo;
        this.priseEnChargeRepo = priseEnChargeRepo;
    }

    public Conjoint createForAgent(Long agentId, Conjoint c) {
        Agent a = agentRepo.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable: " + agentId));
        if (repo.existsByAgent_Id(agentId)) throw new BusinessException("Cet agent a déjà un conjoint");

        // Anti-doublon identité pour ce parent
        if (existsByIdentiteForAgent(c.getNom(), c.getPostnom(), c.getPrenom(), c.getDatenaiss(), agentId)) {
            throw new BusinessException("Doublon CONJOINT pour cet agent (identité)");
        }

        c.setAgent(a); c.setSenateur(null);
        try {
            return repo.save(c);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Contrainte d’unicité violée pour CONJOINT (AGENT)");
        }
    }

    public Conjoint createForSenateur(Long senateurId, Conjoint c) {
        Senateur s = senRepo.findById(senateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Sénateur introuvable: " + senateurId));
        if (repo.existsBySenateur_Id(senateurId)) throw new BusinessException("Ce sénateur a déjà un conjoint");

        if (existsByIdentiteForSenateur(c.getNom(), c.getPostnom(), c.getPrenom(), c.getDatenaiss(), senateurId)) {
            throw new BusinessException("Doublon CONJOINT pour ce sénateur (identité)");
        }

        c.setSenateur(s); c.setAgent(null);
        try {
            return repo.save(c);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Contrainte d’unicité violée pour CONJOINT (SENATEUR)");
        }
    }

    public Conjoint update(Long id, Conjoint changes) {
        Conjoint c = getById(id);

        Long agentId = (c.getAgent() != null) ? c.getAgent().getId() : null;
        Long senId   = (c.getSenateur() != null) ? c.getSenateur().getId() : null;

        // si identité change, vérifier doublon pour le même parent
        boolean identiteChange =
                notEq(c.getNom(), changes.getNom()) ||
                notEq(c.getPostnom(), changes.getPostnom()) ||
                notEq(c.getPrenom(), changes.getPrenom()) ||
                notEq(c.getDatenaiss(), changes.getDatenaiss());

        if (identiteChange) {
            if (agentId != null && existsByIdentiteForAgent(changes.getNom(), changes.getPostnom(), changes.getPrenom(), changes.getDatenaiss(), agentId))
                throw new BusinessException("Conflit identité CONJOINT (agent)");
            if (senId != null && existsByIdentiteForSenateur(changes.getNom(), changes.getPostnom(), changes.getPrenom(), changes.getDatenaiss(), senId))
                throw new BusinessException("Conflit identité CONJOINT (sénateur)");
        }

        c.setNom(changes.getNom());
        c.setPostnom(changes.getPostnom());
        c.setPrenom(changes.getPrenom());
        c.setGenre(changes.getGenre());
        c.setDatenaiss(changes.getDatenaiss());
        c.setTelephone(changes.getTelephone());
        c.setEmail(changes.getEmail());
        c.setPhoto(changes.getPhoto());
        return c;
    }

    public Conjoint getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conjoint introuvable: " + id));
    }

    /** Supprime le conjoint après avoir supprimé les prises en charge qui le référencent (contrainte FK). */
    public void delete(Long id) {
        priseEnChargeRepo.deleteByConjoint_Id(id);
        repo.deleteById(id);
    }

    private boolean existsByIdentiteForAgent(String nom, String postnom, String prenom, Date datenaiss, Long agentId) {
        // Requis au repository (via @Query ou derived query)
        return repo.existsByAgent_IdAndNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaiss(
                agentId, nom, postnom, prenom, datenaiss);
    }

    private boolean existsByIdentiteForSenateur(String nom, String postnom, String prenom, Date datenaiss, Long senateurId) {
        return repo.existsBySenateur_IdAndNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaiss(
                senateurId, nom, postnom, prenom, datenaiss);
    }

    private static boolean notEq(Object a, Object b) {
        return (a == null && b != null) || (a != null && !a.equals(b));
    }
}
