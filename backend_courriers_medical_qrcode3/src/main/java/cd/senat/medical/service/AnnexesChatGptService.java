// src/main/java/fr/senat/courriersaudiences/service/AnnexesChatGptService.java
package cd.senat.medical.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import cd.senat.medical.dto.AnnexeDTO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnnexesChatGptService {

    private static final Logger log = LoggerFactory.getLogger(AnnexesChatGptService.class);

    /** Protège l'appel LLM (tokens) */
    private static final int MAX_CONTENT_CHARS = 12000;

    private final AnnexesStorageService storage;
    private final AnnexeService annexeService;
    private final OcrService ocrService;
    private final ChatGptService chatGptService;

    public AnnexesChatGptService(AnnexesStorageService storage,
                                 AnnexeService annexeService,
                                 OcrService ocrService,
                                 ChatGptService chatGptService) {
        this.storage = storage;
        this.annexeService = annexeService;
        this.ocrService = ocrService;
        this.chatGptService = chatGptService;
    }

    // ======================= API PUBLIC =======================

    public String summarize(Long courrierId, Long annexeId, Map<String, Object> ctx) {
        try {
            LoadedAnnexe la = loadAnnexe(courrierId, annexeId);
            ExtractedContent ec = extractTextWithOcrFallback(la);

            if (ec.text() == null || ec.text().isBlank()) {
                return """
                        Impossible d'extraire du texte de l'annexe (scan/image non OCR ou fichier vide).
                        Vérifiez la configuration OCR (Tesseract) ou fournissez un PDF textuel.
                        """;
            }

            String prompt = buildSummaryPrompt(ec.text(), la.filename());
            Map<String, Object> context = baseContext(la, "summary", ctx);
            context.put("extraction_source", ec.source());
            if (ec.note() != null && !ec.note().isBlank()) {
                context.put("extraction_note", ec.note());
            }

            return chatGptService.ask(prompt, context);

        } catch (Exception e) {
            log.warn("summarize failed: {}", e.toString(), e);
            return "Erreur lors du résumé: " + e.getMessage();
        }
    }

    public String draftReply(Long courrierId, Long annexeId, String instruction, Map<String, Object> ctx) {
        try {
            LoadedAnnexe la = loadAnnexe(courrierId, annexeId);
            ExtractedContent ec = extractTextWithOcrFallback(la);

            if (ec.text() == null || ec.text().isBlank()) {
                return """
                        Impossible d'extraire du texte de l'annexe (scan/image non OCR ou fichier vide).
                        Vérifiez la configuration OCR (Tesseract) ou fournissez un PDF textuel.
                        """;
            }

            String prompt = buildReplyPrompt(ec.text(), la.filename(), (instruction == null ? "" : instruction));
            Map<String, Object> context = baseContext(la, "reply", ctx);
            context.put("extraction_source", ec.source());
            if (ec.note() != null && !ec.note().isBlank()) {
                context.put("extraction_note", ec.note());
            }
            if (instruction != null && !instruction.isBlank()) {
                context.put("instruction", instruction);
            }

            return chatGptService.ask(prompt, context);

        } catch (Exception e) {
            log.warn("draftReply failed: {}", e.toString(), e);
            return "Erreur lors de la génération de la réponse: " + e.getMessage();
        }
    }

    // ======================= EXTRACTION =======================

    private record LoadedAnnexe(Long id, Long courrierId, String filename, String mime, Path path) {}
    private record ExtractedContent(String text, String source, String note) {}

    private LoadedAnnexe loadAnnexe(Long courrierId, Long annexeId) throws Exception {
        List<AnnexeDTO> list = annexeService.listByCourrier(courrierId);
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("Aucune annexe pour ce courrier.");
        }
        AnnexeDTO dto = list.stream()
                .filter(a -> Objects.equals(safeId(a), annexeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Annexe introuvable pour ce courrier."));

        if (dto.getUrl() == null || dto.getUrl().isBlank()) {
            throw new IllegalArgumentException("Chemin de l'annexe manquant.");
        }
        Path p = storage.resolve(dto.getUrl());
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("Fichier annexe manquant sur le disque.");
        }
        String mime = safeMime(p, dto.getType());
        String filename = (dto.getNom() == null || dto.getNom().isBlank()) ? "annexe" : dto.getNom();
        return new LoadedAnnexe(safeId(dto), courrierId, filename, mime, p);
    }

    private Long safeId(AnnexeDTO a) {
        try {
            return (a.getId() == null) ? null : Long.valueOf(String.valueOf(a.getId()));
        } catch (Exception e) {
            return null;
        }
    }

    private ExtractedContent extractTextWithOcrFallback(LoadedAnnexe la) {
        String note = "";
        // 1) Essai d'extraction "native" (PDF texte / TXT)
        try {
            String nativeText = extractNativeText(la.path(), la.mime());
            if (nativeText != null && nativeText.strip().length() >= 10) {
                String cleaned = normalizeAndTrim(nativeText);
                return new ExtractedContent(cleaned, "native", "");
            } else {
                note = "Aucun texte natif détecté ou très faible; tentative OCR.";
            }
        } catch (Throwable t) {
            note = "Extraction native échouée: " + t.getClass().getSimpleName();
            log.warn("Native text extraction failed on {}: {}", la.path(), t.toString());
        }

        // 2) OCR fallback
        String ocrText = "";
        try {
            // Selon ta signature de service OCR :
            // - si tu as ocrAuto(Path, String) garde la ligne suivante,
            // - sinon remplace par ocrService.ocrAuto(la.path()).
            ocrText = ocrService.ocrAuto(la.path(), la.mime());
        } catch (Throwable t) {
            // IMPORTANT : on capte tout (y compris erreurs natives JNA) pour éviter 500
            String msg = "OCR échoué: " + t.getClass().getSimpleName();
            note = note.isBlank() ? msg : (note + " | " + msg);
            log.warn("OCR failed on {}: {}", la.path(), t.toString());
        }

        if (ocrText != null && !ocrText.isBlank()) {
            String cleaned = normalizeAndTrim(ocrText);
            return new ExtractedContent(cleaned, "ocr", note);
        }

        // 3) Rien extrait
        return new ExtractedContent("", "none", note);
    }

    private String extractNativeText(Path p, String mime) throws Exception {
        String lower = (mime == null ? "" : mime.toLowerCase(Locale.ROOT));
        String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);

        // PDF textuel
        if (lower.contains("pdf") || fn.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(p.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String s = stripper.getText(doc);
                return (s == null ? "" : s.trim());
            }
        }

        // TXT
        if (lower.contains("text") || fn.endsWith(".txt")) {
            return Files.readString(p);
        }

        // Images / binaire → pas de texte natif
        return "";
    }

    private String normalizeAndTrim(String s) {
        if (s == null) return "";
        // espaces → 1, grandes suites de sauts de ligne → 2
        String compact = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        compact = compact.replaceAll("\\n{3,}", "\n\n");
        // supprime lignes vides multiples et trim
        List<String> lines = Arrays.stream(compact.split("\\n"))
                .map(String::trim)
                .collect(Collectors.toList());
        String joined = String.join("\n", lines).trim();
        if (joined.length() > MAX_CONTENT_CHARS) {
            return joined.substring(0, MAX_CONTENT_CHARS) + "\n[Texte tronqué…]";
        }
        return joined;
    }

    private String safeMime(Path p, String fromDb) {
        try {
            String guess = Files.probeContentType(p);
            if (guess != null) return guess;
        } catch (Exception ignored) {}
        if (fromDb != null && !fromDb.isBlank()) return fromDb;
        String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fn.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
        if (fn.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (fn.endsWith(".jpg") || fn.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (fn.endsWith(".txt")) return MediaType.TEXT_PLAIN_VALUE;
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    // ======================= PROMPTS =======================

    private String buildSummaryPrompt(String content, String filename) {
        return """
               Tu es un assistant qui résume des pièces jointes administratives en français.
               - Donne un résumé clair en puces (max 10).
               - Mets en évidence : dates, montants, obligations, demandes.
               - Termine par 3 recommandations actionnables.
               Fichier: %s

               ====== CONTENU ANNEXE (début) ======
               %s
               ====== CONTENU ANNEXE (fin) ======
               """.formatted((filename == null ? "annexe" : filename), content);
    }

    private String buildReplyPrompt(String content, String filename, String instruction) {
        return """
               Rédige un projet de réponse officielle en français, poli et concis, fondé sur l'annexe ci-dessous.
               - Ton institutionnel.
               - Structure : Objet, Corps (2-3 paragraphes), Formule de politesse.
               - Respecte l'instruction : "%s".
               Fichier: %s

               ====== CONTENU ANNEXE (début) ======
               %s
               ====== CONTENU ANNEXE (fin) ======
               """.formatted(instruction, (filename == null ? "annexe" : filename), content);
    }

    private Map<String, Object> baseContext(LoadedAnnexe la, String mode, Map<String, Object> extra) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("source", "annexe");
        ctx.put("mode", mode);
        ctx.put("filename", la.filename());
        ctx.put("mime", la.mime());
        if (extra != null && !extra.isEmpty()) {
            ctx.putAll(extra); // ex. frontend_context / delai / urgence
        }
        return ctx;
    }
}
