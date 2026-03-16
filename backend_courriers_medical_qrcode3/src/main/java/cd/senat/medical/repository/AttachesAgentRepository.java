package cd.senat.medical.repository;

import cd.senat.medical.entity.AttachesAgent;
import cd.senat.medical.entity.CategorieEnfant;
import cd.senat.medical.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AttachesAgentRepository extends JpaRepository<AttachesAgent, Long> {

    /** Charger avec parent (agent/senateur) */
    @EntityGraph(attributePaths = {"agent", "senateur"})
    @Query("select e from AttachesAgent e where e.id = :id")
    AttachesAgent findByIdWithParent(@Param("id") Long id);

    /** Lister enfants d’un agent ou d’un sénateur */
    List<AttachesAgent> findByAgent_Id(Long agentId);
    List<AttachesAgent> findBySenateur_Id(Long senateurId);

    /** Recherches / filtres */
    Page<AttachesAgent> findByCategorie(CategorieEnfant categorie, Pageable pageable);
    Page<AttachesAgent> findByGenre(Genre genre, Pageable pageable);
    Page<AttachesAgent> findByDatenaissBetween(Date from, Date to, Pageable pageable);

    /** Recherche par nom/postnom/prenom (insensible à la casse) */
    @Query("""
        select e from AttachesAgent e
        where (:q is null or :q = '' or
               lower(e.nomEnfant)     like lower(concat('%', :q, '%')) or
               lower(e.postnomEnfant) like lower(concat('%', :q, '%')) or
               lower(e.prenomEnfant)  like lower(concat('%', :q, '%')))
        """)
    Page<AttachesAgent> searchByNomComplet(@Param("q") String q, Pageable pageable);

    /** Compteurs utiles */
    long countByAgent_Id(Long agentId);
    long countBySenateur_Id(Long senateurId);
    long countByAgent_IdAndGenre(Long agentId, Genre genre);
    long countBySenateur_IdAndGenre(Long senateurId, Genre genre);

    /** Existence (identité + parent), insensible à la casse */
    boolean existsByAgent_IdAndNomEnfantIgnoreCaseAndPostnomEnfantIgnoreCaseAndPrenomEnfantIgnoreCaseAndDatenaiss(
        Long agentId, String nomEnfant, String postnomEnfant, String prenomEnfant, Date datenaiss
    );

    boolean existsBySenateur_IdAndNomEnfantIgnoreCaseAndPostnomEnfantIgnoreCaseAndPrenomEnfantIgnoreCaseAndDatenaiss(
        Long senateurId, String nomEnfant, String postnomEnfant, String prenomEnfant, Date datenaiss
    );
}
