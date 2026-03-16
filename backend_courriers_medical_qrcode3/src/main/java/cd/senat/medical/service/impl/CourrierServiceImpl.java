package cd.senat.medical.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.senat.medical.dto.AnnexeDTO;
import cd.senat.medical.dto.CourrierDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.Annexe;
import cd.senat.medical.entity.Courrier;
import cd.senat.medical.entity.TypeCourrier;
import cd.senat.medical.repository.AnnexeRepository;
import cd.senat.medical.repository.CourrierRepository;
import cd.senat.medical.service.CourrierService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourrierServiceImpl implements CourrierService {

    private final CourrierRepository courrierRepository;
    private final AnnexeRepository annexeRepository;
    private final ModelMapper modelMapper;

    @PersistenceContext
    private EntityManager em; // ✅ pour nettoyer la session après un échec

    public CourrierServiceImpl(CourrierRepository courrierRepository,
                               AnnexeRepository annexeRepository,
                               ModelMapper modelMapper) {
        this.courrierRepository = courrierRepository;
        this.annexeRepository = annexeRepository;
        this.modelMapper = modelMapper;
    }

    /* ==================== READS ==================== */

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourrierDTO> getAllCourriers(
            String searchTerm,
            TypeCourrier typeCourrier,
            Boolean traite,
            String priorite,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable) {

        Page<Courrier> courriersPage = courrierRepository.findWithFilters(
                searchTerm, typeCourrier, traite, priorite, dateDebut, dateFin, pageable
        );

        List<CourrierDTO> contenu = courriersPage.getContent()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                contenu,
                courriersPage.getTotalPages(),
                courriersPage.getTotalElements(),
                courriersPage.getNumber(),
                courriersPage.getSize(),
                courriersPage.isFirst(),
                courriersPage.isLast(),
                courriersPage.isEmpty()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CourrierDTO getCourrierById(Long id) {
        Courrier courrier = courrierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Courrier non trouvé avec l'ID: " + id));
        return convertToDTO(courrier);
    }

    @Override
    @Transactional(readOnly = true)
    public CourrierDTO getCourrierByRef(String ref) {
        Courrier courrier = courrierRepository.findByRef(ref);
        if (courrier == null) {
            throw new RuntimeException("Courrier non trouvé avec la référence: " + ref);
        }
        return convertToDTO(courrier);
    }

    /* ==================== CREATION ==================== */

    @Override
    public CourrierDTO createCourrier(CourrierDTO courrierDTO) {
        // Normaliser les champs texte en MAJUSCULES (courrier + annexes)
        normalizeCourrierDTO(courrierDTO);

        Courrier courrier = convertToEntity(courrierDTO);

        // La référence est générée côté entité (@PrePersist) — on ne garde jamais celle du client
        courrier.setRef(null);

        // Type requis pour la génération (préfixe ref)
        if (courrier.getTypeCourrier() == null) {
            throw new IllegalArgumentException("Le typeCourrier est obligatoire pour générer la référence.");
        }

        // Annee & sequence
        final int annee = LocalDateTime.now().getYear();
        final TypeCourrier type = courrier.getTypeCourrier();

        int currentMax = courrierRepository.findMaxSequenceForYearAndType(annee, type);
        int nextSeq = currentMax + 1;

        courrier.setAnnee(annee);
        courrier.setSequence(nextSeq);

        try {
            courrier = courrierRepository.save(courrier);
        } catch (DataIntegrityViolationException e) {
            // ⚠️ L'insert a échoué (collision). La session Hibernate est en état d'erreur.
            // On DOIT nettoyer avant tout autre appel JPA pour éviter "don't flush the Session..."
            em.clear();

            // On recalcule proprement la séquence, puis on régénère la ref (@PrePersist)
            int retryMax = courrierRepository.findMaxSequenceForYearAndType(annee, type);
            int retrySeq = retryMax + 1;
            courrier.setSequence(retrySeq);
            courrier.setRef(null); // ✅ force la régénération de la référence

            // 2e tentative
            courrier = courrierRepository.save(courrier);
        }

        // Annexes
        if (courrierDTO.getAnnexes() != null && !courrierDTO.getAnnexes().isEmpty()) {
            saveAnnexes(courrier, courrierDTO.getAnnexes());
        }

        return convertToDTO(courrier);
    }

    /* ==================== MISE A JOUR ==================== */

    @Override
    public CourrierDTO updateCourrier(Long id, CourrierDTO courrierDTO) {
        Courrier existing = courrierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Courrier non trouvé avec l'ID: " + id));

        // Normaliser en MAJUSCULES
        normalizeCourrierDTO(courrierDTO);

        // Option métier : autoriser le changement de type (la ref ne change pas)
        if (courrierDTO.getTypeCourrier() != null
                && !Objects.equals(existing.getTypeCourrier(), courrierDTO.getTypeCourrier())) {
            existing.setTypeCourrier(courrierDTO.getTypeCourrier());
        }

        existing.setPriorite(courrierDTO.getPriorite());
        existing.setExpediteur(courrierDTO.getExpediteur());
        existing.setDestinataire(courrierDTO.getDestinataire());
        existing.setObjet(courrierDTO.getObjet());
        existing.setContenu(courrierDTO.getContenu());
        existing.setDateReception(courrierDTO.getDateReception());
        existing.setTraite(courrierDTO.getTraite());
        existing.setUrgent(courrierDTO.getUrgent());

        existing = courrierRepository.save(existing);

        if (courrierDTO.getAnnexes() != null) {
            updateAnnexes(existing, courrierDTO.getAnnexes());
        }

        return convertToDTO(existing);
    }

    /* ==================== SUPPRESSION & STATS ==================== */

    @Override
    public void deleteCourrier(Long id) {
        if (!courrierRepository.existsById(id)) {
            throw new RuntimeException("Courrier non trouvé avec l'ID: " + id);
        }
        courrierRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTraite(Boolean traite) {
        return courrierRepository.countByTraite(traite);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTypeCourrier(TypeCourrier typeCourrier) {
        return courrierRepository.countByTypeCourrier(typeCourrier);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUrgent(Boolean urgent) {
        return courrierRepository.countByUrgent(urgent);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCreatedToday() {
        return courrierRepository.countCreatedToday();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotal() {
        return courrierRepository.count();
    }

    /* ==================== HELPERS ==================== */

    private CourrierDTO convertToDTO(Courrier courrier) {
        CourrierDTO dto = modelMapper.map(courrier, CourrierDTO.class);

        if (courrier.getAnnexes() != null) {
            List<AnnexeDTO> annexeDTOs = courrier.getAnnexes().stream()
                    .map(annexe -> modelMapper.map(annexe, AnnexeDTO.class))
                    .collect(Collectors.toList());
            dto.setAnnexes(annexeDTOs);
        }
        return dto;
    }

    private Courrier convertToEntity(CourrierDTO dto) {
        Courrier entity = modelMapper.map(dto, Courrier.class);
        // On ne persiste jamais une ref client
        entity.setRef(null);
        return entity;
    }

    /** Normalisation en MAJUSCULES des champs texte (courrier + annexes). */
    private void normalizeCourrierDTO(CourrierDTO dto) {
        if (dto == null) return;

        if (dto.getPriorite() != null) dto.setPriorite(dto.getPriorite().toUpperCase());
        if (dto.getExpediteur() != null) dto.setExpediteur(dto.getExpediteur().toUpperCase());
        if (dto.getDestinataire() != null) dto.setDestinataire(dto.getDestinataire().toUpperCase());
        if (dto.getObjet() != null) dto.setObjet(dto.getObjet().toUpperCase());
        if (dto.getContenu() != null) dto.setContenu(dto.getContenu().toUpperCase());

        if (dto.getAnnexes() != null) {
            dto.getAnnexes().forEach(a -> {
                if (a.getType() != null) a.setType(a.getType().toUpperCase());
            });
        }
    }

    private void saveAnnexes(Courrier courrier, List<AnnexeDTO> annexeDTOs) {
        List<Annexe> annexes = annexeDTOs.stream()
                .map(dto -> {
                    Annexe annexe = modelMapper.map(dto, Annexe.class);
                    annexe.setCourrier(courrier);
                    return annexe;
                })
                .collect(Collectors.toList());
        annexeRepository.saveAll(annexes);
    }

    private void updateAnnexes(Courrier courrier, List<AnnexeDTO> annexeDTOs) {
        // Ici on ne supprime pas explicitement les anciennes annexes
        saveAnnexes(courrier, annexeDTOs);
    }
}
