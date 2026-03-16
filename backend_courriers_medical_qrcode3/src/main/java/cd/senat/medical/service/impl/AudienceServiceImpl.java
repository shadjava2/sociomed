package cd.senat.medical.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.senat.medical.dto.AudienceDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.dto.ParticipantDTO;
import cd.senat.medical.entity.Audience;
import cd.senat.medical.entity.Participant;
import cd.senat.medical.entity.StatutAudience;
import cd.senat.medical.entity.TypeAudience;
import cd.senat.medical.repository.AudienceRepository;
import cd.senat.medical.repository.ParticipantRepository;
import cd.senat.medical.service.AudienceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AudienceServiceImpl implements AudienceService {

    @Autowired
    private AudienceRepository audienceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public PageResponse<AudienceDTO> getAllAudiences(
            String searchTerm,
            StatutAudience statut,
            TypeAudience typeAudience,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable) {

        Page<Audience> audiencesPage = audienceRepository.findWithFilters(
                searchTerm, statut, typeAudience, dateDebut, dateFin, pageable
        );

        List<AudienceDTO> audienceDTOs = audiencesPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                audienceDTOs,
                audiencesPage.getTotalPages(),
                audiencesPage.getTotalElements(),
                audiencesPage.getNumber(),
                audiencesPage.getSize(),
                audiencesPage.isFirst(),
                audiencesPage.isLast(),
                audiencesPage.isEmpty()
        );
    }

    @Override
    public AudienceDTO getAudienceById(Long id) {
        Audience audience = audienceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audience non trouvée avec l'ID: " + id));
        return convertToDTO(audience);
    }

    @Override
    public AudienceDTO createAudience(AudienceDTO audienceDTO) {
        // Normaliser en MAJUSCULE avant persistance
        normalizeAudienceDTO(audienceDTO);

        Audience audience = convertToEntity(audienceDTO);
        audience = audienceRepository.save(audience);

        // Sauvegarder les participants
        if (audienceDTO.getParticipants() != null && !audienceDTO.getParticipants().isEmpty()) {
            saveParticipants(audience, audienceDTO.getParticipants());
        }

        return convertToDTO(audience);
    }

    @Override
    public AudienceDTO updateAudience(Long id, AudienceDTO audienceDTO) {
        Audience existingAudience = audienceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audience non trouvée avec l'ID: " + id));

        // Normaliser en MAJUSCULE avant mise à jour
        normalizeAudienceDTO(audienceDTO);

        // Mettre à jour les champs
        existingAudience.setTitre(audienceDTO.getTitre());
        existingAudience.setDescription(audienceDTO.getDescription());
        existingAudience.setDateHeure(audienceDTO.getDateHeure());
        existingAudience.setDuree(audienceDTO.getDuree());
        existingAudience.setStatut(audienceDTO.getStatut());
        existingAudience.setTypeAudience(audienceDTO.getTypeAudience());
        existingAudience.setOrganisateur(audienceDTO.getOrganisateur());

        existingAudience = audienceRepository.save(existingAudience);

        // Mettre à jour les participants
        if (audienceDTO.getParticipants() != null) {
            updateParticipants(existingAudience, audienceDTO.getParticipants());
        }

        return convertToDTO(existingAudience);
    }

    @Override
    public void deleteAudience(Long id) {
        if (!audienceRepository.existsById(id)) {
            throw new RuntimeException("Audience non trouvée avec l'ID: " + id);
        }
        audienceRepository.deleteById(id);
    }

    @Override
    public List<AudienceDTO> getUpcomingAudiences() {
        List<Audience> audiences = audienceRepository.findUpcomingAudiences(LocalDateTime.now());
        return audiences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AudienceDTO> getTodayAudiences() {
        List<Audience> audiences = audienceRepository.findTodayAudiences();
        return audiences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatut(StatutAudience statut) {
        return audienceRepository.countByStatut(statut);
    }

    @Override
    public long countByTypeAudience(TypeAudience typeAudience) {
        return audienceRepository.countByTypeAudience(typeAudience);
    }

    @Override
    public long countCreatedToday() {
        return audienceRepository.countCreatedToday();
    }

    @Override
    public long countTotal() {
        return audienceRepository.count();
    }

    // ======================== Helpers mapping ========================

    private AudienceDTO convertToDTO(Audience audience) {
        AudienceDTO dto = modelMapper.map(audience, AudienceDTO.class);

        if (audience.getParticipants() != null) {
            List<ParticipantDTO> participantDTOs = audience.getParticipants().stream()
                    .map(participant -> modelMapper.map(participant, ParticipantDTO.class))
                    .collect(Collectors.toList());
            dto.setParticipants(participantDTOs);
        }

        return dto;
    }

    private Audience convertToEntity(AudienceDTO dto) {
        return modelMapper.map(dto, Audience.class);
    }

    /**
     * Force les champs texte à être en MAJUSCULES (audience + participants).
     */
    private void normalizeAudienceDTO(AudienceDTO dto) {
        if (dto == null) return;

        if (dto.getTitre() != null) dto.setTitre(dto.getTitre().toUpperCase());
        if (dto.getDescription() != null) dto.setDescription(dto.getDescription().toUpperCase());
        if (dto.getOrganisateur() != null) dto.setOrganisateur(dto.getOrganisateur().toUpperCase());
        // Si le DTO garde encore "lieu" côté backend un jour, penser à le traiter ici.

        if (dto.getParticipants() != null) {
            dto.getParticipants().forEach(p -> {
                if (p.getNom() != null) p.setNom(p.getNom().toUpperCase());
                if (p.getPrenom() != null) p.setPrenom(p.getPrenom().toUpperCase());
                if (p.getFonction() != null) p.setFonction(p.getFonction().toUpperCase());
                if (p.getEmail() != null) p.setEmail(p.getEmail().toUpperCase());
                if (p.getTelephone() != null) p.setTelephone(p.getTelephone().toUpperCase());
            });
        }
    }

    // ======================== Participants ========================

    private void saveParticipants(Audience audience, List<ParticipantDTO> participantDTOs) {
        List<Participant> participants = participantDTOs.stream()
                .map(dto -> {
                    Participant participant = modelMapper.map(dto, Participant.class);
                    participant.setAudience(audience);
                    return participant;
                })
                .collect(Collectors.toList());

        participantRepository.saveAll(participants);
    }

    private void updateParticipants(Audience audience, List<ParticipantDTO> participantDTOs) {
        // Supprimer les anciens participants qui ne sont plus présents
        List<Long> newParticipantIds = participantDTOs.stream()
                .map(ParticipantDTO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (!newParticipantIds.isEmpty()) {
            participantRepository.deleteByAudienceIdAndIdNotIn(audience.getId(), newParticipantIds);
        } else {
            // Si aucun ID fourni, on supprime tous les existants pour repartir de zéro
            participantRepository.deleteById(audience.getId());
        }

        // Ré-enregistrer/ajouter les participants (si l'ID existe, JPA fera un merge ; sinon insert)
        saveParticipants(audience, participantDTOs);
    }
}
