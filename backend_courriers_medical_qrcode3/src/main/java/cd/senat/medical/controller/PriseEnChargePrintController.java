package cd.senat.medical.controller;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cd.senat.medical.dto.PecVerifyResponse;
import cd.senat.medical.dto.PriseEnChargeDTO;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.security.PecPublicTokenService;
import cd.senat.medical.service.PriseEnChargeService;
import cd.senat.medical.service.QrCodeService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@RestController
@RequestMapping("/api/pec")
@CrossOrigin(origins = "*")
public class PriseEnChargePrintController {

  private static final Logger log = LoggerFactory.getLogger(PriseEnChargePrintController.class);

  private final PriseEnChargeService service;
  private final PecPublicTokenService pecPublicTokenService;
  private final QrCodeService qrCodeService;

  public PriseEnChargePrintController(PriseEnChargeService service,
      PecPublicTokenService pecPublicTokenService, QrCodeService qrCodeService) {
    this.service = service;
    this.pecPublicTokenService = pecPublicTokenService;
    this.qrCodeService = qrCodeService;
  }

  @Value("${app.upload.dir:#{systemProperties['user.dir']}/uploads/photos/}")
  private String uploadDir;

  @Value("${app.public.base-url:}")
  private String publicBaseUrl;

  @Value("${app.front.base-url:}")
  private String frontBaseUrl;

  @Value("${app.pec.qr-mode:pdf}")
  private String qrMode;

  @GetMapping(value = "/{id}/print", produces = "application/pdf")
  public void print(@PathVariable Long id,
      @RequestParam(required = false) String backgroundImageUrl,
      HttpServletRequest req, HttpServletResponse resp) {
    try {
      generateAndStreamPdf(id, req, resp, false, backgroundImageUrl);
    } catch (ResourceNotFoundException rnfe) {
      log.warn("print PEC: {}", rnfe.getMessage());
      sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Note de prise en charge introuvable.");
    } catch (Exception e) {
      log.error("Erreur print PEC", e);
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Erreur lors de la génération du PDF.");
    }
  }

  @GetMapping(value = "/public/{token}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public void publicPdf(@PathVariable String token,
      @RequestParam(required = false) String backgroundImageUrl,
      HttpServletRequest req, HttpServletResponse resp) {
    try {
      Long pecId = pecPublicTokenService.parsePecId(token);
      generateAndStreamPdf(pecId, req, resp, true, backgroundImageUrl);
    } catch (ExpiredJwtException eje) {
      sendError(resp, HttpServletResponse.SC_GONE, "Lien expiré.");
    } catch (JwtException je) {
      sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Lien invalide.");
    } catch (Exception e) {
      log.error("Erreur publicPdf PEC", e);
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Erreur lors de la génération du PDF.");
    }
  }

  private static PecVerifyResponse errorResponse(String status, String message) {
    return new PecVerifyResponse(status, message, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null);
  }

  @GetMapping(value = "/public/{token}/verify", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PecVerifyResponse> verify(@PathVariable String token,
      HttpServletRequest req) {
    try {
      Long pecId = pecPublicTokenService.parsePecId(token);
      Date tokenExpiresAt = pecPublicTokenService.getExpiration(token);

      PriseEnChargeDTO.Detail d = service.getByIdWithRelations(pecId);
      if (d == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new PecVerifyResponse("NOT_FOUND", "PEC introuvable.", pecId, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null));
      }

      String base = resolveBaseUrl(req);
      String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
      String pdfUrl = base + "/api/pec/public/" + encToken + "/pdf";
      String photoUrl = (d.photo() != null && !d.photo().isBlank())
          ? base + "/api/pec/public/" + encToken + "/photo"
          : null;

      String genreStr = d.genre() != null ? d.genre().name() : null;

      return ResponseEntity.ok(new PecVerifyResponse("VALID", "Document authentique (lien valide).",
          pecId, d.numero(), safe(d.nom()), safe(d.postnom()), safe(d.prenom()), genreStr, d.age(),
          safe(d.adresseMalade()), safe(d.qualiteMalade()), safe(d.etablissement()),
          safe(d.motif()), d.dateEmission(), d.dateExpiration(), safe(d.createdByFullname()),
          photoUrl, tokenExpiresAt, pdfUrl));

    } catch (ExpiredJwtException eje) {
      return ResponseEntity.status(HttpStatus.GONE).body(errorResponse("EXPIRED", "Lien expiré."));
    } catch (JwtException je) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(errorResponse("INVALID", "Lien invalide."));
    } catch (Exception e) {
      log.error("Erreur verify PEC", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(errorResponse("ERROR", "Erreur serveur."));
    }
  }

