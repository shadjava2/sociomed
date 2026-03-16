package cd.senat.medical.controller;

import cd.senat.medical.entity.PieceJointe;
import cd.senat.medical.entity.PieceJointeType;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.service.PieceJointeService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PieceJointeController {

    private static final Logger log = LoggerFactory.getLogger(PieceJointeController.class);

    private final PieceJointeService service;

    // ========================= CREATE =========================

    /** Créer une PJ (PDF) pour un Agent */
    @PostMapping(value = "/agents/{agentId}/pieces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createForAgent(@PathVariable Long agentId,
                                            @RequestParam("type") PieceJointeType type,
                                            @RequestParam(value = "titre", required = false) String titre,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestPart("file") MultipartFile file) {
        try {
            PieceJointe pj = service.createForAgent(agentId, type, titre, description, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(pj);
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur create PJ agent", e);
            return serverError();
        }
    }

    /** Créer une PJ (PDF) pour un Sénateur */
    @PostMapping(value = "/senateurs/{senateurId}/pieces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createForSenateur(@PathVariable Long senateurId,
                                               @RequestParam("type") PieceJointeType type,
                                               @RequestParam(value = "titre", required = false) String titre,
                                               @RequestParam(value = "description", required = false) String description,
                                               @RequestPart("file") MultipartFile file) {
        try {
            PieceJointe pj = service.createForSenateur(senateurId, type, titre, description, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(pj);
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur create PJ senateur", e);
            return serverError();
        }
    }

    // ========================= UPDATE =========================

    /** Mettre à jour (méta + remplacement PDF optionnel) */
    @PutMapping(value = "/pieces/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestParam(value = "titre", required = false) String titre,
                                    @RequestParam(value = "description", required = false) String description,
                                    @RequestParam("type") PieceJointeType type,
                                    @RequestPart(value = "newFile", required = false) MultipartFile newFile) {
        try {
            PieceJointe pj = service.update(id, titre, description, type, newFile);
            return ResponseEntity.ok(pj);
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur update PJ", e);
            return serverError();
        }
    }

    // ========================= READ (ONE) =========================

    @GetMapping(value = "/pieces/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get PJ by id", e);
            return serverError();
        }
    }

    // ========================= LIST (PAGINÉ) =========================

    /** Lister paginé les PJ d’un Agent, avec recherche/filtre */
    @GetMapping(value = "/agents/{agentId}/pieces", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listByAgent(@PathVariable Long agentId,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "10") @Min(1) int size,
                                         @RequestParam(defaultValue = "uploadedAt,DESC") String sort,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) PieceJointeType type,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date to) {
        try {
            Pageable pageable = buildPageable(page, size, sort);
            if (q != null && !q.isBlank()) {
                return ResponseEntity.ok(service.searchForAgent(agentId, q, pageable));
            }
            if (type != null) {
                return ResponseEntity.ok(service.listByAgentAndType(agentId, type, pageable));
            }
            if (from != null && to != null) {
                // on pourrait exposer un service dédié; ici simple fallback: le repo a une méthode Between.
                // Pour rester cohérent avec ton service, fais plutôt une recherche front-end ou ajoute une méthode service.
                return ResponseEntity.ok(service.listByAgent(agentId, pageable)); // adapter si besoin
            }
            return ResponseEntity.ok(service.listByAgent(agentId, pageable));
        } catch (Exception e) {
            log.error("Erreur list PJ agent", e);
            return serverError();
        }
    }

    /** Lister paginé les PJ d’un Sénateur, avec recherche/filtre */
    @GetMapping(value = "/senateurs/{senateurId}/pieces", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listBySenateur(@PathVariable Long senateurId,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "10") @Min(1) int size,
                                            @RequestParam(defaultValue = "uploadedAt,DESC") String sort,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(required = false) PieceJointeType type,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date to) {
        try {
            Pageable pageable = buildPageable(page, size, sort);
            if (q != null && !q.isBlank()) {
                return ResponseEntity.ok(service.searchForSenateur(senateurId, q, pageable));
            }
            if (type != null) {
                return ResponseEntity.ok(service.listBySenateurAndType(senateurId, type, pageable));
            }
            if (from != null && to != null) {
                return ResponseEntity.ok(service.listBySenateur(senateurId, pageable)); // adapter si besoin
            }
            return ResponseEntity.ok(service.listBySenateur(senateurId, pageable));
        } catch (Exception e) {
            log.error("Erreur list PJ senateur", e);
            return serverError();
        }
    }

    // ========================= DOWNLOAD (inline / attachment) =========================
    /**
     * Téléchargement sécurisé:
     * - lecture du fichier depuis le service (via meta -> fileName)
     * - forcer le Content-Type PDF
     * - Content-Disposition = inline (pour modal PDF) ou attachment (téléchargement)
     * - empêche traversée de répertoire
     */
    @GetMapping("/pieces/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id,
                                      @RequestParam(defaultValue = "inline") String disposition) {
        try {
            PieceJointe pj = service.getById(id);
            String fileName = pj.getFileName();
            if (fileName == null || fileName.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Fichier manquant"));
            }

            // Empêcher path traversal
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Nom de fichier invalide"));
            }

            // Récupération du flux depuis le dossier configuré
            // Comme c'est le service qui connaît docsDir, on relit via NIO ici:
            Path docsDir = Paths.get(System.getProperty("user.dir"), "uploads", "docs"); // Garde en cohérence avec service
            Path path = docsDir.resolve(fileName).normalize();
            if (!Files.exists(path)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Fichier introuvable"));
            }

            // Forcer PDF
            MediaType mediaType = MediaType.APPLICATION_PDF;
            String safeOriginal = Optional.ofNullable(pj.getOriginalName()).orElse("document.pdf").replaceAll("[\\r\\n]", "");
            String cdValue = ("attachment".equalsIgnoreCase(disposition))
                    ? ContentDisposition.attachment().filename(safeOriginal).build().toString()
                    : ContentDisposition.inline().filename(safeOriginal).build().toString();

            InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
            Resource body = new InputStreamResource(is);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, cdValue)
                    .header("X-Content-Type-Options", "nosniff")

                    .cacheControl(CacheControl.noCache())
                    .body(body);

        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur download PJ", e);
            return serverError();
        }
    }

    // ========================= DELETE =========================

    @DeleteMapping("/pieces/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id); // supprime l’entité + fichier (service.deleteQuiet)
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur delete PJ", e);
            return serverError();
        }
    }

    // ========================= Helpers =========================

    private Pageable buildPageable(int page, int size, String sort) {
        // sort format: "field,DESC" ou "field,ASC"
        Sort s = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            String[] sp = sort.split(",");
            if (sp.length == 2) {
                s = Sort.by(Sort.Direction.fromString(sp[1].trim()), sp[0].trim());
            } else {
                s = Sort.by(sort.trim()).descending();
            }
        }
        return PageRequest.of(page, size, s);
    }

    private ResponseEntity<Map<String, Object>> conflict(String msg) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
    }
    private ResponseEntity<Map<String, Object>> notFound(String msg) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
    }
    private ResponseEntity<Map<String, Object>> serverError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur serveur"));
    }
}
