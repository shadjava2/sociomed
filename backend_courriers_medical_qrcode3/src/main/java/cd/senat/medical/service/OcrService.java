package cd.senat.medical.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class OcrService {

    @Value("${annexes.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${annexes.ocr.datapath:}")
    private String dataPath;

    @Value("${annexes.ocr.language:fra+eng}")
    private String language;

    @Value("${annexes.ocr.dpi:300}")
    private int dpi;

    @Value("${annexes.ocr.max-pages:10}")
    private int maxPages;

    private ITesseract newEngine() {
        Tesseract t = new Tesseract();
        if (dataPath != null && !dataPath.isBlank()) {
            t.setDatapath(dataPath);
        }
        t.setLanguage(language); // ex: "fra+eng"
        // t.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY); // optionnel
        return t;
    }

    /** OCR pour images directes (PNG/JPG) */
    public String ocrImage(Path imagePath) throws IOException, TesseractException {
        if (!ocrEnabled) return "";
        BufferedImage img = ImageIO.read(imagePath.toFile());
        if (img == null) return "";
        return newEngine().doOCR(img);
    }

    /** OCR pour PDF scannés : rendu en images puis OCR page par page */
    public String ocrPdf(Path pdfPath) throws IOException, TesseractException {
        if (!ocrEnabled) return "";
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder sb = new StringBuilder();
            int pages = Math.min(doc.getNumberOfPages(), Math.max(1, maxPages));
            ITesseract engine = newEngine();
            for (int i = 0; i < pages; i++) {
                BufferedImage pageImg = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                String text = engine.doOCR(pageImg);
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n\n");
                }
            }
            return sb.toString().trim();
        }
    }

    /** Détecte type et applique la bonne méthode OCR */
    public String ocrAuto(Path filePath, String mime) throws IOException, TesseractException {
        String lower = (mime == null ? "" : mime.toLowerCase());
        if (lower.contains("pdf")) {
            return ocrPdf(filePath);
        }
        if (lower.contains("png") || lower.contains("jpeg") || lower.contains("jpg")) {
            return ocrImage(filePath);
        }
        // Si le type est inconnu, tente par extension
        String fn = filePath.getFileName().toString().toLowerCase();
        if (fn.endsWith(".pdf")) return ocrPdf(filePath);
        if (fn.endsWith(".png") || fn.endsWith(".jpg") || fn.endsWith(".jpeg")) return ocrImage(filePath);
        // sinon: pas d’OCR
        return "";
    }

    public boolean isLikelyBinaryOrEmpty(Path filePath) {
        try {
            long size = Files.size(filePath);
            return size <= 4 * 1024; // < 4KB : probablement vide
        } catch (Exception e) {
            return false;
        }
    }
}
