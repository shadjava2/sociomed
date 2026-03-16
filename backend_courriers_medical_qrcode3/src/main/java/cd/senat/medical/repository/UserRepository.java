package cd.senat.medical.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cd.senat.medical.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    // Recherche avec pagination et filtres
    @Query("SELECT u FROM User u WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.prenom) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:roleId IS NULL OR u.role.id = :roleId) AND " +
           "(:active IS NULL OR u.active = :active)")
    Page<User> findWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("roleId") Long roleId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    long countByRole_Id(Long roleId);
    long countByActive(Boolean active);
    
    @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) = CURRENT_DATE")
    long countCreatedToday();
}