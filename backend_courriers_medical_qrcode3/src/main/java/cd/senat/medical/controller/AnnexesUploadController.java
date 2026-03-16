package cd.senat.medical.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cd.senat.medical.dto.AnnexeDTO;
import cd.senat.medical.service.AnnexeService;
import cd.senat.medical.service.AnnexesChatGptService;
import cd.senat.medical.service.AnnexesStorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/courriers/{courrierId}/annexes")
@CrossOrigin(origins = "*")
//@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('SECRETAIRE')")
public class AnnexesUploadController {

    private static final Logger log = LoggerFactory.getLogger(AnnexesUploadController.class);

    private final AnnexesStorageService storage;
    private final AnnexeService annexeService;
    private final AnnexesChatGptService annexesChatGptService;

    public AnnexesUploadController(AnnexesStorageService storage,
                                   AnnexeService annexeService,
                                   AnnexesChatGptService annexesChatGptService) {
        this.storage = storage;
        this.annexeService = annexeService;
        this.annexesChatGptService = annexesChatGptService;
    }

    // ---- UPLOAD MULTIPLE ET PERSISTENCE ----
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadAnnexes(@PathVariable Long courrierId,
                                           @RequestParam("files") @NotNull MultipartFile[] files) {
        try {
            if (files.length == 0) return ResponseEntity.badRequest().body(Map.of("error", "Aucun fichier"));

            List<AnnexeDTO> saved = new ArrayList<>();
            for (MultipartFile f : files) {
                validateAllowedType(f); // PDF/JPG/PNG
                String stored = storage.store(f);                   // écrit dans annexes_upload
                String contentType = safeContentType(storage.resolve(stored)); // MIME robuste
                long size = sizeOf(storage.resolve(stored));
                AnnexeDTO dto = new AnnexeDTO(f.getOriginalFilename(), size, contentType, stored);
                saved.add(annexeService.addToCourrier(courrierId, dto)); // persiste
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Upload annexes failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ---- LISTE des annexes du courrier ----
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(@PathVariable Long courrierId) {
        try {
            return ResponseEntity.ok(annexeService.listByCourrier(courrierId));
        } catch (Exception e) {
            log.error("List annexes failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ---- VIEW (inline) depuis annexes_upload ----
    @GetMapping(value = "/view/{filename}")
    public ResponseEntity<Resource> view(@PathVariable Long courrierId,
                                         @PathVariable String filename) throws Exception {
        Resource r = storage.loadAsResource(filename);
        Path p = storage.resolve(filename);
        MediaType mt = guessMediaType(p);
        return ResponseEntity.ok()
                .contentType(mt)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(r);
    }

    // ---- DOWNLOAD depuis annexes_upload ----
    @GetMapping(value = "/download/{filename}")
    public ResponseEntity<Resource> download(@PathVariable Long courrierId,
                                             @PathVariable String filename) throws Exception {
        Resource r = storage.loadAsResource(filename);
        Path p = storage.resolve(filename);
        MediaType mt = guessMediaType(p);
        return ResponseEntity.ok()
                .contentType(mt)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(r);
    }



 // ---- SUPPRESSION ----
    @DeleteMapping("/{annexeId}")
    public ResponseEntity<?> deleteAnnexe(@PathVariable Long courrierId, @PathVariable Long annexeId) {
        try {
            // 1) Retrouver l’annexe dans ce courrier (ou considérer déjà supprimée)
            AnnexeDTO annexe = annexeService.listByCourrier(courrierId).stream()
                    .filter(a -> Objects.equals(a.getId(), annexeId))
                    .findFirst()
                    .orElse(null);

            // 2) Supprimer le lien BD si elle existe encore
            if (annexe != null) {
                annexeService.removeFromCourrier(courrierId, annexeId);

                // 3) Suppression best-effort du fichier physique (si on a une URL/nom stocké)
                try {
                    String fn = annexe.getUrl();
                    if (fn != null && !fn.isBlank()) {
                        Files.deleteIfExists(storage.resolve(fn));
                    }
                } catch (Exception fileDelEx) {
                    // On log, mais on n’échoue pas : l’annexe BD est déjà supprimée
                    log.warn("Suppression fichier échouée pour annexeId={}, courrierId={}", annexeId, courrierId, fileDelEx);
                }
            }

            // Idempotent : 204 même si l’annexe n’était déjà plus là
            return ResponseEntity.noContent().build();

        } catch (EntityNotFoundException e) {
            // Courrier inexistant (ou autre entité manquante coté service)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
            // Conflit de version : on considère la suppression comme aboutie côté client
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Delete annexe error (courrierId={}, annexeId={})", courrierId, annexeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
 


    // ---- helpers ----
    private static final Set<String> ALLOWED = Set.of("application/pdf", "image/jpeg", "image/jpg", "image/png");
    private static final Set<String> ALLOWED_EXT = Set.of(".pdf", ".jpg", ".jpeg", ".png");

    private void validateAllowedType(MultipartFile f) {
        String name = Optional.ofNullable(f.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        boolean extOk = ALLOWED_EXT.stream().anyMatch(name::endsWith);
        if (!extOk) throw new IllegalArgumentException("Extension non autorisée. Formats autorisés: PDF, JPG, PNG");
    }

    private String safeContentType(Path path) {
        try {
            String ct = Files.probeContentType(path);
            if (ct != null) {
                if (!ALLOWED.contains(ct)) {
                    MediaType mt = guessMediaType(path);
                    return mt.toString();
                }
                return ct;
            }
        } catch (Exception ignored) {}
        return guessMediaType(path).toString();
    }

    private long sizeOf(Path p) {
        try { return Files.size(p); } catch (Exception e) { return 0L; }
    }

    private static MediaType guessMediaType(Path path) {
        try {
            String ct = Files.probeContentType(path);
            if (ct != null) return MediaType.parseMediaType(ct);
        } catch (Exception ignored) {}
        String fn = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fn.endsWith(".pdf"))  return MediaType.APPLICATION_PDF;
        if (fn.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (fn.endsWith(".jpg") || fn.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    // ---- IA: Health check simple ----
    @GetMapping("/ai/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Annexes AI service is running");
    }
}
