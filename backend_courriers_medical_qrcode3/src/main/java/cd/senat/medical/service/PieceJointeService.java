package cd.senat.medical.service;

import cd.senat.medical.entity.*;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.repository.AgentRepository;
import cd.senat.medical.repository.PieceJointeRepository;
import cd.senat.medical.repository.SenateurRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class PieceJointeService {

    private static final Logger log = LoggerFactory.getLogger(PieceJointeService.class);

    private final PieceJointeRepository repo;
    private final AgentRepository agentRepo;
    private final SenateurRepository senateurRepo;

    /**
     * Dossier d’upload des documents (PDF).
     * Exemple prod (OVH) : app.upload.docs-dir=/home/otshudii/public_html/courrier-uploads/docs/
     * Défaut : {user.dir}/uploads/docs/
     */
    @Value("${app.upload.docs-dir:#{systemProperties['user.dir']}/uploads/docs/}")
    private String docsDir;

    // ============================== CREATE ==============================

    public PieceJointe createForAgent(Long agentId,
                                      PieceJointeType type,
                                      String titre,
                                      String description,
                                      MultipartFile file) {

        Agent agent = agentRepo.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable: " + agentId));

        validatePdf(file);

        String checksum = computeSha256(file);
        if (repo.existsChecksumForAgent(agentId, checksum)) {
            throw new BusinessException("Ce document (checksum identique) existe déjà pour cet agent.");
        }
        if (repo.existsDuplicateForAgent(agentId, originalName(file), type)) {
            throw new BusinessException("Un document du même type avec le même nom original existe déjà pour cet agent.");
        }

        String storedName = null;
        try {
            storedName = storeDocument(file); // écrit le PDF et retourne le nom unique

            PieceJointe pj = new PieceJointe();
            pj.setAgent(agent);
            pj.setSenateur(null);
            pj.setType(type);
            pj.setTitre(titre);
            pj.setDescription(description);
            pj.setFileName(storedName);
            pj.setOriginalName(originalName(file));
            pj.setMimeType(file.getContentType() != null ? file.getContentType() : "application/pdf");
            pj.setSizeBytes(file.getSize());
            pj.setChecksumSha256(checksum);
            pj.setUploadedAt(new Date());

            return repo.save(pj);

        } catch (RuntimeException ex) {
            // rollback fichier si la persistance échoue
            if (storedName != null) deleteQuiet(storedName);
            throw ex;
        } catch (Exception ex) {
            if (storedName != null) deleteQuiet(storedName);
            throw new BusinessException("Erreur lors de l’enregistrement du document.");
        }
    }

    public PieceJointe createForSenateur(Long senateurId,
                                         PieceJointeType type,
                                         String titre,
                                         String description,
                                         MultipartFile file) {

        Senateur s = senateurRepo.findById(senateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Sénateur introuvable: " + senateurId));

        validatePdf(file);

        String checksum = computeSha256(file);
        if (repo.existsChecksumForSenateur(senateurId, checksum)) {
            throw new BusinessException("Ce document (checksum identique) existe déjà pour ce sénateur.");
        }
        if (repo.existsDuplicateForSenateur(senateurId, originalName(file), type)) {
            throw new BusinessException("Un document du même type avec le même nom original existe déjà pour ce sénateur.");
        }

        String storedName = null;
        try {
            storedName = storeDocument(file);

            PieceJointe pj = new PieceJointe();
            pj.setAgent(null);
            pj.setSenateur(s);
            pj.setType(type);
            pj.setTitre(titre);
            pj.setDescription(description);
            pj.setFileName(storedName);
            pj.setOriginalName(originalName(file));
            pj.setMimeType(file.getContentType() != null ? file.getContentType() : "application/pdf");
            pj.setSizeBytes(file.getSize());
            pj.setChecksumSha256(checksum);
            pj.setUploadedAt(new Date());

            return repo.save(pj);

        } catch (RuntimeException ex) {
            if (storedName != null) deleteQuiet(storedName);
            throw ex;
        } catch (Exception ex) {
            if (storedName != null) deleteQuiet(storedName);
            throw new BusinessException("Erreur lors de l’enregistrement du document.");
        }
    }

    // ============================== UPDATE ==============================

    /**
     * Met à jour les méta + remplace le PDF si newFile != null :
     * - Upload d’abord le nouveau
     * - Persiste
     * - Si OK, supprime l’ancien (safe replace)
     */
    public PieceJointe update(Long id,
                              String nouveauTitre,
                              String nouvelleDescription,
                              PieceJointeType nouveauType,
                              MultipartFile newFile) {

        PieceJointe current = getById(id);

        String oldFile = current.getFileName();
        String newStored = null;

        try {
            // Mise à jour des champs métier
            current.setTitre(nouveauTitre);
            current.setDescription(nouvelleDescription);
            current.setType(nouveauType);

            if (newFile != null && !newFile.isEmpty()) {
                validatePdf(newFile);

                // Checks doublons scoped parent + type
                if (current.getAgent() != null) {
                    Long agentId = current.getAgent().getId();
                    String newChecksum = computeSha256(newFile);
                    if (!Objects.equals(current.getChecksumSha256(), newChecksum)
                            && repo.existsChecksumForAgent(agentId, newChecksum)) {
                        throw new BusinessException("Ce document (checksum identique) existe déjà pour cet agent.");
                    }
                    if (repo.existsDuplicateForAgent(agentId, originalName(newFile), nouveauType)) {
                        // NB: si le nom/type d’origine était identique au même enregistrement, on pourrait assouplir.
                        throw new BusinessException("Un document du même type avec le même nom original existe déjà pour cet agent.");
                    }

                    newStored = storeDocument(newFile);
                    current.setFileName(newStored);
                    current.setOriginalName(originalName(newFile));
                    current.setMimeType(newFile.getContentType() != null ? newFile.getContentType() : "application/pdf");
                    current.setSizeBytes(newFile.getSize());
                    current.setChecksumSha256(newChecksum);

                } else if (current.getSenateur() != null) {
                    Long senId = current.getSenateur().getId();
                    String newChecksum = computeSha256(newFile);
                    if (!Objects.equals(current.getChecksumSha256(), newChecksum)
                            && repo.existsChecksumForSenateur(senId, newChecksum)) {
                        throw new BusinessException("Ce document (checksum identique) existe déjà pour ce sénateur.");
                    }
                    if (repo.existsDuplicateForSenateur(senId, originalName(newFile), nouveauType)) {
                        throw new BusinessException("Un document du même type avec le même nom original existe déjà pour ce sénateur.");
                    }

                    newStored = storeDocument(newFile);
                    current.setFileName(newStored);
                    current.setOriginalName(originalName(newFile));
                    current.setMimeType(newFile.getContentType() != null ? newFile.getContentType() : "application/pdf");
                    current.setSizeBytes(newFile.getSize());
                    current.setChecksumSha256(newChecksum);
                }
            }

            PieceJointe saved = repo.save(current);

            // Remplacement réussi → suppression de l'ancien
            if (newStored != null && oldFile != null && !oldFile.equals(newStored)) {
                deleteQuiet(oldFile);
            }
            return saved;

        } catch (RuntimeException ex) {
            // roll back du nouveau fichier si la persistence échoue
            if (newStored != null) deleteQuiet(newStored);
            throw ex;
        } catch (Exception ex) {
            if (newStored != null) deleteQuiet(newStored);
            throw new BusinessException("Erreur lors de la mise à jour du document.");
        }
    }

    // ============================== READ ==============================

    public PieceJointe getById(Long id) {
        return repo.findByIdWithParent(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable: " + id));
    }

    public Page<PieceJointe> listByAgent(Long agentId, Pageable pageable) {
        return repo.findByAgent_Id(agentId, pageable);
    }

    public Page<PieceJointe> listBySenateur(Long senateurId, Pageable pageable) {
        return repo.findBySenateur_Id(senateurId, pageable);
    }

    public Page<PieceJointe> listByAgentAndType(Long agentId, PieceJointeType type, Pageable pageable) {
        return repo.findByAgent_IdAndType(agentId, type, pageable);
    }

    public Page<PieceJointe> listBySenateurAndType(Long senateurId, PieceJointeType type, Pageable pageable) {
        return repo.findBySenateur_IdAndType(senateurId, type, pageable);
    }

    public Page<PieceJointe> searchForAgent(Long agentId, String q, Pageable pageable) {
        return repo.searchForAgent(agentId, q, pageable);
    }

    public Page<PieceJointe> searchForSenateur(Long senateurId, String q, Pageable pageable) {
        return repo.searchForSenateur(senateurId, q, pageable);
    }

    // ============================== DELETE ==============================

    public void delete(Long id) {
        PieceJointe pj = getById(id);
        String file = pj.getFileName();
        repo.delete(pj);
        if (file != null && !file.isBlank()) {
            deleteQuiet(file);
        }
    }

    // ============================== HELPERS ==============================

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Aucun fichier fourni.");
        }
        String original = originalName(file);
        String clean = StringUtils.cleanPath(original).replaceAll("\\s+", "_");
        if (clean.isEmpty() || !clean.contains(".")) {
            throw new BusinessException("Nom de fichier invalide.");
        }
        String ext = clean.substring(clean.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!"pdf".equals(ext)) {
            throw new BusinessException("Seuls les fichiers PDF sont acceptés.");
        }
        // MIME best-effort
        String mime = file.getContentType();
        if (mime != null && !mime.equalsIgnoreCase("application/pdf")) {
            log.warn("MIME inattendu pour PDF: {}", mime);
        }
    }

    private String computeSha256(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) { /* stream */ }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException("Impossible de calculer le checksum du fichier.");
        }
    }

    private String storeDocument(MultipartFile file) throws Exception {
        Path directory = Paths.get(docsDir);
        if (!Files.exists(directory)) Files.createDirectories(directory);

        String original = originalName(file);
        String clean = StringUtils.cleanPath(original).replaceAll("\\s+", "_");
        String ext = clean.substring(clean.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

        short rnd = (short) new Random().nextInt(1 << 15);
        String ts = new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());
        String newName = ts + rnd + "." + ext;

        Path target = directory.resolve(newName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        log.info("Document PDF uploadé: {}", newName);
        return newName;
        }

    private void deleteQuiet(String storedName) {
        try {
            Path p = Paths.get(docsDir).resolve(storedName);
            Files.deleteIfExists(p);
            log.info("Document supprimé: {}", p);
        } catch (Exception ex) {
            log.warn("Echec suppression fichier '{}': {}", storedName, ex.getMessage());
        }
    }

    private static String originalName(MultipartFile f) {
        return Objects.requireNonNullElse(f.getOriginalFilename(), "document.pdf");
    }
}
