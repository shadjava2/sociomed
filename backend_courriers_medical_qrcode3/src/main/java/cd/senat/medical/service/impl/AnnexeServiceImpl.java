package cd.senat.medical.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.senat.medical.dto.AnnexeDTO;
import cd.senat.medical.entity.Annexe;
import cd.senat.medical.entity.Courrier;
import cd.senat.medical.repository.AnnexeRepository;
import cd.senat.medical.repository.CourrierRepository;
import cd.senat.medical.service.AnnexeService;

import java.util.List;

@Service @Transactional
public class AnnexeServiceImpl implements AnnexeService {

    private final AnnexeRepository annexeRepository;
    private final CourrierRepository courrierRepository;

    public AnnexeServiceImpl(AnnexeRepository annexeRepository, CourrierRepository courrierRepository) {
        this.annexeRepository = annexeRepository;
        this.courrierRepository = courrierRepository;
    }

    @Override @Transactional(readOnly = true)
    public List<AnnexeDTO> listByCourrier(Long courrierId) {
        return annexeRepository.findByCourrierIdOrderByNomAsc(courrierId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public AnnexeDTO addToCourrier(Long courrierId, AnnexeDTO dto) {
        Courrier c = courrierRepository.findById(courrierId)
                .orElseThrow(() -> new EntityNotFoundException("Courrier introuvable: " + courrierId));
        Annexe a = new Annexe(dto.getNom(), dto.getTaille(), dto.getType(), dto.getUrl());
        a.setCourrier(c);
        Annexe saved = annexeRepository.save(a);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void removeFromCourrier(Long courrierId, Long annexeId) {
        int affected = annexeRepository.hardDeleteByIdAndCourrierId(courrierId, annexeId);
        if (affected == 0) {
            throw new EntityNotFoundException("Annexe " + annexeId + " non trouvée pour courrier " + courrierId);
        }
    }

    private AnnexeDTO toDto(Annexe a) {
        AnnexeDTO dto = new AnnexeDTO(a.getNom(), a.getTaille(), a.getType(), a.getUrl());
        dto.setId(a.getId());
        return dto;
    }
}
