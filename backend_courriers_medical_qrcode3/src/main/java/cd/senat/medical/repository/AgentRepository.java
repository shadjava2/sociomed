package cd.senat.medical.repository;

import cd.senat.medical.entity.Agent;
import cd.senat.medical.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    /* ======================= READ avec relations (DÉTAIL) ======================= */

    /** Détail : charge un agent + conjoint + enfants (1 seul hit) */
    @EntityGraph(attributePaths = {"conjoint", "enfants"})
    @Query("select a from Agent a where a.id = :id")
    Optional<Agent> findByIdWithRelations(@Param("id") Long id);

    // ⚠️ Liste avec relations = lourd. À utiliser seulement si tu en as besoin explicitement.
    // @EntityGraph(attributePaths = {"conjoint", "enfants"})
    // @Query("select a from Agent a")
    // Page<Agent> findPageWithRelations(Pageable pageable);


    /* ======================= Recherche simple ======================= */

    @Query("""
        select a from Agent a
        where (:q is null or :q = '' or
               lower(a.nom)       like lower(concat('%', :q, '%')) or
               lower(a.postnom)   like lower(concat('%', :q, '%')) or
               lower(a.prenom)    like lower(concat('%', :q, '%')) or
               lower(a.direction) like lower(concat('%', :q, '%')) or
               lower(a.email)     like lower(concat('%', :q, '%')) or
               a.telephone        like concat('%', :q, '%'))
        """)
    Page<Agent> searchByNomComplet(@Param("q") String q, Pageable pageable);

    Page<Agent> findByGenre(Genre genre, Pageable pageable);
    Page<Agent> findByDirectionIgnoreCase(String direction, Pageable pageable);
    Page<Agent> findByEtatIgnoreCase(String etat, Pageable pageable);


    /* ======================= Recherche combinée (q + filtres) ======================= */

    /**
     * Recherche paginée multi-critères :
     * - q : match sur nom/postnom/prenom/direction/email/téléphone
     * - genre : M/F si fourni
     * - etat : ex. ACTIF/INACTIF si fourni (case-insensitive)
     * - direction : filtrage exact (case-insensitive)
     * - categorie : filtrage exact (Personnel d'appoint, Agent Administratif, Cadre Administratif)
     */
    @Query("""
        select a from Agent a
        where
          (
            :q is null or :q = '' or
            lower(a.nom)       like lower(concat('%', :q, '%')) or
            lower(a.postnom)   like lower(concat('%', :q, '%')) or
            lower(a.prenom)    like lower(concat('%', :q, '%')) or
            lower(a.direction) like lower(concat('%', :q, '%')) or
            lower(a.email)     like lower(concat('%', :q, '%')) or
            a.telephone        like concat('%', :q, '%')
          )
          and (:genre is null or a.genre = :genre)
          and (:etat is null or :etat = '' or lower(coalesce(a.etat, '')) = lower(:etat))
          and (:direction is null or :direction = '' or lower(coalesce(a.direction, '')) = lower(:direction))
          and (:categorie is null or :categorie = '' or coalesce(a.categorie, '') = :categorie)
        """)
    Page<Agent> searchCombined(
        @Param("q") String q,
        @Param("genre") Genre genre,
        @Param("etat") String etat,
        @Param("direction") String direction,
        @Param("categorie") String categorie,
        Pageable pageable
    );


    /* ======================= Unicité / Existence / Compteurs ======================= */

    boolean existsByTelephone(String telephone);
    boolean existsByEmail(String email);

    long countByGenre(Genre genre);
    long countByEtatIgnoreCase(String etat);

    /** Trouver l’agent par id du conjoint */
    @Query("select a from Agent a where a.conjoint.id = :conjointId")
    Optional<Agent> findByConjointId(@Param("conjointId") Long conjointId);

    /** Unicité d'identité (nom+postnom+prenom+datenaiss+lnaiss) */
    boolean existsByNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaissAndLnaiss(
        String nom, String postnom, String prenom, Date datenaiss, String lnaiss
    );

    /** Même unicité en excluant un id (pour mise à jour : éviter de considérer l’agent courant comme doublon). */
    boolean existsByNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaissAndLnaissAndIdNot(
        String nom, String postnom, String prenom, Date datenaiss, String lnaiss, Long id
    );
}
