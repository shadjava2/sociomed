package cd.senat.medical.service;

import cd.senat.medical.entity.DocumentPersonne;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.repository.DocumentPersonneRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class DocumentPersonneService {

    private final DocumentPersonneRepository repo;

    public DocumentPersonneService(DocumentPersonneRepository repo) {
        this.repo = repo;
    }

    public DocumentPersonne create(DocumentPersonne d) {
        // Règle 1: storedFilename doit être unique (file system + DB)
        if (repo.existsByStoredFilenameIgnoreCase(d.getStoredFilename())) {
            throw new BusinessException("Fichier déjà enregistré (storedFilename)");
        }

        // Règle 2 (optionnelle mais utile) : éviter doublon logique par propriétaire + type + originalFilename
        if (d.getAgent() != null &&
            repo.existsByAgent_IdAndTypeAndOriginalFilenameIgnoreCase(d.getAgent().getId(), d.getType(), d.getOriginalFilename())) {
            throw new BusinessException("Document déjà présent pour cet agent (type + nom d'origine)");
        }
        if (d.getSenateur() != null &&
            repo.existsBySenateur_IdAndTypeAndOriginalFilenameIgnoreCase(d.getSenateur().getId(), d.getType(), d.getOriginalFilename())) {
            throw new BusinessException("Document déjà présent pour ce sénateur (type + nom d'origine)");
        }

        try {
            return repo.save(d);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Contrainte d’unicité violée pour DOCUMENT");
        }
    }

    public DocumentPersonne update(Long id, DocumentPersonne changes) {
        DocumentPersonne d = getById(id);

        // Si storedFilename change → recheck
        if (changes.getStoredFilename() != null && !changes.getStoredFilename().equals(d.getStoredFilename()) &&
            repo.existsByStoredFilenameIgnoreCase(changes.getStoredFilename())) {
            throw new BusinessException("storedFilename déjà utilisé");
        }

        // Si original/type/proprio changent → recheck doublon logique
        boolean checkOwnerTypeOriginal = false;
        if (changes.getType() != null && changes.getType() != d.getType()) checkOwnerTypeOriginal = true;
        if (changes.getOriginalFilename() != null && !changes.getOriginalFilename().equals(d.getOriginalFilename())) checkOwnerTypeOriginal = true;
        if ((changes.getAgent() != null && d.getAgent() == null) || (changes.getAgent() == null && d.getAgent() != null)) checkOwnerTypeOriginal = true;
        if ((changes.getSenateur() != null && d.getSenateur() == null) || (changes.getSenateur() == null && d.getSenateur() != null)) checkOwnerTypeOriginal = true;

        if (checkOwnerTypeOriginal) {
            Long agentId = (changes.getAgent() != null) ? changes.getAgent().getId() : (d.getAgent() != null ? d.getAgent().getId() : null);
            Long senId   = (changes.getSenateur() != null) ? changes.getSenateur().getId() : (d.getSenateur() != null ? d.getSenateur().getId() : null);
            var type = (changes.getType() != null) ? changes.getType() : d.getType();
            var orig = (changes.getOriginalFilename() != null) ? changes.getOriginalFilename() : d.getOriginalFilename();

            if (agentId != null &&
                repo.existsByAgent_IdAndTypeAndOriginalFilenameIgnoreCase(agentId, type, orig)) {
                throw new BusinessException("Document déjà présent pour cet agent (type + nom d'origine)");
            }
            if (senId != null &&
                repo.existsBySenateur_IdAndTypeAndOriginalFilenameIgnoreCase(senId, type, orig)) {
                throw new BusinessException("Document déjà présent pour ce sénateur (type + nom d'origine)");
            }
        }

        // apply
        d.setType(changes.getType());
        d.setOriginalFilename(changes.getOriginalFilename());
        d.setStoredFilename(changes.getStoredFilename());
        d.setRelativeDirectory(changes.getRelativeDirectory());
        d.setContentType(changes.getContentType());
        d.setSize(changes.getSize());
        d.setSha256(changes.getSha256());
        d.setLabel(changes.getLabel());
        d.setActif(changes.getActif());

        // transfert éventuel de propriétaire (respecter la contrainte XOR côté entité)
        d.setAgent(changes.getAgent());
        d.setSenateur(changes.getSenateur());
        return d;
    }

    public void delete(Long id) { repo.deleteById(id); }

    public DocumentPersonne getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable: " + id));
    }
}
