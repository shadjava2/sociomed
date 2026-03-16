package cd.senat.medical.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.*;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"message\":\"Fichier vide\"}");
        }

        Files.createDirectories(Path.of(uploadDir));

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String storedName = System.currentTimeMillis() + "_" + originalName;
        Path target = Path.of(uploadDir).resolve(storedName).normalize();

        // Sécurité contre path traversal
        if (!target.toAbsolutePath().startsWith(Path.of(uploadDir).toAbsolutePath())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\":\"Nom de fichier invalide\"}");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        var contentType = file.getContentType();
        long size = file.getSize();

        return ResponseEntity.ok(new UploadResponse(originalName, storedName, contentType, size));
    }

    @GetMapping("/view/{filename}")
    public ResponseEntity<Resource> view(@PathVariable String filename) throws Exception {
        Path path = Path.of(uploadDir).resolve(filename).normalize();
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(path);
        Resource resource = new FileSystemResource(path.toFile());

        return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) throws Exception {
        Path path = Path.of(uploadDir).resolve(filename).normalize();
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(path);
        Resource resource = new FileSystemResource(path.toFile());

        return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<?> delete(@PathVariable String filename) throws Exception {
        Path path = Path.of(uploadDir).resolve(filename).normalize();
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Files.delete(path);
        return ResponseEntity.noContent().build();
    }

    public record UploadResponse(String originalName, String filename, String contentType, long size) {}
}
