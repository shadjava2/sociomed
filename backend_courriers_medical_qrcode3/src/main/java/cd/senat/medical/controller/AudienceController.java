package cd.senat.medical.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cd.senat.medical.dto.AudienceDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.StatutAudience;
import cd.senat.medical.entity.TypeAudience;
import cd.senat.medical.service.AudienceService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audiences")
@CrossOrigin(origins = "*")
public class AudienceController {
    
    @Autowired
    private AudienceService audienceService;
    
    @GetMapping
    public ResponseEntity<PageResponse<AudienceDTO>> getAllAudiences(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) StatutAudience statut,
            @RequestParam(required = false) TypeAudience typeAudience,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateHeure") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<AudienceDTO> response = audienceService.getAllAudiences(
                searchTerm, statut, typeAudience, dateDebut, dateFin, pageable
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AudienceDTO> getAudienceById(@PathVariable Long id) {
        AudienceDTO audience = audienceService.getAudienceById(id);
        return ResponseEntity.ok(audience);
    }
    
    @PostMapping
    public ResponseEntity<AudienceDTO> createAudience(@Valid @RequestBody AudienceDTO audienceDTO) {
        AudienceDTO createdAudience = audienceService.createAudience(audienceDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAudience);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AudienceDTO> updateAudience(
            @PathVariable Long id, 
            @Valid @RequestBody AudienceDTO audienceDTO) {
        AudienceDTO updatedAudience = audienceService.updateAudience(id, audienceDTO);
        return ResponseEntity.ok(updatedAudience);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAudience(@PathVariable Long id) {
        audienceService.deleteAudience(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<List<AudienceDTO>> getUpcomingAudiences() {
        List<AudienceDTO> audiences = audienceService.getUpcomingAudiences();
        return ResponseEntity.ok(audiences);
    }
    
    @GetMapping("/today")
    public ResponseEntity<List<AudienceDTO>> getTodayAudiences() {
        List<AudienceDTO> audiences = audienceService.getTodayAudiences();
        return ResponseEntity.ok(audiences);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAudiences", audienceService.countTotal());
        stats.put("audiencesPlanifiees", audienceService.countByStatut(StatutAudience.PLANIFIEE));
        stats.put("audiencesEnCours", audienceService.countByStatut(StatutAudience.EN_COURS));
        stats.put("audiencesTerminees", audienceService.countByStatut(StatutAudience.TERMINEE));
        stats.put("audiencesAnnulees", audienceService.countByStatut(StatutAudience.ANNULEE));
        stats.put("audiencesPubliques", audienceService.countByTypeAudience(TypeAudience.PUBLIQUE));
        stats.put("audiencesPrivees", audienceService.countByTypeAudience(TypeAudience.PRIVEE));
        stats.put("audiencesCommissions", audienceService.countByTypeAudience(TypeAudience.COMMISSION));
        stats.put("audiencesCreatedToday", audienceService.countCreatedToday());
        
        return ResponseEntity.ok(stats);
    }
}