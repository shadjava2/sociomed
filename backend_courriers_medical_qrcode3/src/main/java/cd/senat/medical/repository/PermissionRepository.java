package cd.senat.medical.repository;

import cd.senat.medical.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findAllByOrderByCoderbacAsc();

    Optional<Permission> findByCoderbac(String coderbac);

    boolean existsByCoderbac(String coderbac);
}
