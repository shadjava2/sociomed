// src/main/java/fr/senat/courriersaudiences/repository/AbonnementRepository.java
package cd.senat.medical.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cd.senat.medical.entity.Abonnement;

@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    @Query("""
           select a from Abonnement a
           where a.user.id = :userId
             and a.status = 'ACTIVE'
             and a.startAt <= :now and a.endAt >= :now
           """)
    Optional<Abonnement> findCurrentActive(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("""
           select (count(a) > 0) from Abonnement a
           where a.user.id = :userId and a.status = 'ACTIVE'
             and a.startAt <= :now and a.endAt >= :now
           """)
    boolean existsCurrentActive(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update Abonnement a
           set a.status = 'EXPIRED'
           where a.status = 'ACTIVE' and a.endAt < :now
           """)
    int expireAllEnded(@Param("now") LocalDateTime now);

    Optional<Abonnement> findTopByUser_IdOrderByEndAtDesc(Long userId);
}
