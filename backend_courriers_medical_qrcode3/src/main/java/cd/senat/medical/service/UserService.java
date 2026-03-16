package cd.senat.medical.service;

import org.springframework.data.domain.Pageable;

import cd.senat.medical.dto.CreateUserRequest;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.dto.UserDTO;

public interface UserService {

    PageResponse<UserDTO> getAllUsers(
            String searchTerm,
            Long roleId,
            Boolean active,
            Pageable pageable
    );

    UserDTO getUserById(Long id);

    UserDTO createUser(CreateUserRequest createUserRequest);

    UserDTO updateUser(Long id, UserDTO userDTO);

    void deleteUser(Long id);

    void toggleUserStatus(Long id);

    long countByRoleId(Long roleId);
    long countByActive(Boolean active);
    long countCreatedToday();
    long countTotal();
    
    // Validation
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    String getFullnameByUsername(String username);
   }
