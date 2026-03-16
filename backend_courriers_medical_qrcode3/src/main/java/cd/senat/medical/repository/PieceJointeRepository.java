package cd.senat.medical.repository;

import cd.senat.medical.entity.PieceJointe;
import cd.senat.medical.entity.PieceJointeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {

    // ========= Chargement avec parents (Agent / Sénateur) =========
    @EntityGraph(attributePaths = {"agent", "senateur"})
    @Query("select p from PieceJointe p where p.id = :id")
    Optional<PieceJointe> findByIdWithParent(@Param("id") Long id);

    // ========= Listes paginées par parent =========
    Page<PieceJointe> findByAgent_Id(Long agentId, Pageable pageable);
    Page<PieceJointe> findBySenateur_Id(Long senateurId, Pageable pageable);

    // ========= Listes paginées par parent + type =========
    Page<PieceJointe> findByAgent_IdAndType(Long agentId, PieceJointeType type, Pageable pageable);
    Page<PieceJointe> findBySenateur_IdAndType(Long senateurId, PieceJointeType type, Pageable pageable);

    // ========= Recherche texte paginée (titre/description/nom original), scoped par parent =========
    @Query("""
           select p from PieceJointe p
           where p.agent.id = :agentId and (
                 :q is null or :q = '' or
                 lower(p.titre) like lower(concat('%', :q, '%')) or
                 lower(p.description) like lower(concat('%', :q, '%')) or
                 lower(p.originalName) like lower(concat('%', :q, '%'))
           )
           """)
    Page<PieceJointe> searchForAgent(@Param("agentId") Long agentId,
                                     @Param("q") String q,
                                     Pageable pageable);

    @Query("""
           select p from PieceJointe p
           where p.senateur.id = :senateurId and (
                 :q is null or :q = '' or
                 lower(p.titre) like lower(concat('%', :q, '%')) or
                 lower(p.description) like lower(concat('%', :q, '%')) or
                 lower(p.originalName) like lower(concat('%', :q, '%'))
           )
           """)
    Page<PieceJointe> searchForSenateur(@Param("senateurId") Long senateurId,
                                        @Param("q") String q,
                                        Pageable pageable);

    // ========= Filtres de période (uploadedAt) =========
    Page<PieceJointe> findByAgent_IdAndUploadedAtBetween(Long agentId, Date from, Date to, Pageable pageable);
    Page<PieceJointe> findBySenateur_IdAndUploadedAtBetween(Long senateurId, Date from, Date to, Pageable pageable);

    // ========= Filtres divers =========
    Page<PieceJointe> findByAgent_IdAndMimeType(Long agentId, String mimeType, Pageable pageable);
    Page<PieceJointe> findBySenateur_IdAndMimeType(Long senateurId, String mimeType, Pageable pageable);

    Page<PieceJointe> findByAgent_IdAndChecksumSha256(Long agentId, String checksumSha256, Pageable pageable);
    Page<PieceJointe> findBySenateur_IdAndChecksumSha256(Long senateurId, String checksumSha256, Pageable pageable);

    // ========= Détection de doublons (nom original + type) =========
    @Query("""
           select (count(p) > 0) from PieceJointe p
           where p.agent.id = :agentId
             and lower(p.originalName) = lower(:originalName)
             and p.type = :type
           """)
    boolean existsDuplicateForAgent(@Param("agentId") Long agentId,
                                    @Param("originalName") String originalName,
                                    @Param("type") PieceJointeType type);

    @Query("""
           select (count(p) > 0) from PieceJointe p
           where p.senateur.id = :senateurId
             and lower(p.originalName) = lower(:originalName)
             and p.type = :type
           """)
    boolean existsDuplicateForSenateur(@Param("senateurId") Long senateurId,
                                       @Param("originalName") String originalName,
                                       @Param("type") PieceJointeType type);

    // ========= Détection de doublons (checksum) =========
    @Query("""
           select (count(p) > 0) from PieceJointe p
           where p.agent.id = :agentId and p.checksumSha256 = :checksum
           """)
    boolean existsChecksumForAgent(@Param("agentId") Long agentId,
                                   @Param("checksum") String checksumSha256);

    @Query("""
           select (count(p) > 0) from PieceJointe p
           where p.senateur.id = :senateurId and p.checksumSha256 = :checksum
           """)
    boolean existsChecksumForSenateur(@Param("senateurId") Long senateurId,
                                      @Param("checksum") String checksumSha256);

    // ========= Compteurs utiles =========
    long countByAgent_Id(Long agentId);
    long countBySenateur_Id(Long senateurId);
    long countByAgent_IdAndType(Long agentId, PieceJointeType type);
    long countBySenateur_IdAndType(Long senateurId, PieceJointeType type);

    // ========= Suppressions ciblées (utile pour nettoyage) =========
    void deleteByAgent_Id(Long agentId);
    void deleteBySenateur_Id(Long senateurId);

    // ========= Accès direct utile =========
    Optional<PieceJointe> findByFileName(String fileName);
}
