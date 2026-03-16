package cd.senat.medical.service;

import cd.senat.medical.entity.Agent;
import cd.senat.medical.entity.AttachesAgent;
import cd.senat.medical.entity.Senateur;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.repository.AgentRepository;
import cd.senat.medical.repository.AttachesAgentRepository;
import cd.senat.medical.repository.PriseEnChargeRepository;
import cd.senat.medical.repository.SenateurRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Transactional
public class AttachesAgentService {

    private final AttachesAgentRepository repo;
    private final AgentRepository agentRepo;
    private final SenateurRepository senRepo;
    private final PriseEnChargeRepository priseEnChargeRepo;

    public AttachesAgentService(AttachesAgentRepository repo,
                                AgentRepository agentRepo,
                                SenateurRepository senRepo,
                                PriseEnChargeRepository priseEnChargeRepo) {
        this.repo = repo;
        this.agentRepo = agentRepo;
        this.senRepo = senRepo;
        this.priseEnChargeRepo = priseEnChargeRepo;
    }

    public AttachesAgent createForAgent(Long agentId, AttachesAgent e) {
        Agent a = agentRepo.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable: " + agentId));

        if (existsChildForAgent(e.getNomEnfant(), e.getPostnomEnfant(), e.getPrenomEnfant(), e.getDatenaiss(), agentId)) {
            throw new BusinessException("Doublon ENFANT pour cet agent (identité)");
        }

        e.setAgent(a);
        e.setSenateur(null);
        try {
            return repo.save(e);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Contrainte d’unicité violée pour ENFANT (AGENT)");
        }
    }

    public AttachesAgent createForSenateur(Long senateurId, AttachesAgent e) {
        Senateur s = senRepo.findById(senateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Sénateur introuvable: " + senateurId));

        if (existsChildForSenateur(e.getNomEnfant(), e.getPostnomEnfant(), e.getPrenomEnfant(), e.getDatenaiss(), senateurId)) {
            throw new BusinessException("Doublon ENFANT pour ce sénateur (identité)");
        }

        e.setSenateur(s);
        e.setAgent(null);
        try {
            return repo.save(e);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Contrainte d’unicité violée pour ENFANT (SENATEUR)");
        }
    }

    public AttachesAgent update(Long id, AttachesAgent changes) {
        AttachesAgent e = getById(id);

        Long agentId = (e.getAgent() != null) ? e.getAgent().getId() : null;
        Long senId   = (e.getSenateur() != null) ? e.getSenateur().getId() : null;

        boolean identiteChange =
                notEq(e.getNomEnfant(), changes.getNomEnfant()) ||
                notEq(e.getPostnomEnfant(), changes.getPostnomEnfant()) ||
                notEq(e.getPrenomEnfant(), changes.getPrenomEnfant()) ||
                notEq(e.getDatenaiss(), changes.getDatenaiss());

        if (identiteChange) {
            if (agentId != null && existsChildForAgent(changes.getNomEnfant(), changes.getPostnomEnfant(), changes.getPrenomEnfant(), changes.getDatenaiss(), agentId)) {
                throw new BusinessException("Conflit identité ENFANT (agent)");
            }
            if (senId != null && existsChildForSenateur(changes.getNomEnfant(), changes.getPostnomEnfant(), changes.getPrenomEnfant(), changes.getDatenaiss(), senId)) {
                throw new BusinessException("Conflit identité ENFANT (sénateur)");
            }
        }

        e.setNomEnfant(changes.getNomEnfant());
        e.setPostnomEnfant(changes.getPostnomEnfant());
        e.setPrenomEnfant(changes.getPrenomEnfant());
        e.setDatenaiss(changes.getDatenaiss());
        e.setGenre(changes.getGenre());
        e.setCategorie(changes.getCategorie());
        e.setStat(changes.getStat());
        e.setReference(changes.getReference());
        e.setPhoto(changes.getPhoto());

        // L’entité est managée (@Transactional) : les changements seront flush au commit
        return e;
    }

    public AttachesAgent getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enfant introuvable: " + id));
    }

    /** Supprime l'enfant après avoir supprimé les PEC qui le référencent (contrainte FK). */
    public void delete(Long id) {
        AttachesAgent e = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enfant introuvable: " + id));
        priseEnChargeRepo.deleteByEnfant_Id(id);
        repo.delete(e);
    }

    private boolean existsChildForAgent(String nom, String postnom, String prenom, Date naissance, Long agentId) {
        return repo.existsByAgent_IdAndNomEnfantIgnoreCaseAndPostnomEnfantIgnoreCaseAndPrenomEnfantIgnoreCaseAndDatenaiss(
                agentId, nom, postnom, prenom, naissance
        );
    }

    private boolean existsChildForSenateur(String nom, String postnom, String prenom, Date naissance, Long senateurId) {
        return repo.existsBySenateur_IdAndNomEnfantIgnoreCaseAndPostnomEnfantIgnoreCaseAndPrenomEnfantIgnoreCaseAndDatenaiss(
                senateurId, nom, postnom, prenom, naissance
        );
    }

    private static boolean notEq(Object a, Object b) {
        return (a == null && b != null) || (a != null && !a.equals(b));
    }
}