  @GetMapping(value = "/public/{token}/photo",
      produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
  public void publicPhoto(@PathVariable String token, HttpServletResponse resp) {
    try {
      Long pecId = pecPublicTokenService.parsePecId(token);
      PriseEnChargeDTO.Detail d = service.getById(pecId);
      if (d == null || d.photo() == null || d.photo().isBlank()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      String photoFilename = normalizeFilename(d.photo());
      if (photoFilename == null || photoFilename.isBlank()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
      Path file = base.resolve(photoFilename).normalize();
      if (!file.startsWith(base) || !Files.exists(file) || !Files.isReadable(file)) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      byte[] bytes = Files.readAllBytes(file);
      String lower = photoFilename.toLowerCase();
      String contentType =
          (lower.endsWith(".png")) ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
      resp.setContentType(contentType);
      resp.setContentLength(bytes.length);
      resp.getOutputStream().write(bytes);
      resp.flushBuffer();
    } catch (JwtException e) {
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    } catch (Exception e) {
      log.warn("Erreur publicPhoto PEC: {}", e.getMessage());
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @GetMapping("/public/{token}/page")
  public void publicPage(@PathVariable String token, HttpServletRequest req,
      HttpServletResponse resp) {
    try {
      String front = resolveFrontBaseUrl(req);
      String location = front + "/public/pec/" + URLEncoder.encode(token, StandardCharsets.UTF_8);
      resp.sendRedirect(location);
    } catch (Exception e) {
      log.error("Erreur publicPage PEC", e);
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur redirection.");
    }
  }

  @GetMapping(value = "/print-listing", produces = MediaType.APPLICATION_PDF_VALUE)
  public void printListing(@RequestParam(required = false) Long hopitalId,
      @RequestParam(required = false) String month, @RequestParam(defaultValue = "5000") int limit,
      @RequestParam(required = false) String backgroundImageUrl,
      HttpServletResponse resp) {
    try {
      List<PriseEnChargeDTO.ListByHopitalItem> rows =
          service.listForListing(hopitalId, month, limit);

      Map<String, Object> params = new HashMap<>();
      params.put("REPORT_TITLE", "LISTE DES PRISES EN CHARGE");
      params.put("HOPITAL_LABEL", resolveHopitalLabel(hopitalId));
      params.put("MONTH_LABEL", month != null && !month.isBlank() ? month : "Tous");
      params.put("PRINTED_AT", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));

      params.put("logo", loadLogoBytes());
      byte[] bgBytes = (backgroundImageUrl != null && !backgroundImageUrl.isBlank())
          ? fetchImageBytesFromUrl(backgroundImageUrl) : null;
      params.put("backgroundImage", bgBytes != null ? bgBytes : loadBackgroundImageBytes("reports/fond_paysage_a4.png"));

      var df = new SimpleDateFormat("dd/MM/yyyy");
      List<Map<String, Object>> data = rows.stream().map(r -> {
        Map<String, Object> m = new HashMap<>();
        m.put("numero", safe(r.numero()));
        m.put("dateEmissionStr",
            r.dateEmission() != null ? df.format(r.dateEmission()) : "");
        m.put("dateExpirationStr",
            r.dateExpiration() != null ? df.format(r.dateExpiration()) : "");
        m.put("statut", r.statut() != null ? r.statut() : "");
        m.put("hopitalNom", safe(r.hopitalNom()));
        m.put("beneficiaireNom", safe(r.beneficiaireNom()));
        m.put("beneficiaireQualite", safe(r.beneficiaireQualite()));
        m.put("etablissement", safe(r.etablissement()));
        m.put("motif", "");
        return m;
      }).collect(Collectors.toList());

      JasperReport report;
      try (
          InputStream jrxml = new ClassPathResource("reports/pec_listing.jrxml").getInputStream()) {
        report = JasperCompileManager.compileReport(jrxml);
      }

      JasperPrint jp =
          JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(data));

      String suffix = (month != null && !month.isBlank()) ? month : "all";
      String filename = "PEC-LISTING-" + suffix + ".pdf";

      resp.setContentType(MediaType.APPLICATION_PDF_VALUE);
      resp.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

      JasperExportManager.exportReportToPdfStream(jp, resp.getOutputStream());
      resp.flushBuffer();

    } catch (Exception e) {
      log.error("Erreur impression listing PEC", e);
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Erreur lors de la génération du listing PDF.");
    }
  }

  private void generateAndStreamPdf(Long id, HttpServletRequest req, HttpServletResponse resp,
      boolean isPublic, String backgroundImageUrl) throws Exception {
    PriseEnChargeDTO.Detail d = service.getById(id);
    if (d == null) {
      throw new ResourceNotFoundException("PEC introuvable: " + id);
    }

    Map<String, Object> params = new HashMap<>();
    params.put("numero", d.numero());

    var dfDate = new SimpleDateFormat("dd/MM/yyyy");
    var dfHeure = new SimpleDateFormat("HH:mm");
    Date em = d.dateEmission();
    params.put("dateEmission", em != null ? dfDate.format(em) : "");
    params.put("heureEmission", em != null ? dfHeure.format(em) : "");
    Date exp = d.dateExpiration();
    params.put("dateExpiration", exp != null ? dfDate.format(exp) : "");

    params.put("nom", safe(d.nom()));
    params.put("postnom", safe(d.postnom()));
    params.put("prenom", safe(d.prenom()));
    params.put("genre", d.genre() != null ? d.genre().name() : "");
    params.put("age", d.age() != null ? d.age() : "");

    params.put("adresseMalade", safe(d.adresseMalade()));
    params.put("qualiteMalade", safe(d.qualiteMalade()));
    params.put("etablissement", safe(d.etablissement()));
    params.put("motif", safe(d.motif()));
    params.put("createdByFullname", safe(d.createdByFullname()));

    byte[] logoBytes = loadLogoBytes();
    params.put("logo", logoBytes);
    byte[] bgBytes = (backgroundImageUrl != null && !backgroundImageUrl.isBlank())
        ? fetchImageBytesFromUrl(backgroundImageUrl) : null;
    params.put("backgroundImage", bgBytes != null ? bgBytes : loadBackgroundImageBytes("reports/fond_portrait_a4.png"));

    String rawPhoto = d.photo();
    String photoFilename = normalizeFilename(rawPhoto);

    byte[] photoAgentBytes = null;
    Path photoFilePath = null;
    if (photoFilename != null && !photoFilename.isBlank()) {
      try {
        Path uploadBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = uploadBase.resolve(photoFilename).normalize();
        if (file.startsWith(uploadBase) && Files.exists(file) && Files.isReadable(file)) {
          photoAgentBytes = Files.readAllBytes(file);
          photoFilePath = file;
        } else {
          log.warn("Fichier photo introuvable ou non lisible (répertoire: {}): {}", uploadBase, photoFilename);
        }
      } catch (Exception ex) {
        log.warn("Lecture bytes photo agent échouée pour '{}': {}", photoFilename, ex.toString());
      }
    }

    // Chemin fichier pour JasperReports (repli si chargement par URL échoue)
    if (photoFilePath != null) {
      String dir = photoFilePath.getParent().toString().replace('\\', '/');
      if (!dir.endsWith("/")) {
        dir += "/";
      }
      params.put("photoAgentPath", dir);
      params.put("photoAgentFilename", photoFilePath.getFileName().toString());
    }
    if (photoFilePath == null) {
      params.put("photoAgentPath", null);
      params.put("photoAgentFilename", null);
    }

    params.put("photoAgent", photoAgentBytes);

    URL photoAgentUrl = null;
    if (photoAgentBytes == null && photoFilename != null && !photoFilename.isBlank()) {
      try {
        String baseUrl = resolveBaseUrl(req);
        String enc = URLEncoder.encode(photoFilename, StandardCharsets.UTF_8);
        photoAgentUrl = new URL(baseUrl + "/api/agents/photos/" + enc);
      } catch (Exception e) {
        log.warn("Construction URL photo échouée: {}", e.toString());
      }
    }
    params.put("photoAgentUrl", photoAgentUrl);

    // Si aucune photo bénéficiaire : afficher le logo à la place pour éviter un cadre vide
    if (photoAgentBytes == null && photoAgentUrl == null && photoFilePath == null && logoBytes != null) {
      params.put("photoAgent", logoBytes);
    }

    String base = resolveBaseUrl(req);
    String frontBase = resolveFrontBaseUrlFromRequest(req);
    String token = pecPublicTokenService.createToken(id, 30L * 24 * 60 * 60 * 1000);
    String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);

    // QR en mode "page" → URL du frontend (dynamique : Origin/Referer de la requête en cours)
    // QR en mode "pdf" → URL du backend (téléchargement direct du PDF)
    String urlPdf = base + "/api/pec/public/" + encodedToken + "/pdf";
    String urlPageFront = frontBase + "/public/pec/" + encodedToken;
    String qrUrl = "page".equalsIgnoreCase(qrMode) ? urlPageFront : urlPdf;

    log.info("QR URL (PEC {}, mode {}, frontBase {}): {}", id, qrMode, frontBase, qrUrl);

    byte[] qrPng = qrCodeService.png(qrUrl, 300);
    params.put("qrcode", qrPng);

    JasperReport report;
    try (var jrxml = new ClassPathResource("reports/pec_note.jrxml").getInputStream()) {
      report = JasperCompileManager.compileReport(jrxml);
    }
    JasperPrint jp = JasperFillManager.fillReport(report, params, new JREmptyDataSource(1));

    String filename = "PEC-" + d.numero() + ".pdf";
    resp.setContentType(MediaType.APPLICATION_PDF_VALUE);
    resp.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
    JasperExportManager.exportReportToPdfStream(jp, resp.getOutputStream());
    resp.flushBuffer();
  }

  private String resolveHopitalLabel(Long hopitalId) {
    if (hopitalId == null) {
      return "Tous";
    }
    try {
      PriseEnChargeDTO.HopitalMini h = service.getHopitalMini(hopitalId);
      return h != null ? safe(h.nom()) : "Hôpital #" + hopitalId;
    } catch (Exception e) {
      return "Hôpital #" + hopitalId;
    }
  }

  private String resolveBaseUrl(HttpServletRequest req) {
    if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
      return publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
          : publicBaseUrl;
    }
    return computeBaseUrl(req);
  }

  /**
   * Base URL du frontend pour le QR code : dynamique selon la requête en cours. 1) Origin (ex:
   * http://192.168.22.30:5173) si présent 2) Base extraite du Referer (ex:
   * http://192.168.22.30:5173/pec/123 → http://192.168.22.30:5173) 3) app.front.base-url en config
   * (repli statique) 4) Base du backend (dernier repli)
   */
  private String resolveFrontBaseUrlFromRequest(HttpServletRequest req) {
    String origin = req.getHeader("Origin");
    if (origin != null && !origin.isBlank()) {
      String base = origin.trim().replaceAll("/+$", "");
      if (isValidHttpBase(base)) {
        log.debug("Front base from Origin: {}", base);
        return base;
      }
    }
    String referer = req.getHeader("Referer");
    if (referer != null && !referer.isBlank()) {
      String base = baseUrlFromReferer(referer.trim());
      if (base != null) {
        log.debug("Front base from Referer: {}", base);
        return base;
      }
    }
    if (frontBaseUrl != null && !frontBaseUrl.isBlank()) {
      return frontBaseUrl.endsWith("/") ? frontBaseUrl.substring(0, frontBaseUrl.length() - 1)
          : frontBaseUrl;
    }
    return resolveBaseUrl(req);
  }

  private static boolean isValidHttpBase(String s) {
    return s.startsWith("http://") || s.startsWith("https://");
  }

  private static String baseUrlFromReferer(String referer) {
    try {
      URI uri = URI.create(referer);
      String scheme = uri.getScheme();
      if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")))
        return null;
      String host = uri.getHost();
      if (host == null || host.isBlank())
        return null;
      int port = uri.getPort();
      boolean omitPort = (port == -1) || ("http".equalsIgnoreCase(scheme) && port == 80)
          || ("https".equalsIgnoreCase(scheme) && port == 443);
      String portPart = omitPort ? "" : ":" + port;
      return scheme + "://" + host + portPart;
    } catch (Exception e) {
      return null;
    }
  }

  /** Utilisé par la redirection /api/pec/public/{token}/page (repli config). */
  private String resolveFrontBaseUrl(HttpServletRequest req) {
    return resolveFrontBaseUrlFromRequest(req);
  }

  private static String computeBaseUrl(HttpServletRequest req) {
    String forwardedProto = req.getHeader("X-Forwarded-Proto");
    String forwardedHost = req.getHeader("X-Forwarded-Host");
    String forwardedPort = req.getHeader("X-Forwarded-Port");

    String scheme =
        (forwardedProto != null && !forwardedProto.isBlank()) ? forwardedProto : req.getScheme();
    String host =
        (forwardedHost != null && !forwardedHost.isBlank()) ? forwardedHost : req.getServerName();

    String portPart;
    if (forwardedPort != null && !forwardedPort.isBlank()) {
      portPart = (("http".equalsIgnoreCase(scheme) && "80".equals(forwardedPort))
          || ("https".equalsIgnoreCase(scheme) && "443".equals(forwardedPort))) ? ""
              : ":" + forwardedPort;
    } else {
      int port = req.getServerPort();
      portPart = ((port == 80 && "http".equalsIgnoreCase(scheme))
          || (port == 443 && "https".equalsIgnoreCase(scheme))) ? "" : ":" + port;
    }

    return scheme + "://" + host + portPart;
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }

  /** Charge le logo en byte[] pour le rapport Jasper (plusieurs chemins essayés). */
  private static byte[] loadLogoBytes() {
    String[] paths = {"static/logo192.png", "static/senat-logo.png", "static/logo.png", "logo.png"};
    for (String path : paths) {
      try {
        var res = new ClassPathResource(path);
        if (res.exists()) {
          try (InputStream in = res.getInputStream()) {
            return in.readAllBytes();
          }
        }
      } catch (Exception e) {
        // ignore, try next path
      }
    }
    return null;
  }

  /** Récupère les octets d'une image depuis une URL (ex. frontend). Retourne null en cas d'erreur. */
  private byte[] fetchImageBytesFromUrl(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) return null;
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(imageUrl.trim()))
          .timeout(Duration.ofSeconds(15))
          .GET()
          .build();
      HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        byte[] body = response.body();
        return (body != null && body.length > 0) ? body : null;
      }
    } catch (Exception e) {
      log.warn("Impossible de charger l'image de fond depuis l'URL: {} — {}", imageUrl, e.getMessage());
    }
    return null;
  }

  /** Charge une image de fond depuis le classpath. Essaie plusieurs chemins. Retourne null si absent. */
  private byte[] loadBackgroundImageBytes(String preferredPath) {
    String[] paths = {preferredPath, "reports/fond_portrait_a4.png", "reports/fond_paysage_a4.png",
        "static/fond_portrait_a4.png", "static/fond_paysage_a4.png", "fond_portrait_a4.png", "fond_paysage_a4.png"};
    for (String path : paths) {
      try {
        var res = new ClassPathResource(path);
        if (res.exists()) {
          try (InputStream in = res.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            if (bytes != null && bytes.length > 0) {
              log.debug("Image de fond chargée: {} ({} octets)", path, bytes.length);
              return bytes;
            }
          }
        }
      } catch (Exception e) {
        // essai chemin suivant
      }
    }
    log.warn("Aucune image de fond trouvée (vérifiez src/main/resources/reports/fond_portrait_a4.png)");
    return null;
  }

  private static String normalizeFilename(String raw) {
    if (raw == null || raw.isBlank())
      return null;
    String s = raw.trim().replace("\\", "/");
    if (s.regionMatches(true, 0, "file://", 0, "file://".length())) {
      while (s.startsWith("file:/")) {
        s = s.substring("file:/".length());
      }
      int p = s.lastIndexOf('/');
      return (p >= 0) ? s.substring(p + 1) : s;
    }
    int p = s.lastIndexOf('/');
    return (p >= 0) ? s.substring(p + 1) : s;
  }

  private void sendError(HttpServletResponse resp, int status, String message) {
    try {
      resp.setStatus(status);
      resp.setContentType("text/plain; charset=UTF-8");
      resp.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
      resp.flushBuffer();
    } catch (Exception ignore) {
    }
  }
}
