package cd.senat.medical.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cd.senat.medical.entity.Annexe;

import java.util.List;

@Repository
public interface AnnexeRepository extends JpaRepository<Annexe, Long> {
    
    List<Annexe> findByCourrierIdOrderByNomAsc(Long courrierId);
    
    void deleteByCourrierIdAndIdNotIn(Long courrierId, List<Long> annexeIds);

    @Modifying
    @Query("delete from Annexe a where a.id = :annexeId and a.courrier.id = :courrierId")
    int hardDeleteByIdAndCourrierId(@Param("courrierId") Long courrierId,
                                    @Param("annexeId") Long annexeId);
}