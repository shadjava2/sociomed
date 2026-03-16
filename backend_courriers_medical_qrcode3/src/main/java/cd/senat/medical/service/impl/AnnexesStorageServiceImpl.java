package cd.senat.medical.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import cd.senat.medical.service.AnnexesStorageService;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

@Service
public class AnnexesStorageServiceImpl implements AnnexesStorageService {

    private final Path root;

    public AnnexesStorageServiceImpl(@Value("${app.annexes.dir:annexes_upload}") String dir) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.root); // crée le dossier s'il n'existe pas
    }

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        // nom unique
        String stored = System.currentTimeMillis() + "_" + original.replaceAll("\\s+", "_");

        Path target = root.resolve(stored).normalize();

        // anti-traversal
        if (!target.toAbsolutePath().startsWith(root)) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Échec d'écriture du fichier", e);
        }
        return stored;
    }

    @Override
    public Resource loadAsResource(String filename) {
        Path p = resolve(filename);
        if (!Files.exists(p)) throw new IllegalArgumentException("Fichier introuvable");
        return new FileSystemResource(p.toFile());
    }

    @Override
    public Path resolve(String filename) {
        Path p = root.resolve(filename).normalize();
        if (!p.toAbsolutePath().startsWith(root)) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }
        return p;
    }
}
