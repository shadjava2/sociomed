package cd.senat.medical.repository;

import cd.senat.medical.entity.DocumentPersonne;
import cd.senat.medical.entity.TypeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPersonneRepository extends JpaRepository<DocumentPersonne, Long> {

    // ---------- Anti-doublons ----------
    boolean existsByStoredFilenameIgnoreCase(String storedFilename);

    boolean existsByAgent_IdAndTypeAndOriginalFilenameIgnoreCase(
            Long agentId, TypeDocument type, String originalFilename);

    boolean existsBySenateur_IdAndTypeAndOriginalFilenameIgnoreCase(
            Long senateurId, TypeDocument type, String originalFilename);

    // ---------- Chargements ----------
    @EntityGraph(attributePaths = {"agent", "senateur"})
    @Query("select d from DocumentPersonne d where d.id = :id")
    Optional<DocumentPersonne> findByIdWithOwner(@Param("id") Long id);

    List<DocumentPersonne> findByAgent_Id(Long agentId);
    List<DocumentPersonne> findBySenateur_Id(Long senateurId);

    Page<DocumentPersonne> findByAgent_Id(Long agentId, Pageable pageable);
    Page<DocumentPersonne> findBySenateur_Id(Long senateurId, Pageable pageable);

    Page<DocumentPersonne> findByActifTrue(Pageable pageable);
    long countByActifTrue();

    // ---------- Filtres ----------
    Page<DocumentPersonne> findByAgent_IdAndType(Long agentId, TypeDocument type, Pageable pageable);
    Page<DocumentPersonne> findBySenateur_IdAndType(Long senateurId, TypeDocument type, Pageable pageable);

    // Recherche simple (label / nom d’origine)
    @Query("""
        select d from DocumentPersonne d
        where (:q is null or :q = '' or
               lower(d.label) like lower(concat('%', :q, '%')) or
               lower(d.originalFilename) like lower(concat('%', :q, '%')))
          and (:type is null or d.type = :type)
          and (:actif is null or d.actif = :actif)
        """)
    Page<DocumentPersonne> search(@Param("q") String q,
                                  @Param("type") TypeDocument type,
                                  @Param("actif") Boolean actif,
                                  Pageable pageable);

    // ---------- Suppressions ciblées (optionnelles) ----------
    long deleteByAgent_Id(Long agentId);
    long deleteBySenateur_Id(Long senateurId);
}
