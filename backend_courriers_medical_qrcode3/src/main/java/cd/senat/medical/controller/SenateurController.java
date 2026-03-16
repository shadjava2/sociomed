// src/main/java/cd/senat/medical/controller/SenateurController.java
package cd.senat.medical.controller;

import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.dto.SenateurDTO;
import cd.senat.medical.entity.Genre;
import cd.senat.medical.entity.StatutSenateur;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.service.SenateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/senateurs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SenateurController {

    private static final Logger log = LoggerFactory.getLogger(SenateurController.class);

    private final SenateurService service;

    /** Dossier d'upload des photos (défaut : {user.dir}/uploads/photos/) */
    @Value("${app.upload.dir:#{systemProperties['user.dir']}/uploads/photos/}")
    private String uploadDir;

    /** Optionnel si tu reçois encore des dates 'dd/MM/yyyy' côté front. */
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(java.util.Date.class,
            new CustomDateEditor(new SimpleDateFormat("dd/MM/yyyy"), true));
    }

    // =========================================================================
    // 0) LISTE PAGINÉE + RECHERCHE/FILTRES (DTO Summary)
    // =========================================================================
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageResponse<SenateurDTO.Summary>> listPaged(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StatutSenateur statut,   // EN_ACTIVITE / HONORAIRE
            @RequestParam(required = false) Genre genre,             // M / F
            @RequestParam(required = false) String legislature,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // ⚠️ Implémente/ajuste ces filtres dans SenateurService
        PageResponse<SenateurDTO.Summary> resp = service.getAll(q, statut, genre, legislature, pageable);
        return ResponseEntity.ok(resp);
    }

    // =========================================================================
    // 1) CRUD JSON (Create/Update en DTO)
    // =========================================================================
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createJson(@RequestBody @Valid SenateurDTO.CreateRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur createJson sénateur", e);
            return serverError();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateJson(@PathVariable Long id, @RequestBody @Valid SenateurDTO.UpdateRequest req) {
        try {
            return ResponseEntity.ok(service.update(id, req));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur updateJson sénateur", e);
            return serverError();
        }
    }

    // =========================================================================
    // 1bis) ALIAS MULTIPART sur les mêmes routes (pour FormData front)
    // =========================================================================
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMultipart(
            @RequestPart("senateur") @Valid SenateurDTO.CreateRequest req,
            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                String newName = storePhoto(photoFile);
                req = new SenateurDTO.CreateRequest(
                        req.nom(), req.postnom(), req.prenom(), req.genre(), req.datenaiss(),
                        req.statut(), req.telephone(), req.legislature(), req.email(), req.adresse(),
                        newName
                );
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur createMultipart sénateur", e);
            return serverError();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMultipart(
            @PathVariable Long id,
            @RequestPart("senateur") @Valid SenateurDTO.UpdateRequest req,
            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                String newName = storePhoto(photoFile);
                req = new SenateurDTO.UpdateRequest(
                        req.nom(), req.postnom(), req.prenom(), req.genre(), req.datenaiss(),
                        req.statut(), req.telephone(), req.legislature(), req.email(), req.adresse(),
                        newName
                );
            }
            return ResponseEntity.ok(service.update(id, req));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur updateMultipart sénateur", e);
            return serverError();
        }
    }

    // =========================================================================
    // 2) Upload/Remplacement de photo (multipart) → retourne Detail
    // =========================================================================
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id,
                                         @RequestPart("photoFile") MultipartFile photoFile) {
        String newName = null;
        try {
            if (photoFile == null || photoFile.isEmpty()) {
                return conflict("Aucun fichier fourni.");
            }

            var current = service.getById(id); // DTO.Detail
            String oldPhoto = current.photo();

            newName = storePhoto(photoFile);

            var req = new SenateurDTO.UpdateRequest(
                    null, null, null, null, null,   // identité/genre/date
                    null,                           // statut
                    null, null, null, null,         // téléphone/législature/email/adresse
                    newName                         // photo
            );

            var updated = service.update(id, req);

            if (oldPhoto != null && !oldPhoto.isBlank() && !oldPhoto.equals(newName)) {
                deletePhotoQuiet(oldPhoto);
            }

            return ResponseEntity.ok(updated);

        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur uploadPhoto sénateur", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    // =========================================================================
    // 3) READ (Detail) / DELETE
    // =========================================================================
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            // On renvoie directement un DTO.Detail (incluant éventuellement le conjoint + enfants)
            return ResponseEntity.ok(service.getById(id));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur getById sénateur", e);
            return serverError();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            var current = service.getById(id);
            String oldPhoto = (current != null) ? current.photo() : null;

            service.delete(id);

            if (oldPhoto != null && !oldPhoto.isBlank()) {
                deletePhotoQuiet(oldPhoto);
            }
            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur delete sénateur", e);
            return serverError();
        }
    }

    // =========================================================================
    // 4) Serveur de fichiers : GET photo
    // =========================================================================
    @GetMapping("/photos/{filename}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path file = base.resolve(filename).normalize();

            // Sécurité : empêche le path traversal
            if (!file.startsWith(base)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Resource res = new UrlResource(file.toUri());
            if (!res.exists() || !res.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String mime = Files.probeContentType(file);
            if (mime == null) mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mime)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                    .body(res);

        } catch (MalformedURLException e) {
            log.warn("URL invalide pour la photo '{}': {}", filename, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur getPhoto '{}'", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // Helpers upload fichiers
    // =========================================================================
    private String storePhoto(MultipartFile file) throws Exception {
        Path directory = Paths.get(uploadDir);
        if (!Files.exists(directory)) Files.createDirectories(directory);

        String original = Objects.requireNonNull(file.getOriginalFilename(), "Nom de fichier manquant");
        String fileName = StringUtils.cleanPath(original).replaceAll("\\s+", "_");
        if (fileName.isEmpty()) throw new Exception("Le fichier n'a pas de nom valide.");

        String[] bt = fileName.split("\\.");
        if (bt.length < 2) throw new Exception("Le fichier ne possède pas d'extension valide.");
        String ext = bt[bt.length - 1].toLowerCase(Locale.ROOT);

        List<String> allowed = Arrays.asList("jpg", "jpeg", "png", "webp");
        if (!allowed.contains(ext)) throw new Exception("Extension non autorisée: " + ext);

        String ts = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String newName = ts + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

        Path target = directory.resolve(newName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        log.info("Photo (sénateur) uploadée: {}", newName);
        return newName;
    }

    private void deletePhotoQuiet(String fileName) {
        try {
            if (fileName == null || fileName.isBlank()) return;
            Path p = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(p);
            log.info("Photo supprimée: {}", p);
        } catch (Exception ex) {
            log.warn("Echec suppression photo '{}': {}", fileName, ex.getMessage());
        }
    }

    // =========================================================================
    // Helpers réponses JSON erreur
    // =========================================================================
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
