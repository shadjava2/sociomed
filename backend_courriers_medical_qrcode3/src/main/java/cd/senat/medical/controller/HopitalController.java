// src/main/java/cd/senat/medical/controller/HopitalController.java
package cd.senat.medical.controller;

import cd.senat.medical.dto.HopitalDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.CategorieHopital;
import cd.senat.medical.entity.Hopital;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.service.HopitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hopitaux")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HopitalController {

    private static final Logger log = LoggerFactory.getLogger(HopitalController.class);

    private final HopitalService service;

    // LISTE paginée (Summary)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) CategorieHopital categorie,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);

            Page<Hopital> p = service.search(q, actif, categorie, pageable);
            PageResponse<HopitalDTO.Summary> resp = PageResponse.from(p).map(HopitalDTO::toSummary);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Erreur liste hopitaux", e);
            return serverError();
        }
    }

    @GetMapping(value = "/actifs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listActifs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);

            Page<Hopital> p = service.listActifs(pageable);
            PageResponse<HopitalDTO.Summary> resp = PageResponse.from(p).map(HopitalDTO::toSummary);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Erreur liste hopitaux actifs", e);
            return serverError();
        }
    }

    // CREATE (DTO)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody @Valid HopitalDTO.CreateRequest req) {
        try {
            Hopital h = new Hopital();
            HopitalDTO.apply(h, req);
            Hopital saved = service.create(h);
            return ResponseEntity.status(HttpStatus.CREATED).body(HopitalDTO.toDetail(saved));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur create hopital", e);
            return serverError();
        }
    }

    // UPDATE (DTO partiel)
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody @Valid HopitalDTO.UpdateRequest req) {
        try {
            Hopital current = service.getById(id);
            HopitalDTO.apply(current, req);
            Hopital updated = service.update(id, current);
            return ResponseEntity.ok(HopitalDTO.toDetail(updated));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur update hopital", e);
            return serverError();
        }
    }

    // READ (Detail)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            Hopital h = service.getById(id);
            return ResponseEntity.ok(HopitalDTO.toDetail(h));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get hopital by id", e);
            return serverError();
        }
    }

    @GetMapping(value = "/code/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getByCode(@PathVariable String code) {
        try {
            Hopital h = service.getByCode(code);
            return ResponseEntity.ok(HopitalDTO.toDetail(h));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get hopital by code", e);
            return serverError();
        }
    }

    // ACTIVER / DÉSACTIVER
    @PatchMapping(value = "/{id}/actif", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setActif(@PathVariable Long id, @RequestParam boolean value) {
        try {
            Hopital h = service.setActif(id, value);
            return ResponseEntity.ok(HopitalDTO.toDetail(h));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur set actif hopital", e);
            return serverError();
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur delete hopital", e);
            return serverError();
        }
    }

    // Helpers erreurs
    private ResponseEntity<Map<String, Object>> conflict(String msg) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
    }
    private ResponseEntity<Map<String, Object>> notFound(String msg) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
    }
    private ResponseEntity<Map<String, Object>> serverError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erreur serveur"));
    }
}
