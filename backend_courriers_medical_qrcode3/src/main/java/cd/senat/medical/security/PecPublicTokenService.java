package cd.senat.medical.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class PecPublicTokenService {

    private final Key key;

    public PecPublicTokenService(@Value("${jwt.secret}") String secretBase64OrRaw) {
        // Ton secret dans yml ressemble déjà à une Base64. On tente Base64 d’abord.
        byte[] keyBytes;
        try {
            keyBytes = java.util.Base64.getDecoder().decode(secretBase64OrRaw);
        } catch (Exception ignore) {
            keyBytes = secretBase64OrRaw.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(Long pecId, long ttlMs) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .setSubject("PEC_PUBLIC")
                .claim("pecId", pecId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long parsePecId(String token) {
        Claims claims = parseClaims(token);
        Object v = claims.get("pecId");
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(v));
    }

    /** Date d'expiration du token (pour affichage "Actif" / "Expiré" sur la page de vérification). */
    public Date getExpiration(String token) {
        try {
            return parseClaims(token).getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
