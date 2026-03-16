package cd.senat.medical.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de health pour le déploiement et les load balancers.
 * Caddy transmet /api/health → backend en /health. Sans auth (WebSecurityConfig autorise /api/health).
 */
@RestController
public class HealthController {

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("ok");
  }
}
