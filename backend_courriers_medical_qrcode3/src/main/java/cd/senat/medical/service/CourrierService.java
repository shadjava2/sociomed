package cd.senat.medical.service;

import org.springframework.data.domain.Pageable;

import cd.senat.medical.dto.CourrierDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.TypeCourrier;

import java.time.LocalDateTime;

public interface CourrierService {
    
    PageResponse<CourrierDTO> getAllCourriers(
            String searchTerm,
            TypeCourrier typeCourrier,
            Boolean traite,
            String priorite,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Pageable pageable
    );
    
    CourrierDTO getCourrierById(Long id);
    
    CourrierDTO getCourrierByRef(String ref);
    
    CourrierDTO createCourrier(CourrierDTO courrierDTO);
    
    CourrierDTO updateCourrier(Long id, CourrierDTO courrierDTO);
    
    void deleteCourrier(Long id);
    
    // Statistiques
    long countByTraite(Boolean traite);
    long countByTypeCourrier(TypeCourrier typeCourrier);
    long countByUrgent(Boolean urgent);
    long countCreatedToday();
    
    // Total count method
    long countTotal();
    
    // Génération de référence
   // String generateCourrierRef(TypeCourrier typeCourrier);
}