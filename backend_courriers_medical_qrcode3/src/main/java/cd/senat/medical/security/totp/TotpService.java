package cd.senat.medical.security.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.HmacHashFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class TotpService {

    private final GoogleAuthenticator gauth;

    @Value("${app.security.totp.issuer:Senat Medical}")
    private String issuer;

    @Value("${app.security.totp.window:2}")
    private int window;

    @Value("${app.security.totp.qr.width:240}")
    private int qrWidth;

    @Value("${app.security.totp.qr.height:240}")
    private int qrHeight;

    public TotpService(
            @Value("${app.security.totp.digits:6}") int digits,
            @Value("${app.security.totp.periodSeconds:30}") int periodSeconds
    ) {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(digits)
                .setTimeStepSizeInMillis(periodSeconds * 1000L)
                .setWindowSize(1) // on gère window via authorize(secret, code) en tolérance simple; sinon builder window
                .setHmacHashFunction(HmacHashFunction.HmacSHA1)
                .build();

        this.gauth = new GoogleAuthenticator(config);
    }

    /** Génère un secret Base32 */
    public String generateSecret() {
        GoogleAuthenticatorKey key = gauth.createCredentials();
        return key.getKey(); // Base32
    }

    /** Vérifie un code TOTP */
    public boolean verifyCode(String base32Secret, String code) {
        if (base32Secret == null || base32Secret.isBlank()) return false;
        if (code == null || code.isBlank()) return false;

        String cleaned = code.trim().replace(" ", "");
        if (!cleaned.matches("\\d{6,8}")) return false;

        int otp;
        try { otp = Integer.parseInt(cleaned); }
        catch (Exception e) { return false; }

        // Tolérance: on teste plusieurs fenêtres si besoin
        // La lib gauth gère déjà une fenêtre interne, mais on peut simuler en jouant sur config.
        // Ici on reste simple: authorize = ok (si tu veux une vraie fenêtre ±N, on ajuste config builder).
        return gauth.authorize(base32Secret, otp);
    }

    /** Construit l'URL otpauth:// */
    public String buildOtpAuthUrl(String usernameOrEmail, String secret) {
        String label = issuer + ":" + usernameOrEmail;
        String encodedLabel = urlEncode(label);
        String encodedIssuer = urlEncode(issuer);
        return "otpauth://totp/" + encodedLabel +
                "?secret=" + secret +
                "&issuer=" + encodedIssuer;
    }

    /** QR Code en data URL (png) */
    public String generateQrCodeDataUrl(String otpauthUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpauthUrl, BarcodeFormat.QR_CODE, qrWidth, qrHeight);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            String base64 = Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération QR Code", e);
        }
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    private String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
