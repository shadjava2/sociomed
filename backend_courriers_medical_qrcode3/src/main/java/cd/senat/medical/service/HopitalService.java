package cd.senat.medical.service;

import cd.senat.medical.entity.CategorieHopital;
import cd.senat.medical.entity.Hopital;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.repository.HopitalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class HopitalService {

    private final HopitalRepository repo;

    // =============== CREATE ===============
    public Hopital create(Hopital h) {
        if (h.getCode() != null && repo.existsByCodeIgnoreCase(h.getCode())) {
            throw new BusinessException("Code hôpital déjà utilisé");
        }
        try {
            return repo.save(h);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Contrainte d’unicité violée (code ou nom + ville)");
        }
    }

    // =============== UPDATE ===============
    public Hopital update(Long id, Hopital changes) {
        Hopital current = getById(id);

        if (changes.getCode() != null
                && !changes.getCode().equalsIgnoreCase(current.getCode())
                && repo.existsByCodeIgnoreCase(changes.getCode())) {
            throw new BusinessException("Code hôpital déjà utilisé");
        }

        current.setCode(changes.getCode());
        current.setNom(changes.getNom());
        current.setCategorie(changes.getCategorie());
        current.setAdresse(changes.getAdresse());
        current.setCommune(changes.getCommune());
        current.setVille(changes.getVille());
        current.setProvince(changes.getProvince());
        current.setPays(changes.getPays());
        current.setContactNom(changes.getContactNom());
        current.setContactTelephone(changes.getContactTelephone());
        current.setEmail(changes.getEmail());
        current.setSiteWeb(changes.getSiteWeb());
        current.setActif(changes.getActif());
        current.setNumeroConvention(changes.getNumeroConvention());
        current.setConventionDebut(changes.getConventionDebut());
        current.setConventionFin(changes.getConventionFin());
        current.setPlafondMensuel(changes.getPlafondMensuel());

        try {
            return current; // JPA dirty checking
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Contrainte d’unicité violée (code ou nom + ville)");
        }
    }

    // =============== READS ===============
    public Hopital getById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Hôpital introuvable: " + id));
    }

    public Hopital getByCode(String code) {
        return repo.findByCodeIgnoreCase(code).orElseThrow(() ->
                new ResourceNotFoundException("Hôpital introuvable (code): " + code));
    }

    public Page<Hopital> search(String q, Boolean actif, CategorieHopital categorie, Pageable pageable) {
        return repo.search(q, actif, categorie, pageable);
    }

    public Page<Hopital> listActifs(Pageable pageable) {
        return repo.findByActifTrue(pageable);
    }

    // =============== DELETE ===============
    public void delete(Long id) {
        Hopital h = getById(id);
        repo.delete(h);
    }

    // =============== ACTIVATION ===============
    public Hopital setActif(Long id, boolean actif) {
        Hopital h = getById(id);
        h.setActif(actif);
        return h;
    }

    // =============== STATS ===============
    public List<HopitalRepository.StatsGenreParHopital> statsGenreTousHopitaux(Date from, Date to) {
        return repo.statsParHopitalEtGenre(from, to);
    }

    public List<HopitalRepository.StatsGenreParHopital> statsGenrePourHopital(Long hopitalId, Date from, Date to) {
        return repo.statsParGenrePourHopital(hopitalId, from, to);
    }
}
