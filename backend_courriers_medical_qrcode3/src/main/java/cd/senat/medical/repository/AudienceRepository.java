package cd.senat.medical.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cd.senat.medical.entity.Audience;
import cd.senat.medical.entity.StatutAudience;
import cd.senat.medical.entity.TypeAudience;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AudienceRepository extends JpaRepository<Audience, Long> {
    
    // Recherche avec pagination et filtres
    @Query("SELECT a FROM Audience a WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(a.titre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.lieu) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:statut IS NULL OR a.statut = :statut) AND " +
           "(:typeAudience IS NULL OR a.typeAudience = :typeAudience) AND " +
           "(:dateDebut IS NULL OR a.dateHeure >= :dateDebut) AND " +
           "(:dateFin IS NULL OR a.dateHeure <= :dateFin)")
    Page<Audience> findWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("statut") StatutAudience statut,
            @Param("typeAudience") TypeAudience typeAudience,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            Pageable pageable
    );
    
    // Audiences à venir
    @Query("SELECT a FROM Audience a WHERE a.dateHeure > :now AND a.statut = 'PLANIFIEE' ORDER BY a.dateHeure ASC")
    List<Audience> findUpcomingAudiences(@Param("now") LocalDateTime now);
    
    // Audiences du jour
    @Query("SELECT a FROM Audience a WHERE DATE(a.dateHeure) = CURRENT_DATE ORDER BY a.dateHeure ASC")
    List<Audience> findTodayAudiences();
    
    // Audiences par statut
    Page<Audience> findByStatut(StatutAudience statut, Pageable pageable);
    
    // Audiences par type
    Page<Audience> findByTypeAudience(TypeAudience typeAudience, Pageable pageable);
    
    // Statistiques
    long countByStatut(StatutAudience statut);
    long countByTypeAudience(TypeAudience typeAudience);
    
    @Query("SELECT COUNT(a) FROM Audience a WHERE DATE(a.createdAt) = CURRENT_DATE")
    long countCreatedToday();
    
    // Audiences dans une période
    @Query("SELECT a FROM Audience a WHERE a.dateHeure BETWEEN :debut AND :fin ORDER BY a.dateHeure ASC")
    List<Audience> findByDateHeureBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}