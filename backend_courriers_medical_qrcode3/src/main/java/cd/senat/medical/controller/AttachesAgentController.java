// src/main/java/cd/senat/medical/controller/AttachesAgentController.java
package cd.senat.medical.controller;

import cd.senat.medical.dto.AttachesAgentDTO;
import cd.senat.medical.entity.AttachesAgent;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.mapper.AttachesAgentMapper;
import cd.senat.medical.repository.AttachesAgentRepository;
import cd.senat.medical.service.AttachesAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AttachesAgentController {

    private static final Logger log = LoggerFactory.getLogger(AttachesAgentController.class);

    private final AttachesAgentService service;
    private final AttachesAgentRepository repo;

    /** Dossier d'upload (photos). Défaut : {user.dir}/uploads/photos/ */
    @Value("${app.upload.dir:#{systemProperties['user.dir']}/uploads/photos/}")
    private String uploadDir;

    /** Binder (optionnel) si dates dd/MM/yyyy arrivent du front. */
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("dd/MM/yyyy"), true));
    }

    // =========================================================================
    // CREATE (JSON) — par parent (agent ou sénateur)
    // =========================================================================

    /** Créer un enfant pour un AGENT (JSON) */
    @PostMapping(value = "/agents/{agentId}/enfants", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createForAgentJson(@PathVariable Long agentId,
                                                @RequestBody @Valid AttachesAgentDTO.CreateRequest req) {
        try {
            AttachesAgent saved = service.createForAgent(agentId, AttachesAgentMapper.fromCreate(req));
            return ResponseEntity.status(HttpStatus.CREATED).body(AttachesAgentMapper.toItem(saved));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur create enfant (agent, JSON)", e);
            return serverError();
        }
    }

    /** Créer un enfant pour un SÉNATEUR (JSON) */
    @PostMapping(value = "/senateurs/{senateurId}/enfants", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createForSenateurJson(@PathVariable Long senateurId,
                                                   @RequestBody @Valid AttachesAgentDTO.CreateRequest req) {
        try {
            AttachesAgent saved = service.createForSenateur(senateurId, AttachesAgentMapper.fromCreate(req));
            return ResponseEntity.status(HttpStatus.CREATED).body(AttachesAgentMapper.toItem(saved));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur create enfant (senateur, JSON)", e);
            return serverError();
        }
    }

    // =========================================================================
    // CREATE (alias MULTIPART) — JSON + photoFile
    // =========================================================================

    /** Alias multipart (agent) : @RequestPart enfant DTO + photoFile */
    @PostMapping(value = "/agents/{agentId}/enfants", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createForAgentMultipart(@PathVariable Long agentId,
                                                     @RequestPart("enfant") @Valid AttachesAgentDTO.CreateRequest req,
                                                     @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        String newName = null;
        try {
            AttachesAgent entity = AttachesAgentMapper.fromCreate(req);
            if (photoFile != null && !photoFile.isEmpty()) {
                newName = storePhoto(photoFile);
                entity.setPhoto(newName);
            }
            AttachesAgent saved = service.createForAgent(agentId, entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(AttachesAgentMapper.toItem(saved));
        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur create enfant (agent, multipart)", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    /** Alias multipart (sénateur) : @RequestPart enfant DTO + photoFile */
    @PostMapping(value = "/senateurs/{senateurId}/enfants", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createForSenateurMultipart(@PathVariable Long senateurId,
                                                        @RequestPart("enfant") @Valid AttachesAgentDTO.CreateRequest req,
                                                        @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        String newName = null;
        try {
            AttachesAgent entity = AttachesAgentMapper.fromCreate(req);
            if (photoFile != null && !photoFile.isEmpty()) {
                newName = storePhoto(photoFile);
                entity.setPhoto(newName);
            }
            AttachesAgent saved = service.createForSenateur(senateurId, entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(AttachesAgentMapper.toItem(saved));
        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur create enfant (senateur, multipart)", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    // =========================================================================
    // UPDATE (JSON + alias MULTIPART)
    // =========================================================================

    @PutMapping(value = "/enfants/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateJson(@PathVariable Long id,
                                        @RequestBody @Valid AttachesAgentDTO.UpdateRequest req) {
        try {
            AttachesAgent current = service.getById(id);
            AttachesAgentMapper.applyUpdate(current, req);
            AttachesAgent updated = service.update(id, current);
            return ResponseEntity.ok(AttachesAgentMapper.toItem(updated));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur update enfant (JSON)", e);
            return serverError();
        }
    }

    @PutMapping(value = "/enfants/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMultipart(@PathVariable Long id,
                                             @RequestPart("enfant") @Valid AttachesAgentDTO.UpdateRequest req,
                                             @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        String newName = null;
        try {
            AttachesAgent current = service.getById(id);
            AttachesAgentMapper.applyUpdate(current, req);
            if (photoFile != null && !photoFile.isEmpty()) {
                newName = storePhoto(photoFile);
                current.setPhoto(newName);
            }
            AttachesAgent updated = service.update(id, current);
            return ResponseEntity.ok(AttachesAgentMapper.toItem(updated));
        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur update enfant (multipart)", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    // =========================================================================
    // UPLOAD dédié photo → retourne DTO.Item
    // =========================================================================

    @PostMapping(value = "/enfants/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id, @RequestPart("photoFile") MultipartFile photoFile) {
        String newName = null;
        try {
            if (photoFile == null || photoFile.isEmpty()) return conflict("Aucun fichier fourni.");
            AttachesAgent current = service.getById(id);

            String oldPhoto = current.getPhoto();
            newName = storePhoto(photoFile);
            current.setPhoto(newName);

            AttachesAgent updated = service.update(id, current);

            if (oldPhoto != null && !oldPhoto.equals(newName)) deletePhotoQuiet(oldPhoto);
            return ResponseEntity.ok(AttachesAgentMapper.toItem(updated));

        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur uploadPhoto enfant", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    // =========================================================================
    // READ par ID & LIST par parent → DTO
    // =========================================================================

    @GetMapping(value = "/enfants/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(AttachesAgentMapper.toItem(service.getById(id)));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get enfant by id", e);
            return serverError();
        }
    }

    @GetMapping(value = "/agents/{agentId}/enfants", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listByAgent(@PathVariable Long agentId) {
        try {
            return ResponseEntity.ok(
                AttachesAgentMapper.toItems(repo.findByAgent_Id(agentId))
            );
        } catch (Exception e) {
            log.error("Erreur list enfants by agent", e);
            return serverError();
        }
    }

    @GetMapping(value = "/senateurs/{senateurId}/enfants", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listBySenateur(@PathVariable Long senateurId) {
        try {
            return ResponseEntity.ok(
                AttachesAgentMapper.toItems(repo.findBySenateur_Id(senateurId))
            );
        } catch (Exception e) {
            log.error("Erreur list enfants by senateur", e);
            return serverError();
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @DeleteMapping("/enfants/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            // Option : supprimer aussi la photo physique
            AttachesAgent e = service.getById(id);
            String old = (e != null) ? e.getPhoto() : null;

            service.delete(id);

            if (old != null && !old.isBlank()) deletePhotoQuiet(old);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erreur delete enfant", e);
            return serverError();
        }
    }

    // =========================================================================
    // GET fichier photo (public/privé selon WebSecurityConfig)
    // =========================================================================
    @GetMapping("/enfants/photos/{filename}")
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
            log.error("Erreur getPhoto enfant '{}'", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // Upload helpers
    // =========================================================================

    private String storePhoto(MultipartFile file) throws Exception {
        Path directory = Paths.get(uploadDir);
        if (!Files.exists(directory)) Files.createDirectories(directory);

        String original = Objects.requireNonNull(file.getOriginalFilename());
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

        log.info("Photo enfant uploadée: {}", newName);
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
    // Helpers erreurs JSON
    // =========================================================================
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
