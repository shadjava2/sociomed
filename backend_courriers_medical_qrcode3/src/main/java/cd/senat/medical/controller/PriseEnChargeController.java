package cd.senat.medical.controller;

import cd.senat.medical.dto.PriseEnChargeDTO;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.service.PriseEnChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/pec")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PriseEnChargeController {

    private static final Logger log = LoggerFactory.getLogger(PriseEnChargeController.class);
    private final PriseEnChargeService service;

    // ============== CREATE ==============
    @PostMapping(consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody @Valid PriseEnChargeDTO.CreateRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur create PEC", e);
            return serverError();
        }
    }

    // ============== UPDATE ==============
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody @Valid PriseEnChargeDTO.UpdateRequest req) {
        try {
            return ResponseEntity.ok(service.update(id, req));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur update PEC", e);
            return serverError();
        }
    }

    // ============== READ (Detail) ==============
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get PEC", e);
            return serverError();
        }
    }

    // ============== DELETE ==============
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur delete PEC", e);
            return serverError();
        }
    }

    /* ======================= ROUTES JSON ======================= */

    @GetMapping(value = "/listing/by-hopital", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listByHopital(
            @RequestParam(required = false) Long hopitalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Date fromDate = (from != null)
                    ? Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : null;

            Date toDate = (to != null)
                    ? Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : null;

            var filter = new PriseEnChargeDTO.ListFilter(hopitalId, fromDate, toDate);
            var pageable = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)));

            return ResponseEntity.ok(service.listByHopital(filter, pageable));
        } catch (Exception e) {
            log.error("Erreur listByHopital", e);
            return serverError();
        }
    }

    @GetMapping(value = "/stats/hopital", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> statsParHopital(@RequestParam(name = "month") String yearMonth) {
        try {
            var ym = LocalDate.parse(yearMonth + "-01");
            return ResponseEntity.ok(service.statsPecParHopital(ym));
        } catch (Exception e) {
            log.error("Erreur statsParHopital", e);
            return serverError();
        }
    }

    @GetMapping(value = "/stats/categorie", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> statsParCategorie(@RequestParam(name = "month") String yearMonth) {
        try {
            var ym = LocalDate.parse(yearMonth + "-01");
            return ResponseEntity.ok(service.statsPecParCategorie(ym));
        } catch (Exception e) {
            log.error("Erreur statsParCategorie", e);
            return serverError();
        }
    }

    /* ======================= Helpers erreurs JSON ======================= */

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