package cd.senat.medical.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;           // ✅ MANQUANT
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;              // ✅ MANQUANT
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Service("medicalQrCodeService")  // Nom unique pour éviter conflit TOTP
public class QrCodeService {

    public byte[] png(String text, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            out.close();  // Bonne pratique
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Génération QR échouée", e);
        }
    }
}
