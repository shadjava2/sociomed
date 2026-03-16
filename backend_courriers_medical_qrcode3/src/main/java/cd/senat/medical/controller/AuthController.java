package cd.senat.medical.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import cd.senat.medical.dto.JwtResponse;
import cd.senat.medical.dto.LoginRequest;
import cd.senat.medical.entity.User;
import cd.senat.medical.repository.UserRepository;
import cd.senat.medical.security.JwtUtils;
import cd.senat.medical.security.totp.TotpService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UserRepository userRepository;
    @Autowired private TotpService totpService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        logger.info("🔑 Tentative de connexion avec username={}", loginRequest.getUsername());

        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            // ✅ message personnalisé
            return ResponseEntity.status(401).body(Map.of(
                    "message", "Identifiants incorrects (nom d'utilisateur ou mot de passe)."
            ));
        }

        User principal = (User) authentication.getPrincipal();

        // ✅ Recharge DB (récupère 2FA secret/enabled + état active)
        User dbUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // ✅ Refuser si user désactivé
        if (dbUser.getActive() != null && !dbUser.getActive()) {
            return ResponseEntity.status(403).body(Map.of("message", "Compte désactivé"));
        }

        // ✅ Si 2FA activé : OTP obligatoire
        if (Boolean.TRUE.equals(dbUser.getTwoFactorEnabled())) {

            // Secret absent => incohérence DB
            if (dbUser.getTwoFactorSecret() == null || dbUser.getTwoFactorSecret().isBlank()) {
                return ResponseEntity.status(409).body(Map.of(
                        "message", "2FA activé mais secret absent. Contactez l’administrateur."
                ));
            }

            String otp = loginRequest.getOtp();

            // OTP manquant => demande au front d'afficher l'écran OTP
            if (otp == null || otp.isBlank()) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "OTP requis",
                        "twoFactorRequired", true
                ));
            }

            boolean ok = totpService.verifyCode(dbUser.getTwoFactorSecret(), otp);
            if (!ok) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "OTP invalide",
                        "twoFactorRequired", true
                ));
            }
        }

        // ✅ On définit le contexte seulement après OTP OK (propre)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ✅ Génération JWT final
        String jwt = jwtUtils.generateJwtToken(authentication);

        logger.info("🎫 JWT généré pour l’utilisateur id={}, username={}, rôle={}",
                dbUser.getId(), dbUser.getUsername(), dbUser.getRole());

        return ResponseEntity.ok(new JwtResponse(jwt,
                dbUser.getId(),
                dbUser.getUsername(),
                dbUser.getEmail(),
                dbUser.getNom(),
                dbUser.getPrenom(),
                dbUser.getRole()));
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        logger.info("👋 Déconnexion de l’utilisateur courant");
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie!"));
    }
}
