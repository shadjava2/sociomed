package cd.senat.medical.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecretBase64;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${jwt.algorithm:HS256}")
    private String algName;

    private Key signingKey() {
        // La clé doit être la **décodée** Base64 (≥256 bits pour HS256)
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private SignatureAlgorithm algorithm() {
        // Permet HS256/384/512 via config si besoin
        return switch (algName) {
            case "HS384" -> SignatureAlgorithm.HS384;
            case "HS512" -> SignatureAlgorithm.HS512;
            default -> SignatureAlgorithm.HS256;
        };
    }

    public String generateJwtToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(signingKey(), algorithm())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            // signature invalide / token mal formé
        } catch (ExpiredJwtException e) {
            // token expiré
        } catch (UnsupportedJwtException e) {
            // algo/format non supporté
        } catch (IllegalArgumentException e) {
            // claims vides
        }
        return false;
        // (journalise à ta guise si nécessaire)
    }
}
