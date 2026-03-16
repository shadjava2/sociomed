package cd.senat.medical.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface AnnexesStorageService {
    String store(MultipartFile file);                    // retourne le nom stocké (filename)
    Resource loadAsResource(String filename);
    Path resolve(String filename);
}
