package cd.senat.medical.repository;

import cd.senat.medical.entity.Conjoint;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface ConjointRepository extends JpaRepository<Conjoint, Long> {

    Optional<Conjoint> findByAgent_Id(Long agentId);
    Optional<Conjoint> findBySenateur_Id(Long senateurId);

    boolean existsByAgent_Id(Long agentId);
    boolean existsBySenateur_Id(Long senateurId);

    long deleteByAgent_Id(Long agentId);
    long deleteBySenateur_Id(Long senateurId);
    
  

    boolean existsByAgent_IdAndNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaiss(
        Long agentId, String nom, String postnom, String prenom, Date datenaiss);

    boolean existsBySenateur_IdAndNomIgnoreCaseAndPostnomIgnoreCaseAndPrenomIgnoreCaseAndDatenaiss(
        Long senateurId, String nom, String postnom, String prenom, Date datenaiss);

}
