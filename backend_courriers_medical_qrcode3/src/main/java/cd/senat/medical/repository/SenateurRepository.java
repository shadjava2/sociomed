package cd.senat.medical.repository;

import cd.senat.medical.entity.Genre;
import cd.senat.medical.entity.Senateur;
import cd.senat.medical.entity.StatutSenateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface SenateurRepository extends JpaRepository<Senateur, Long> {

    /** Charger sénateur + conjoint + enfants */
    @EntityGraph(attributePaths = {"conjoint", "enfants"})
    @Query("select s from Senateur s where s.id = :id")
    Optional<Senateur> findByIdWithRelations(@Param("id") Long id);

    /** Filtres */
    Page<Senateur> findByStatut(StatutSenateur statut, Pageable pageable);
    Page<Senateur> findByGenre(Genre genre, Pageable pageable);
    Page<Senateur> findByLegislatureIgnoreCase(String legislature, Pageable pageable);
    
    /** Recherche paginée par nom/postnom/prenom */
    @Query("""
        select s from Senateur s
        where (:q is null or :q = '' or
               lower(s.nom) like lower(concat('%', :q, '%')) or
               lower(s.postnom) like lower(concat('%', :q, '%')) or
               lower(s.prenom) like lower(concat('%', :q, '%')))
        """)
    Page<Senateur> searchByNomComplet(@Param("q") String q, Pageable pageable);

    /** Compteurs */
    long countByStatut(StatutSenateur statut);
    long countByGenre(Genre genre);
    @Query("""
    	    select (count(s) > 0) from Senateur s
    	    where lower(s.nom) = lower(:nom)
    	      and lower(coalesce(s.postnom, '')) = lower(coalesce(:postnom, ''))
    	      and lower(coalesce(s.prenom,  '')) = lower(coalesce(:prenom,  ''))
    	      and s.datenaiss = :datenaiss
    	      and s.id <> :id
    	    """)
    	boolean existsByIdentiteAndIdNot(@Param("nom") String nom,
    	                                 @Param("postnom") String postnom,
    	                                 @Param("prenom") String prenom,
    	                                 @Param("datenaiss") Date datenaiss,
    	                                 @Param("id") Long id);

    /** Trouver le sénateur par id du conjoint */
    @Query("select s from Senateur s where s.conjoint.id = :conjointId")
    Optional<Senateur> findByConjointId(@Param("conjointId") Long conjointId);
    
    boolean existsByNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaiss(
    	    String nom, String postnom, String prenom, Date datenaiss);

}
