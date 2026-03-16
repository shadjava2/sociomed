package cd.senat.medical.repository;

import cd.senat.medical.entity.BeneficiaireType;
import cd.senat.medical.entity.PriseEnCharge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriseEnChargeRepository extends JpaRepository<PriseEnCharge, Long> {

    Optional<PriseEnCharge> findByNumero(String numero);

    @EntityGraph(attributePaths = { "agent", "senateur", "conjoint", "enfant", "hopital" })
    @Query("select p from PriseEnCharge p where p.id = :id")
    Optional<PriseEnCharge> findByIdWithRelations(@Param("id") Long id);

    /* ========= Recherches simples ========= */

    @EntityGraph(attributePaths = { "hopital" })
    Page<PriseEnCharge> findByType(BeneficiaireType type, Pageable pageable);

    @EntityGraph(attributePaths = { "hopital" })
    @Query("""
           select p from PriseEnCharge p
           where p.hopital.id = :hopitalId
           order by p.dateEmission desc
           """)
    Page<PriseEnCharge> findByHopital_Id(@Param("hopitalId") Long hopitalId, Pageable pageable);

    /* ========= Filtrage par période (UTILISER dateEmission) ========= */

    @EntityGraph(attributePaths = { "hopital" })
    @Query("""
           select p from PriseEnCharge p
           where p.dateEmission between :from and :to
           order by p.dateEmission desc
           """)
    Page<PriseEnCharge> findAllInPeriod(@Param("from") Date from,
                                        @Param("to") Date to,
                                        Pageable pageable);

    @EntityGraph(attributePaths = { "hopital" })
    @Query("""
           select p from PriseEnCharge p
           where p.hopital.id = :hopitalId
             and p.dateEmission between :from and :to
           order by p.dateEmission desc
           """)
    Page<PriseEnCharge> findByHopitalInPeriod(@Param("hopitalId") Long hopitalId,
                                              @Param("from") Date from,
                                              @Param("to") Date to,
                                              Pageable pageable);

    @EntityGraph(attributePaths = { "hopital" })
    @Query("""
           select p from PriseEnCharge p
           where p.type = :type
             and p.dateEmission between :from and :to
           order by p.dateEmission desc
           """)
    Page<PriseEnCharge> findByTypeInPeriod(@Param("type") BeneficiaireType type,
                                           @Param("from") Date from,
                                           @Param("to") Date to,
                                           Pageable pageable);

    /* ========= Compteurs / stats de base ========= */

    @Query("""
           select count(p) from PriseEnCharge p
           where p.dateEmission between :from and :to
           """)
    long countAllInPeriod(@Param("from") Date from, @Param("to") Date to);

    @Query("""
           select count(p) from PriseEnCharge p
           where p.hopital.id = :hopitalId
             and p.dateEmission between :from and :to
           """)
    long countByHopitalInPeriod(@Param("hopitalId") Long hopitalId,
                                @Param("from") Date from,
                                @Param("to") Date to);

    @Query("""
           select count(p) from PriseEnCharge p
           where p.type = :type
             and p.dateEmission between :from and :to
           """)
    long countByTypeInPeriod(@Param("type") BeneficiaireType type,
                             @Param("from") Date from,
                             @Param("to") Date to);

    @Query("""
           select p.genre, count(p)
           from PriseEnCharge p
           where p.hopital.id = :hopitalId
             and p.dateEmission between :from and :to
           group by p.genre
           """)
    List<Object[]> countByGenreForHopitalInPeriod(@Param("hopitalId") Long hopitalId,
                                                  @Param("from") Date from,
                                                  @Param("to") Date to);

    /* ========= Projection pour les requêtes natives (label, value) ========= */

    interface LabelCount {
        String getLabel();
        Long getValue();
    }

    /* ========= AJOUTS SANS TOUCHER AUX MÉTHODES EXISTANTES ========= */

    @Query(value = """
        SELECT h.nom AS label, COUNT(p.id) AS value
        FROM hopitaux h
        JOIN prise_en_charge p ON p.hopital_id = h.id
        WHERE p.date_emission >= :startMonth AND p.date_emission < :endMonth
        GROUP BY h.id, h.nom
        ORDER BY value DESC
        """, nativeQuery = true)
    List<LabelCount> pecParHopitalMois(
        @Param("startMonth") LocalDateTime startMonth,
        @Param("endMonth")   LocalDateTime endMonth);

    @Query(value = """
        SELECT cat.label AS label, COUNT(*) AS value
        FROM (
          SELECT 'ATTACHE_AGENT' AS label
          FROM prise_en_charge p
          JOIN attaches_agents e ON e.id = p.enfant_id
          WHERE e.agent_id IS NOT NULL
            AND p.date_emission >= :startMonth AND p.date_emission < :endMonth

          UNION ALL

          SELECT 'SENATEUR_EN_ACTIVITE' AS label
          FROM prise_en_charge p
          JOIN senateurs s ON s.id = p.senateur_id
          WHERE s.statut = 'EN_ACTIVITE'
            AND p.date_emission >= :startMonth AND p.date_emission < :endMonth

          UNION ALL

          SELECT 'SENATEUR_HONORAIRE' AS label
          FROM prise_en_charge p
          JOIN senateurs s ON s.id = p.senateur_id
          WHERE s.statut = 'HONORAIRE'
            AND p.date_emission >= :startMonth AND p.date_emission < :endMonth
        ) AS cat
        GROUP BY cat.label
        ORDER BY value DESC
        """, nativeQuery = true)
    List<LabelCount> pecParCategorieMois(
        @Param("startMonth") LocalDateTime startMonth,
        @Param("endMonth")   LocalDateTime endMonth);

    /** Supprime toutes les PEC liées à un agent (bénéficiaire direct, avant suppression de l'agent). */
    void deleteByAgent_Id(Long agentId);

    /** Supprime toutes les PEC liées à un enfant (avant suppression de l'enfant). */
    void deleteByEnfant_Id(Long enfantId);

    /** Supprime toutes les PEC liées à un conjoint (avant suppression du conjoint). */
    void deleteByConjoint_Id(Long conjointId);
}