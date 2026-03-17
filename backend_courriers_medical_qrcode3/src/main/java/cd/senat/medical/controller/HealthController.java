package cd.senat.medical.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de health pour le déploiement et les load balancers.
 * Caddy garde le chemin (handle /api/*) donc les deux chemins sont exposés.
 */
@RestController
public class HealthController {

  @GetMapping({"/health", "/api/health"})
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("ok");
  }
}
