package cd.senat.medical.controller;

import cd.senat.medical.dto.AgentsDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.Genre;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.service.AgentAppService;
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
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentAppService service;

    /** Dossier d'upload des photos (défaut : {user.dir}/uploads/photos/) */
    @Value("${app.upload.dir:#{systemProperties['user.dir']}/uploads/photos/}")
    private String uploadDir;

    /** Binder utile si des dates "dd/MM/yyyy" arrivent encore depuis le front. */
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(java.util.Date.class,
            new CustomDateEditor(new SimpleDateFormat("dd/MM/yyyy"), true));
    }

    // =========================================================================
    // 0) LISTE PAGINÉE + RECHERCHE/FILTRES (DTO Summary)
    // =========================================================================
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageResponse<AgentsDTO.Summary>> listPaged(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Genre genre,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String etat,
            @RequestParam(required = false) String categorie,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<AgentsDTO.Summary> resp = service.getAll(q, genre, etat, direction, categorie, pageable);
        return ResponseEntity.ok(resp);
    }

    // =========================================================================
    // 1) CRUD JSON (Create/Update avec DTO)
    // =========================================================================
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createJson(@RequestBody @Valid AgentsDTO.CreateRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur createJson agent", e);
            return serverError();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateJson(@PathVariable Long id, @RequestBody @Valid AgentsDTO.UpdateRequest req) {
        try {
            return ResponseEntity.ok(service.update(id, req));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur updateJson agent", e);
            return serverError();
        }
    }

    // =========================================================================
    // 1bis) ALIAS MULTIPART sur les mêmes routes (pour éviter 415)
    // =========================================================================
    /** Alias multipart pour POST /api/agents (même sémantique que /multipart) */
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMultipartAlias(
            @RequestPart("agent") @Valid AgentsDTO.CreateRequest req,
            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                String newName = storePhoto(photoFile);
                req = new AgentsDTO.CreateRequest(
                    req.nom(), req.postnom(), req.prenom(), req.genre(), req.datenaiss(), req.lnaiss(),
                    req.etatc(), req.village(), req.groupement(), req.secteur(), req.territoire(), req.district(),
                    req.province(), req.nationalite(), req.telephone(), req.email(), req.adresse(), req.direction(),
                    req.etat(), req.stat(), newName, req.categorie()
                );
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur createMultipartAlias agent", e);
            return serverError();
        }
    }

    /** Alias multipart pour PUT /api/agents/{id} (même sémantique que /multipart/{id}) */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMultipartAlias(
            @PathVariable Long id,
            @RequestPart("agent") @Valid AgentsDTO.UpdateRequest req,
            @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                String newName = storePhoto(photoFile);
                req = new AgentsDTO.UpdateRequest(
                    req.nom(), req.postnom(), req.prenom(), req.genre(), req.datenaiss(), req.lnaiss(),
                    req.etatc(), req.village(), req.groupement(), req.secteur(), req.territoire(), req.district(),
                    req.province(), req.nationalite(), req.telephone(), req.email(), req.adresse(), req.direction(),
                    req.etat(), req.stat(), newName, req.categorie()
                );
            }
            return ResponseEntity.ok(service.update(id, req));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur updateMultipartAlias agent", e);
            return serverError();
        }
    }

    // =========================================================================
    // 2) Upload de photo (appel séparé) → retourne le DÉTAIL à jour
    // =========================================================================
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id,
                                         @RequestPart("photoFile") MultipartFile photoFile) {
        try {
            if (photoFile == null || photoFile.isEmpty()) {
                return conflict("Aucun fichier fourni.");
            }
            String newName = storePhoto(photoFile);

            AgentsDTO.UpdateRequest dto = new AgentsDTO.UpdateRequest(
                null,null,null,null,null,null,
                null,null,null,null,null,null,null,null,null,null,
                null,null,null,null,
                newName, null
            );
            return ResponseEntity.ok(service.update(id, dto));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur uploadPhoto agent", e);
            return serverError();
        }
    }

    // =========================================================================
    // 3) READ (détail) / DELETE
    // =========================================================================
    @GetMapping(value = "/{id}/details", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDetails(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getDetails(id));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur getDetails agent", e);
            return serverError();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur getById agent", e);
            return serverError();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception e) {
            log.error("Erreur delete agent", e);
            return serverError();
        }
    }

    // =========================================================================
    // 4) Compat : anciens endpoints multipart (si encore utilisés)
    //    → on renvoie aussi des DTO Detail
    // =========================================================================
    @PostMapping(value = "/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMultipart(@RequestPart("agent") @Valid AgentsDTO.CreateRequest req,
                                             @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                String newName = storePhoto(photoFile);
                req = new AgentsDTO.CreateRequest(
                    req.nom(), req.postnom(), req.prenom(), req.genre(), req.datenaiss(), req.lnaiss(),
                    req.etatc(), req.village(), req.groupement(), req.secteur(), req.territoire(), req.district(),
                    req.province(), req.nationalite(), req.telephone(), req.email(), req.adresse(), req.direction(),
                    req.etat(), req.stat(), newName, req.categorie()
                );
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur createMultipart agent", e);
            return serverError();
        }
    }

    @PutMapping(value = "/multipart/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMultipart(@PathVariable Long id,
                                             @RequestPart("agent") @Valid AgentsDTO.UpdateRequest req,
                                             @RequestPart(value = "photoFile", required = false) MultipartFile photoFile) {
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                String newName = storePhoto(photoFile);
                req = new AgentsDTO.UpdateRequest(
                    req.nom(), req.postnom(), req.prenom(), req.genre(), req.datenaiss(), req.lnaiss(),
                    req.etatc(), req.village(), req.groupement(), req.secteur(), req.territoire(), req.district(),
                    req.province(), req.nationalite(), req.telephone(), req.email(), req.adresse(), req.direction(),
                    req.etat(), req.stat(), newName, req.categorie()
                );
            }
            return ResponseEntity.ok(service.update(id, req));
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (BusinessException be) {
            return conflict(be.getMessage());
        } catch (Exception e) {
            log.error("Erreur updateMultipart agent", e);
            return serverError();
        }
    }

    // =========================================================================
    // 5) Serveur de fichiers : GET photo
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
    // Helpers
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

        log.info("Photo uploadée: {}", newName);
        return newName;
    }

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
