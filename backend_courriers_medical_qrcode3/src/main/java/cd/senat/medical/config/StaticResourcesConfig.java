package cd.senat.medical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourcesConfig implements WebMvcConfigurer {

    /**
     * Même propriété que dans tes contrôleurs.
     * Par défaut : {user.dir}/uploads/photos/
     */
    @Value("${app.upload.dir:#{systemProperties['user.dir']}/uploads/photos/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Répertoire absolu normalisé
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // Important : Spring attend une URL de type "file:/.../"
        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }

        registry.addResourceHandler("/uploads/photos/**")
                .addResourceLocations(location)          // ex: file:/.../uploads/photos/
                .setCachePeriod(31536000)                // 1 an (en secondes) – optionnel
                .resourceChain(true);
    }
}
