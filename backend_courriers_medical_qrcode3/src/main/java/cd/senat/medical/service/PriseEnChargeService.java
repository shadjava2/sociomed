package cd.senat.medical.service;

import cd.senat.medical.dto.LabelCountDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.dto.PriseEnChargeDTO;
import cd.senat.medical.dto.PriseEnChargeDTO.CreateRequest;
import cd.senat.medical.entity.*;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class PriseEnChargeService {

    private final PriseEnChargeRepository repo;
    private final AgentRepository agentRepo;
    private final SenateurRepository senateurRepo;
    private final ConjointRepository conjointRepo;
    private final AttachesAgentRepository enfantRepo;
    private final HopitalRepository hopitalRepo;
    private final UserService userService;

    public PriseEnChargeService(PriseEnChargeRepository repo,
                                AgentRepository agentRepo,
                                SenateurRepository senateurRepo,
                                ConjointRepository conjointRepo,
                                AttachesAgentRepository enfantRepo,
                                HopitalRepository hopitalRepo,
                                UserService userService) {
        this.repo = repo;
        this.agentRepo = agentRepo;
        this.senateurRepo = senateurRepo;
        this.conjointRepo = conjointRepo;
        this.enfantRepo = enfantRepo;
        this.hopitalRepo = hopitalRepo;
        this.userService = userService;
    }

    /* ===================== CREATE ===================== */
    public PriseEnChargeDTO.Detail create(PriseEnChargeDTO.CreateRequest req) {
        if (req == null) throw new BusinessException("Requête vide");
        if (req.hopitalId() == null) throw new BusinessException("Hôpital obligatoire");

        var holder = resolveBeneficiaire(req);

        var hopital = hopitalRepo.findById(req.hopitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hôpital introuvable: " + req.hopitalId()));

        var p = new PriseEnCharge();
        p.setType(holder.type);
        p.setNumero(generateNumero());

        p.setNom(holder.nom);
        p.setPostnom(holder.postnom);
        p.setPrenom(holder.prenom);
        p.setGenre(holder.genre);

        p.setAgent(holder.agent);
        p.setSenateur(holder.senateur);
        p.setConjoint(holder.conjoint);
        p.setEnfant(holder.enfant);
        p.setHopital(hopital);

        p.setQualiteMalade(notBlankOrElse(req.qualiteMalade(), defaultQualite(holder)));
        p.setAdresseMalade(trim(req.adresseMalade()));
        p.setEtablissement(trim(
                (req.etablissement() == null || req.etablissement().isBlank())
                        ? hopital.getNom()
                        : req.etablissement()
        ));
        p.setMotif(trim(req.motif()));
        p.setActes(trim(req.actes()));
        p.setRemarque(trim(req.remarque()));

        final String issuer = Optional.ofNullable(currentUsername()).filter(s -> !s.isBlank()).orElse("system");
        String fullname = "SYSTEM";
        try {
            fullname = Optional.ofNullable(userService.getFullnameByUsername(issuer))
                    .filter(s -> !s.isBlank())
                    .orElse("SYSTEM");
        } catch (Exception ignored) { }
        p.setCreatedBy(issuer);
        p.setCreatedByFullname(fullname);
        // Double garantie avant persist (au cas où @PrePersist ne s'exécute pas dans un contexte particulier)
        if (p.getCreatedBy() == null || p.getCreatedBy().isBlank()) p.setCreatedBy("system");
        if (p.getCreatedByFullname() == null || p.getCreatedByFullname().isBlank()) p.setCreatedByFullname("SYSTEM");

        var saved = repo.save(p);
        return PriseEnChargeDTO.toDetail(saved);
    }

    /* ===================== UPDATE ===================== */
    public PriseEnChargeDTO.Detail update(Long id, PriseEnChargeDTO.UpdateRequest req) {
        var p = getEntity(id);
        if (req == null) return PriseEnChargeDTO.toDetail(p);

        applyIfNotBlank(req.qualiteMalade(), p::setQualiteMalade);
        applyIfNotBlank(req.adresseMalade(), p::setAdresseMalade);
        applyIfNotBlank(req.etablissement(), p::setEtablissement);
        applyIfNotBlank(req.motif(), p::setMotif);
        applyIfNotBlank(req.actes(), p::setActes);
        applyIfNotBlank(req.remarque(), p::setRemarque);

        return PriseEnChargeDTO.toDetail(p);
    }

    /* ===================== READ / DELETE ===================== */
    public PriseEnChargeDTO.Detail getById(Long id) {
        return PriseEnChargeDTO.toDetail(getEntity(id));
    }

    /** Charge une PEC avec bénéficiaire (agent/sénateur/conjoint/enfant) pour avoir la photo. */
    public PriseEnChargeDTO.Detail getByIdWithRelations(Long id) {
        return repo.findByIdWithRelations(id)
                .map(PriseEnChargeDTO::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("PEC introuvable: " + id));
    }

    public void delete(Long id) {
        repo.delete(getEntity(id));
    }

    /* ===================== LIST ===================== */
    public PageResponse<PriseEnChargeDTO.Item> list(Pageable pageable) {
        Page<PriseEnChargeDTO.Item> page = repo.findAll(pageable)
                .map(PriseEnChargeDTO::toItem);
        return PageResponse.from(page);
    }

    /* ===================== LIST PAR HOPITAL ===================== */
    public PageResponse<PriseEnChargeDTO.ListByHopitalItem> listByHopital(
            PriseEnChargeDTO.ListFilter filter,
            Pageable pageable
    ) {
        final Long hopId = filter != null ? filter.hopitalId() : null;
        Date from = filter != null ? filter.from() : null;
        Date to = filter != null ? filter.to() : null;

        Page<PriseEnCharge> page;

        if (from == null && to == null) {
            if (hopId != null) {
                page = repo.findByHopital_Id(hopId, pageable);
            } else {
                page = repo.findAll(pageable);
            }
        } else {
            if (from == null) from = new Date(0L);
            if (to == null) to = new Date(4102444800000L);
            if (hopId != null) {
                page = repo.findByHopitalInPeriod(hopId, from, to, pageable);
            } else {
                page = repo.findAllInPeriod(from, to, pageable);
            }
        }

        var mapped = page.map(this::toListByHopitalItem);
        return PageResponse.from(mapped);
    }

    public List<PriseEnChargeDTO.ListByHopitalItem> listByHopitalForJasper(
            PriseEnChargeDTO.ListFilter filter,
            int maxRows
    ) {
        int size = Math.min(Math.max(1, maxRows), 50_000);

        final Long hopId = filter != null ? filter.hopitalId() : null;
        Date from = filter != null ? filter.from() : null;
        Date to = filter != null ? filter.to() : null;

        Page<PriseEnCharge> page;

        if (from == null && to == null) {
            if (hopId != null) {
                page = repo.findByHopital_Id(hopId, PageRequest.of(0, size));
            } else {
                page = repo.findAll(PageRequest.of(0, size));
            }
        } else {
            if (from == null) from = new Date(0L);
            if (to == null) to = new Date(4102444800000L);
            if (hopId != null) {
                page = repo.findByHopitalInPeriod(hopId, from, to, PageRequest.of(0, size));
            } else {
                page = repo.findAllInPeriod(from, to, PageRequest.of(0, size));
            }
        }

        return page.map(this::toListByHopitalItem).getContent();
    }

    /**
     * Utilisé par le controller d'impression PDF mensuelle.
     * month attendu au format YYYY-MM.
     */
    public List<PriseEnChargeDTO.ListByHopitalItem> listForListing(Long hopitalId, String month, int limit) {
        Date from = null;
        Date to = null;

        if (month != null && !month.isBlank()) {
            LocalDate startMonth = LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime start = startMonth.atStartOfDay();
            LocalDateTime end = startMonth.plusMonths(1).atStartOfDay();

            from = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
            to = Date.from(end.atZone(ZoneId.systemDefault()).toInstant());
        }

        return listByHopitalForJasper(
                new PriseEnChargeDTO.ListFilter(hopitalId, from, to),
                limit
        );
    }

    public PriseEnChargeDTO.HopitalMini getHopitalMini(Long hopitalId) {
        if (hopitalId == null) return null;

        return hopitalRepo.findById(hopitalId)
                .map(h -> new PriseEnChargeDTO.HopitalMini(h.getId(), h.getNom()))
                .orElse(null);
    }

    /* ===================== STATS ===================== */
    public List<LabelCountDTO> statsPecParHopital(LocalDate yearMonth) {
        LocalDateTime start = yearMonth.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = yearMonth.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        List<PriseEnChargeRepository.LabelCount> rows = repo.pecParHopitalMois(start, end);
        return rows.stream()
                .map(rc -> new LabelCountDTO(rc.getLabel(), rc.getValue()))
                .collect(Collectors.toList());
    }

    public List<LabelCountDTO> statsPecParCategorie(LocalDate yearMonth) {
        LocalDateTime start = yearMonth.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = yearMonth.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        List<PriseEnChargeRepository.LabelCount> rows = repo.pecParCategorieMois(start, end);
        return rows.stream()
                .map(rc -> new LabelCountDTO(rc.getLabel(), rc.getValue()))
                .collect(Collectors.toList());
    }

    /* ===================== HELPERS PRIVÉS ===================== */
    private PriseEnCharge getEntity(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PEC introuvable: " + id));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String notBlankOrElse(String v, String def) {
        return (v != null && !v.isBlank()) ? v.trim() : def;
    }

    private static void applyIfNotBlank(String v, java.util.function.Consumer<String> set) {
        if (v != null && !v.isBlank()) set.accept(v.trim());
    }

    private String generateNumero() {
        var now = LocalDate.now();
        var prefix = "PEC-" + now.format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        var rnd = new Random();

        for (int i = 0; i < 5; i++) {
            var tail = String.format("%06d", rnd.nextInt(1_000_000));
            var num = prefix + tail;
            if (repo.findByNumero(num).isEmpty()) return num;
        }
        return prefix + System.currentTimeMillis();
    }

    private record Holder(
            BeneficiaireType type,
            Agent agent,
            Senateur senateur,
            Conjoint conjoint,
            AttachesAgent enfant,
            String nom,
            String postnom,
            String prenom,
            Genre genre
    ) {}

    private String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        String name = (a != null) ? a.getName() : null;
        return (name != null && !name.isBlank()) ? name : "system";
    }

    private Holder resolveBeneficiaire(CreateRequest req) {
        int count = 0;
        if (req.agentId() != null) count++;
        if (req.senateurId() != null) count++;
        if (req.conjointId() != null) count++;
        if (req.enfantId() != null) count++;

        if (count != 1) {
            throw new BusinessException("Spécifie exactement un bénéficiaire (agentId | senateurId | conjointId | enfantId).");
        }

        if (req.agentId() != null) {
            var a = agentRepo.findById(req.agentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable: " + req.agentId()));
            return new Holder(
                    BeneficiaireType.AGENT, a, null, null, null,
                    a.getNom(), a.getPostnom(), a.getPrenom(), a.getGenre()
            );
        }

        if (req.senateurId() != null) {
            var s = senateurRepo.findById(req.senateurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sénateur introuvable: " + req.senateurId()));
            return new Holder(
                    BeneficiaireType.SENATEUR, null, s, null, null,
                    s.getNom(), s.getPostnom(), s.getPrenom(), s.getGenre()
            );
        }

        if (req.conjointId() != null) {
            var c = conjointRepo.findById(req.conjointId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conjoint introuvable: " + req.conjointId()));
            return new Holder(
                    BeneficiaireType.CONJOINT, null, null, c, null,
                    c.getNom(), c.getPostnom(), c.getPrenom(), c.getGenre()
            );
        }

        var e = enfantRepo.findById(req.enfantId())
                .orElseThrow(() -> new ResourceNotFoundException("Enfant introuvable: " + req.enfantId()));

        return new Holder(
                BeneficiaireType.ENFANT, null, null, null, e,
                e.getNomEnfant(), e.getPostnomEnfant(), e.getPrenomEnfant(), e.getGenre()
        );
    }

    private String defaultQualite(Holder h) {
        return switch (h.type) {
            case AGENT -> "Agent";
            case SENATEUR -> "Sénateur";
            case CONJOINT -> "Conjoint(e)";
            case ENFANT -> "Enfant";
        };
    }

    private PriseEnChargeDTO.ListByHopitalItem toListByHopitalItem(PriseEnCharge p) {
        Long hopitalId = (p.getHopital() != null) ? p.getHopital().getId() : null;
        String hopitalNom = (p.getHopital() != null) ? p.getHopital().getNom() : null;

        String beneficiaireNom = concatNom(p.getNom(), p.getPostnom(), p.getPrenom());
        String beneficiaireQualite = p.getQualiteMalade();
        String etablissement = p.getEtablissement();

        java.util.Date dateExpiration = p.getDateExpiration();
        String statut = statutFromExpiration(dateExpiration);

        BigDecimal montant = null;
        try {
            var m = p.getClass().getMethod("getMontant");
            Object val = m.invoke(p);
            if (val instanceof BigDecimal bd) montant = bd;
        } catch (Exception ignore) {
        }

        return new PriseEnChargeDTO.ListByHopitalItem(
                p.getId(),
                p.getNumero(),
                p.getDateEmission(),
                dateExpiration,
                statut,
                hopitalId,
                hopitalNom,
                beneficiaireNom,
                beneficiaireQualite,
                etablissement,
                montant
        );
    }

    /** Calcule le statut Valide / Expiré à partir de la date d'expiration. */
    private static String statutFromExpiration(java.util.Date dateExpiration) {
        if (dateExpiration == null) return "—";
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Date today = cal.getTime();
        return !dateExpiration.before(today) ? "Valide" : "Expiré";
    }

    private static String concatNom(String nom, String postnom, String prenom) {
        StringBuilder sb = new StringBuilder();
        if (nom != null && !nom.isBlank()) sb.append(nom.trim());
        if (postnom != null && !postnom.isBlank()) sb.append(sb.length() > 0 ? " " : "").append(postnom.trim());
        if (prenom != null && !prenom.isBlank()) sb.append(sb.length() > 0 ? " " : "").append(prenom.trim());
        String s = sb.toString().trim();
        return s.isEmpty() ? null : s;
    }
}