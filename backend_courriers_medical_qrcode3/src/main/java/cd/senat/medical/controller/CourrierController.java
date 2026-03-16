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

import cd.senat.medical.dto.CourrierDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.TypeCourrier;
import cd.senat.medical.service.CourrierService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/courriers")
@CrossOrigin(origins = "*")
public class CourrierController {
    
    @Autowired
    private CourrierService courrierService;
    
    @GetMapping
    public ResponseEntity<PageResponse<CourrierDTO>> getAllCourriers(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) TypeCourrier typeCourrier,
            @RequestParam(required = false) Boolean traite,
            @RequestParam(required = false) String priorite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<CourrierDTO> response = courrierService.getAllCourriers(
                searchTerm, typeCourrier, traite, priorite, dateDebut, dateFin, pageable
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CourrierDTO> getCourrierById(@PathVariable Long id) {
        CourrierDTO courrier = courrierService.getCourrierById(id);
        return ResponseEntity.ok(courrier);
    }
    
    @GetMapping("/ref/{ref}")
    public ResponseEntity<CourrierDTO> getCourrierByRef(@PathVariable String ref) {
        CourrierDTO courrier = courrierService.getCourrierByRef(ref);
        return ResponseEntity.ok(courrier);
    }
    
    @PostMapping
    public ResponseEntity<CourrierDTO> createCourrier(@Valid @RequestBody CourrierDTO courrierDTO) {
        CourrierDTO createdCourrier = courrierService.createCourrier(courrierDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourrier);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CourrierDTO> updateCourrier(
            @PathVariable Long id, 
            @Valid @RequestBody CourrierDTO courrierDTO) {
        CourrierDTO updatedCourrier = courrierService.updateCourrier(id, courrierDTO);
        return ResponseEntity.ok(updatedCourrier);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourrier(@PathVariable Long id) {
        courrierService.deleteCourrier(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCourriers", courrierService.countTotal());
        stats.put("courriersTraites", courrierService.countByTraite(true));
        stats.put("courriersNonTraites", courrierService.countByTraite(false));
        stats.put("courriersRecus", courrierService.countByTypeCourrier(TypeCourrier.RECU));
        stats.put("courriersEnvoyes", courrierService.countByTypeCourrier(TypeCourrier.ENVOYE));
        stats.put("courriersUrgents", courrierService.countByUrgent(true));
        stats.put("courriersCreatedToday", courrierService.countCreatedToday());
        return ResponseEntity.ok(stats);
    }
    

}