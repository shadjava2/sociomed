package cd.senat.medical.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cd.senat.medical.entity.Participant;

import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    
    List<Participant> findByAudienceIdOrderByNomAsc(Long audienceId);
    
    void deleteByAudienceIdAndIdNotIn(Long audienceId, List<Long> participantIds);
    
    long countByAudienceIdAndPresentTrue(Long audienceId);
}