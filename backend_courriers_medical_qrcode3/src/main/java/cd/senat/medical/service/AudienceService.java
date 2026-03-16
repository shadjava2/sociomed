package cd.senat.medical.service;

import org.springframework.data.domain.Pageable;

import cd.senat.medical.dto.AudienceDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.StatutAudience;
import cd.senat.medical.entity.TypeAudience;

import java.time.LocalDateTime;
import java.util.List;

public interface AudienceService {
    
    PageResponse<AudienceDTO> getAllAudiences(
            String searchTerm,
            StatutAudience statut,
            TypeAudience typeAudience,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable
    );
    
    AudienceDTO getAudienceById(Long id);
    
    AudienceDTO createAudience(AudienceDTO audienceDTO);
    
    AudienceDTO updateAudience(Long id, AudienceDTO audienceDTO);
    
    void deleteAudience(Long id);
    
    List<AudienceDTO> getUpcomingAudiences();
    
    List<AudienceDTO> getTodayAudiences();
    
    // Statistiques
    long countByStatut(StatutAudience statut);
    long countByTypeAudience(TypeAudience typeAudience);
    long countCreatedToday();
    long countTotal();
}