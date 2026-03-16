package cd.senat.medical.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cd.senat.medical.entity.Courrier;
import cd.senat.medical.entity.TypeCourrier;

import java.time.LocalDateTime;

@Repository
public interface CourrierRepository extends JpaRepository<Courrier, Long> {

    // Recherche par référence
    Courrier findByRef(String ref);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select coalesce(max(c.sequence), 0)
           from Courrier c
           where c.annee = :annee and c.typeCourrier = :type
           """)
    int findMaxSequenceForYearAndType(@Param("annee") int annee, @Param("type") TypeCourrier type);

    // Recherche avec pagination et filtres
    @Query("SELECT c FROM Courrier c WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(c.expediteur) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "+
           "LOWER(c.destinataire) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "+
           "LOWER(c.objet) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.ref) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:typeCourrier IS NULL OR c.typeCourrier = :typeCourrier) AND " +
           "(:traite IS NULL OR c.traite = :traite) AND " +
           "(:priorite IS NULL OR :priorite = '' OR c.priorite = :priorite) AND " +
           "(:dateDebut IS NULL OR c.dateReception >= :dateDebut) AND " +   // ✅ remplacé par dateReception
           "(:dateFin IS NULL OR c.dateReception <= :dateFin)")             // ✅ remplacé par dateReception
    Page<Courrier> findWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("typeCourrier") TypeCourrier typeCourrier,
            @Param("traite") Boolean traite,
            @Param("priorite") String priorite,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            Pageable pageable
    );

    // Statistiques
    long countByTraite(Boolean traite);
    long countByTypeCourrier(TypeCourrier typeCourrier);
    long countByUrgent(Boolean urgent);

    @Query("SELECT COUNT(c) FROM Courrier c WHERE DATE(c.dateCreation) = CURRENT_DATE")
    long countCreatedToday();

    // Courriers par mois (basés sur dateReception au lieu de dateEnvoi)
    @Query("SELECT c FROM Courrier c WHERE YEAR(c.dateReception) = :year AND MONTH(c.dateReception) = :month")
    Page<Courrier> findByYearAndMonth(@Param("year") int year, @Param("month") int month, Pageable pageable);

    // Courriers urgents non traités
    Page<Courrier> findByUrgentTrueAndTraiteFalse(Pageable pageable);
}
