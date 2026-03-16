// src/main/java/cd/senat/medical/controller/ConjointController.java
package cd.senat.medical.controller;

import cd.senat.medical.dto.ConjointDTO;
import cd.senat.medical.mapper.ConjointMapper;
import cd.senat.medical.entity.Agent;
import cd.senat.medical.entity.Conjoint;
import cd.senat.medical.entity.Senateur;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.service.ConjointService;
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

/**
 * ConjointController aligné sur AgentController :
 * - Routes JSON + alias multipart
 * - Upload dédié /conjoints/{id}/photo
 * - Réponses en DTO via ConjointMapper
 * - Rétro-compatibilité: endpoints imbriqués conservés
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConjointController {

    private static final Logger log = LoggerFactory.getLogger(ConjointController.class);

    private final ConjointService service;

    /** Dossier d’upload (photos). Défaut: {user.dir}/uploads/photos/ */
    @Value("${app.upload.dir:#{systemProperties['user.dir']}/uploads/photos/}")
    private String uploadDir;

    /** Binder si la date arrive en dd/MM/yyyy depuis d’anciens fronts (facultatif). */
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(java.util.Date.class,
            new CustomDateEditor(new SimpleDateFormat("dd/MM/yyyy"), true));
    }

    // =========================================================================
    // 1) CRUD JSON (symétrique à AgentController)
    // =========================================================================

    /** Créer un conjoint (JSON) : fournir agentId XOR senateurId dans le DTO */
    @PostMapping(value = "/conjoints", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createJson(@RequestBody @Valid ConjointDTO.CreateRequest req) {
        try {
            Conjoint saved = routeCreateByParent(req, ConjointMapper.fromCreate(req));
            return ResponseEntity.status(HttpStatus.CREATED).body(ConjointMapper.toDetail(saved));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur createJson conjoint", e);
            return serverError();
        }
    }

    /** Mettre à jour un conjoint (JSON). */
    @PutMapping(value = "/conjoints/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateJson(@PathVariable Long id, @RequestBody @Valid ConjointDTO.UpdateRequest req) {
        Conjoint current;
        try {
            current = service.getById(id);
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get conjoint avant updateJson", e);
            return serverError();
        }

        try {
            ConjointMapper.applyUpdate(current, req);
            Conjoint updated = service.update(id, current);
            return ResponseEntity.ok(ConjointMapper.toDetail(updated));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur updateJson conjoint", e);
            return serverError();
        }
    }

    // =========================================================================
    // 1bis) Alias MULTIPART (comme AgentController)
    // =========================================================================

    /** Alias multipart pour POST /api/conjoints (DTO + photoFile) */
    @PostMapping(value = "/conjoints", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMultipartAlias(
            @RequestPart("conjoint") @Valid ConjointDTO.CreateRequest req,
            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        String newName = null;
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                newName = storePhoto(photoFile);
                req = new ConjointDTO.CreateRequest(
                    req.nom(), req.postnom(), req.prenom(), req.genre(), req.datenaiss(),
                    req.profession(), newName, req.agentId(), req.senateurId()
                );
            }
            Conjoint saved = routeCreateByParent(req, ConjointMapper.fromCreate(req));
            return ResponseEntity.status(HttpStatus.CREATED).body(ConjointMapper.toDetail(saved));
        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur createMultipartAlias conjoint", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    /** Alias multipart pour PUT /api/conjoints/{id} (DTO + photoFile) */
    @PutMapping(value = "/conjoints/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMultipartAlias(
            @PathVariable Long id,
            @RequestPart("conjoint") @Valid ConjointDTO.UpdateRequest req,
            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {

        Conjoint current;
        try {
            current = service.getById(id);
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get conjoint avant updateMultipartAlias", e);
            return serverError();
        }

        String oldPhoto = current.getPhoto();
        String newName = null;

        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                newName = storePhoto(photoFile);
                req = new ConjointDTO.UpdateRequest(
                    req.nom(), req.postnom(), req.prenom(), req.genre(), req.datenaiss(),
                    req.profession(), newName, req.agentId(), req.senateurId()
                );
            }
            ConjointMapper.applyUpdate(current, req);
            Conjoint updated = service.update(id, current);

            if (newName != null && oldPhoto != null && !oldPhoto.equals(newName)) {
                deletePhotoQuiet(oldPhoto);
            }
            return ResponseEntity.ok(ConjointMapper.toDetail(updated));

        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur updateMultipartAlias conjoint", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    // =========================================================================
    // 2) UPLOAD photo dédié → retourne DTO.Detail
    // =========================================================================
    @PostMapping(value = "/conjoints/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id,
                                         @RequestPart("photoFile") MultipartFile photoFile) {
        Conjoint current;
        try {
            current = service.getById(id);
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get conjoint avant uploadPhoto", e);
            return serverError();
        }

        String oldPhoto = current.getPhoto();
        String newName = null;

        try {
            if (photoFile == null || photoFile.isEmpty()) {
                return conflict("Aucun fichier fourni.");
            }
            newName = storePhoto(photoFile);
            current.setPhoto(newName);

            Conjoint updated = service.update(id, current);

            if (oldPhoto != null && !oldPhoto.equals(newName)) {
                deletePhotoQuiet(oldPhoto);
            }
            return ResponseEntity.ok(ConjointMapper.toDetail(updated));

        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur uploadPhoto conjoint", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    // =========================================================================
    // 3) READ / DELETE
    // =========================================================================
    @GetMapping(value = "/conjoints/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ConjointMapper.toDetail(service.getById(id)));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur get conjoint by id", e);
            return serverError();
        }
    }

    @DeleteMapping("/conjoints/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            Conjoint c = service.getById(id);
            String oldPhoto = (c != null) ? c.getPhoto() : null;

            service.delete(id);

            if (oldPhoto != null && !oldPhoto.isBlank()) {
                deletePhotoQuiet(oldPhoto);
            }
            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur delete conjoint", e);
            return serverError();
        }
    }

    // =========================================================================
    // 4) Serveur de fichiers : GET photo (même style qu’Agent)
    // =========================================================================
    @GetMapping("/conjoints/photos/{filename}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path file = base.resolve(filename).normalize();

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
            log.error("Erreur getPhoto conjoint '{}'", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // 5) Rétro-compat : endpoints imbriqués existants (renvoient maintenant DTO)
    // =========================================================================
    @PostMapping(
        value = "/agents/{agentId}/conjoint",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> createForAgent(@PathVariable Long agentId,
                                            @RequestPart("conjoint") Conjoint conjoint,
                                            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        String newName = null;
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                newName = storePhoto(photoFile);
                conjoint.setPhoto(newName);
            }
            Conjoint saved = service.createForAgent(agentId, conjoint);
            return ResponseEntity.status(HttpStatus.CREATED).body(ConjointMapper.toDetail(saved));

        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur create conjoint (agent)", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    @PostMapping(
        value = "/senateurs/{senateurId}/conjoint",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> createForSenateur(@PathVariable Long senateurId,
                                               @RequestPart("conjoint") Conjoint conjoint,
                                               @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        String newName = null;
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                newName = storePhoto(photoFile);
                conjoint.setPhoto(newName);
            }
            Conjoint saved = service.createForSenateur(senateurId, conjoint);
            return ResponseEntity.status(HttpStatus.CREATED).body(ConjointMapper.toDetail(saved));

        } catch (BusinessException be) {
            if (newName != null) deletePhotoQuiet(newName);
            return conflict(be.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            if (newName != null) deletePhotoQuiet(newName);
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur create conjoint (senateur)", e);
            if (newName != null) deletePhotoQuiet(newName);
            return serverError();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Conjoint routeCreateByParent(ConjointDTO.CreateRequest req, Conjoint entity)
            throws BusinessException, ResourceNotFoundException {
        if ((req.agentId() != null) ^ (req.senateurId() != null)) {
            if (req.agentId() != null) {
                return service.createForAgent(req.agentId(), entity);
            } else {
                return service.createForSenateur(req.senateurId(), entity);
            }
        }
        throw new BusinessException("Associer exactement un parent : soit agentId, soit senateurId.");
    }

    private String storePhoto(MultipartFile file) throws Exception {
        Path directory = Paths.get(uploadDir);
        if (!Files.exists(directory)) Files.createDirectories(directory);

        String original = Objects.requireNonNull(file.getOriginalFilename(), "Nom de fichier manquant");
        String fileName = StringUtils.cleanPath(original).replaceAll("\\s+", "_");
        if (fileName.isEmpty()) throw new Exception("Le fichier n'a pas de nom valide.");

        String[] bt = fileName.split("\\.");
        if (bt.length < 2) throw new Exception("Le fichier ne possède pas d'extension valide.");
        String ext = bt[bt.length - 1].toLowerCase(Locale.ROOT);

        List<String> allowed = Arrays.asList("jpg","jpeg","png","webp");
        if (!allowed.contains(ext)) throw new Exception("Extension non autorisée: " + ext);

        String ts = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String newName = ts + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

        Path target = directory.resolve(newName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        log.info("Photo uploadée (conjoint): {}", newName);
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
