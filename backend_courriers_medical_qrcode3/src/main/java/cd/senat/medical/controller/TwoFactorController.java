package cd.senat.medical.controller;

import cd.senat.medical.dto.TwoFactorSetupResponse;
import cd.senat.medical.dto.TwoFactorVerifyRequest;
import cd.senat.medical.entity.User;
import cd.senat.medical.repository.UserRepository;
import cd.senat.medical.security.totp.TotpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
@CrossOrigin(origins = "*")
public class TwoFactorController {

    @Autowired private UserRepository userRepository;
    @Autowired private TotpService totpService;

    /**
     * (Optionnel) Status 2FA pour le front
     */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication authentication) {
        User principal = (User) authentication.getPrincipal();

        User dbUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        return ResponseEntity.ok(Map.of(
                "twoFactorEnabled", Boolean.TRUE.equals(dbUser.getTwoFactorEnabled()),
                "hasSecret", dbUser.getTwoFactorSecret() != null && !dbUser.getTwoFactorSecret().isBlank()
        ));
    }

    /**
     * 1) Génère secret + QR, stocke le secret (mais n’active pas 2FA)
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setup(Authentication authentication) {
        User principal = (User) authentication.getPrincipal();

        User dbUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // ✅ recommandé : refuser si user désactivé
        if (dbUser.getActive() != null && !dbUser.getActive()) {
            return ResponseEntity.status(403).body(Map.of("message", "Compte désactivé"));
        }

        if (Boolean.TRUE.equals(dbUser.getTwoFactorEnabled())) {
            return ResponseEntity.badRequest().body(Map.of("message", "2FA déjà activé"));
        }

        // ✅ si secret déjà présent mais 2FA pas activé, on régénère proprement
        // (ou tu peux retourner "setup déjà fait" selon ton choix)
        String secret = totpService.generateSecret();

        String label = (dbUser.getEmail() != null && !dbUser.getEmail().isBlank())
                ? dbUser.getEmail()
                : dbUser.getUsername();

        String otpauthUrl = totpService.buildOtpAuthUrl(label, secret);
        String qrDataUrl = totpService.generateQrCodeDataUrl(otpauthUrl);

        dbUser.setTwoFactorSecret(secret);
        dbUser.setTwoFactorEnabled(false);
        dbUser.setTwoFactorEnabledAt(null);
        userRepository.save(dbUser);

        // 🔒 Optionnel sécurité: ne pas renvoyer secret (tu peux le laisser si tu veux)
        return ResponseEntity.ok(new TwoFactorSetupResponse(secret, otpauthUrl, qrDataUrl));
    }

    /**
     * 2) Vérifie code et ACTIVE 2FA
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyAndEnable(@Valid @RequestBody TwoFactorVerifyRequest req,
                                             Authentication authentication) {
        User principal = (User) authentication.getPrincipal();

        User dbUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (dbUser.getTwoFactorSecret() == null || dbUser.getTwoFactorSecret().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Setup requis (secret absent)"));
        }

        boolean ok = totpService.verifyCode(dbUser.getTwoFactorSecret(), req.getCode());
        if (!ok) {
            return ResponseEntity.status(401).body(Map.of("message", "Code invalide"));
        }

        dbUser.setTwoFactorEnabled(true);
        dbUser.setTwoFactorEnabledAt(totpService.now());
        userRepository.save(dbUser);

        return ResponseEntity.ok(Map.of("message", "2FA activé"));
    }

    /**
     * 3) Désactive 2FA (OTP requis)
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disable(@Valid @RequestBody TwoFactorVerifyRequest req,
                                     Authentication authentication) {
        User principal = (User) authentication.getPrincipal();

        User dbUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!Boolean.TRUE.equals(dbUser.getTwoFactorEnabled())) {
            return ResponseEntity.badRequest().body(Map.of("message", "2FA non activé"));
        }

        if (dbUser.getTwoFactorSecret() == null || dbUser.getTwoFactorSecret().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Secret absent (setup requis)"));
        }

        boolean ok = totpService.verifyCode(dbUser.getTwoFactorSecret(), req.getCode());
        if (!ok) {
            return ResponseEntity.status(401).body(Map.of("message", "Code invalide"));
        }

        dbUser.setTwoFactorEnabled(false);
        dbUser.setTwoFactorSecret(null);
        dbUser.setTwoFactorEnabledAt(null);
        userRepository.save(dbUser);

        return ResponseEntity.ok(Map.of("message", "2FA désactivé"));
    }
}
