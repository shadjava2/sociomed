package cd.senat.medical.repository;

import cd.senat.medical.entity.CategorieHopital;
import cd.senat.medical.entity.Genre;
import cd.senat.medical.entity.Hopital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface HopitalRepository extends JpaRepository<Hopital, Long> {

    // --------- Trouver / vérifier -------------
    Optional<Hopital> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    Page<Hopital> findByActifTrue(Pageable pageable);
    Page<Hopital> findByCategorie(CategorieHopital categorie, Pageable pageable);

    long countByActifTrue();
    long countByCategorie(CategorieHopital categorie);

    List<Hopital> findTop3ByActifTrueOrderByNomAsc();

    // Recherche insensible à la casse sur nom/ville (avec filtres simples)
    @Query("""
        select h from Hopital h
        where (:q is null or :q = '' or
               lower(h.nom) like lower(concat('%', :q, '%')) or
               lower(h.ville) like lower(concat('%', :q, '%')))
          and (:actif is null or h.actif = :actif)
          and (:categorie is null or h.categorie = :categorie)
        """)
    Page<Hopital> search(@Param("q") String q,
                         @Param("actif") Boolean actif,
                         @Param("categorie") CategorieHopital categorie,
                         Pageable pageable);

    // --------- Projections Stats -------------
    /** Projection pour stats "nombre de PEC par hopital et par sexe" */
    interface StatsGenreParHopital {
        Long getHopitalId();
        String getHopitalNom();
        Genre getGenre();
        long getTotal();
    }

    // --------- Stats : groupement par sexe (tous hôpitaux) -------------
    @Query("""
        select h.id   as hopitalId,
               h.nom  as hopitalNom,
               p.genre as genre,
               count(p.id) as total
        from PriseEnCharge p
        join p.hopital h
        where (:from is null or p.dateEmission >= :from)
          and (:to   is null or p.dateEmission <  :to)
        group by h.id, h.nom, p.genre
        order by h.nom asc
        """)
    List<StatsGenreParHopital> statsParHopitalEtGenre(@Param("from") Date from,
                                                      @Param("to") Date to);

    // --------- Stats : groupement par sexe pour un hôpital précis -------------
    @Query("""
        select h.id   as hopitalId,
               h.nom  as hopitalNom,
               p.genre as genre,
               count(p.id) as total
        from PriseEnCharge p
        join p.hopital h
        where h.id = :hopitalId
          and (:from is null or p.dateEmission >= :from)
          and (:to   is null or p.dateEmission <  :to)
        group by h.id, h.nom, p.genre
        order by h.nom asc
        """)
    List<StatsGenreParHopital> statsParGenrePourHopital(@Param("hopitalId") Long hopitalId,
                                                        @Param("from") Date from,
                                                        @Param("to") Date to);
}
