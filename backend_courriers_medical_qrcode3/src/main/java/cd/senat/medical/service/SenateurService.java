package cd.senat.medical.service;

import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.dto.SenateurDTO;
import cd.senat.medical.entity.Genre;
import cd.senat.medical.entity.Senateur;
import cd.senat.medical.entity.StatutSenateur;
import cd.senat.medical.exception.BusinessException;
import cd.senat.medical.exception.ResourceNotFoundException;
import cd.senat.medical.repository.SenateurRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@Transactional
public class SenateurService {

    private final SenateurRepository repo;

    public SenateurService(SenateurRepository repo) {
        this.repo = repo;
    }

    /* ======================= LISTE PAGINÉE (Résumé) ======================= */
    public PageResponse<SenateurDTO.Summary> getAll(String q, StatutSenateur statut, Genre genre, String legislature, Pageable pageable) {
        // TODO: appliquer réellement les filtres statut/genre/legislature dans le repo si besoin
        Page<Senateur> page = repo.searchByNomComplet(q, pageable);
        return PageResponse.from(page).map(SenateurDTO::toSummary);
    }

    /* ======================= CREATE ======================= */
    public SenateurDTO.Detail create(SenateurDTO.CreateRequest req) {
        // normalisation des champs identité
        String nom       = norm(req.nom());
        String postnom   = norm(req.postnom());
        String prenom    = norm(req.prenom());
        Date   datenaiss = req.datenaiss();

        if (existsByIdentite(nom, postnom, prenom, datenaiss)) {
            throw new BusinessException("Doublon SENATEUR (identité déjà enregistrée)");
        }

        Senateur s = new Senateur();
        // on applique en prenant les valeurs normalisées
        s.setNom(nom);
        s.setPostnom(postnom);
        s.setPrenom(prenom);
        s.setDatenaiss(datenaiss);
        s.setGenre(req.genre());
        s.setStatut(req.statut());
        s.setTelephone(norm(req.telephone()));
        s.setLegislature(norm(req.legislature()));
        s.setEmail(norm(req.email()));
        s.setAdresse(norm(req.adresse()));
        s.setPhoto(norm(req.photo()));

        try {
            Senateur saved = repo.save(s);
            return SenateurDTO.toDetail(getByIdEntity(saved.getId()));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Contrainte d’unicité violée pour SENATEUR");
        }
    }

    /* ======================= UPDATE (partiel) ======================= */
    public SenateurDTO.Detail update(Long id, SenateurDTO.UpdateRequest req) {
        Senateur current = getByIdEntity(id);

        // 1) Normalise le payload ("" -> null, trim)
        String pNom       = norm(req.nom());
        String pPostnom   = norm(req.postnom());
        String pPrenom    = norm(req.prenom());
        Date   pDatenaiss = req.datenaiss();

        // 2) Reconstruit l'identité effective (payload si fourni, sinon valeur actuelle)
        String effNom       = coalesce(pNom, current.getNom());
        String effPostnom   = coalesce(pPostnom, current.getPostnom());
        String effPrenom    = coalesce(pPrenom, current.getPrenom());
        Date   effDatenaiss = coalesce(pDatenaiss, current.getDatenaiss());

        // 3) Détecte un vrai changement d'identité (après normalisation)
        boolean identityWouldChange =
                notEq(current.getNom(),       effNom) ||
                notEq(current.getPostnom(),   effPostnom) ||
                notEq(current.getPrenom(),    effPrenom) ||
                notEq(current.getDatenaiss(), effDatenaiss);

        // 4) Si l'identité changerait, vérifier l’unicité en excluant l’ID courant
        if (identityWouldChange && existsByIdentiteAndIdNot(effNom, effPostnom, effPrenom, effDatenaiss, id)) {
            throw new BusinessException("Conflit identité SENATEUR (déjà existant)");
        }

        // 5) Applique uniquement les champs non-nuls (update partiel)
        applyIfNotNull(pNom,        current::setNom);
        applyIfNotNull(pPostnom,    current::setPostnom);
        applyIfNotNull(pPrenom,     current::setPrenom);
        applyIfNotNull(req.genre(), current::setGenre);
        applyIfNotNull(pDatenaiss,  current::setDatenaiss);

        applyIfNotNull(norm(req.telephone()),   current::setTelephone);
        applyIfNotNull(norm(req.legislature()), current::setLegislature);
        applyIfNotNull(norm(req.email()),       current::setEmail);
        applyIfNotNull(norm(req.adresse()),     current::setAdresse);
        applyIfNotNull(norm(req.photo()),       current::setPhoto);

        applyIfNotNull(req.statut(), current::setStatut);

        // Force le flush pour lever tôt les erreurs éventuelles
        repo.save(current);

        return SenateurDTO.toDetail(current);
    }

    /* ======================= READ / DELETE ======================= */
    public SenateurDTO.Detail getById(Long id) {
        return SenateurDTO.toDetail(getByIdEntity(id));
    }

    public void delete(Long id) {
        repo.delete(getByIdEntity(id));
    }

    /* ======================= Helpers ======================= */
    private Senateur getByIdEntity(Long id) {
        return repo.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sénateur introuvable: " + id));
    }

    // Unicité stricte (create)
    private boolean existsByIdentite(String nom, String postnom, String prenom, Date datenaiss) {
        return repo.existsByNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaiss(
                nom, postnom, prenom, datenaiss
        );
    }

    // Unicité en excluant l’ID courant (update)
    private boolean existsByIdentiteAndIdNot(String nom, String postnom, String prenom, Date datenaiss, Long id) {
        // ⚠️ assure-toi d’avoir cette méthode côté repository, par ex. :
        // @Query("""
        //   select (count(s) > 0) from Senateur s
        //   where lower(s.nom) = lower(:nom)
        //     and coalesce(lower(s.postnom),'') = coalesce(lower(:postnom),'')
        //     and coalesce(lower(s.prenom),'')  = coalesce(lower(:prenom),'')
        //     and s.datenaiss = :datenaiss
        //     and s.id <> :id
        // """)
        // boolean existsByIdentiteAndIdNot(String nom, String postnom, String prenom, Date datenaiss, Long id);
        return repo.existsByIdentiteAndIdNot(nom, postnom, prenom, datenaiss, id);
    }

    /* -------- utilitaires -------- */
    private static boolean notEq(Object a, Object b) {
        return !Objects.equals(a, b);
    }
    private static <T> T coalesce(T candidate, T fallback) {
        return (candidate != null) ? candidate : fallback;
    }
    private static <T> void applyIfNotNull(T value, Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }
    /** trim + convertit "" en null */
    private static String norm(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
