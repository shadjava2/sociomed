package cd.senat.medical.security.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeService {

    /**
     * Génère un QR Code PNG (bytes) à partir d'un texte (otpauthUrl).
     */
    public byte[] generatePng(String text) {
        return generatePng(text, 280, 280);
    }

    /**
     * Génère un QR Code PNG (bytes) avec taille personnalisée.
     */
    public byte[] generatePng(String text, int width, int height) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("QR text is blank");
        }

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // bordure

            BitMatrix matrix = new MultiFormatWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
            );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération QR Code", e);
        }
    }
}
