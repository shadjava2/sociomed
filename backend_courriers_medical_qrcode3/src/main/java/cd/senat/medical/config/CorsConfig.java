package cd.senat.medical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class CorsConfig {

    /** Origines supplémentaires (séparées par des virgules), ex: http://192.168.22.30:5173,http://192.168.177.247:5173 */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsConfig;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Patterns par défaut : localhost, dev (5173), prod Caddy (9080/9443), tout HTTPS
        List<String> patterns = Stream.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:3000",
            "http://localhost:9080",
            "http://127.0.0.1:9080",
            "http://*:5173",            // dev réseau ex: http://192.168.22.30:5173
            "http://*:3000",
            "http://*:9080",             // prod Caddy (accès LAN ex: http://192.168.100.12:9080)
            "http://*:9443",
            "https://*"
        ).collect(Collectors.toList());

        if (StringUtils.hasText(allowedOriginsConfig)) {
            List<String> extra = Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
            patterns.addAll(extra);
        }

        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

