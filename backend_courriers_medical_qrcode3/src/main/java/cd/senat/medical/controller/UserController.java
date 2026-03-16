package cd.senat.medical.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cd.senat.medical.dto.CreateUserRequest;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.dto.UserDTO;
import cd.senat.medical.repository.RoleRepository;
import cd.senat.medical.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public ResponseEntity<PageResponse<UserDTO>> getAllUsers(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<UserDTO> response = userService.getAllUsers(
                searchTerm, roleId, active, pageable
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        UserDTO createdUser = userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.countTotal());
        stats.put("activeUsers", userService.countByActive(true));
        stats.put("inactiveUsers", userService.countByActive(false));
        stats.put("adminUsers", roleRepository.findByDesignation("ADMIN").map(r -> userService.countByRoleId(r.getId())).orElse(0L));
        stats.put("regularUsers", roleRepository.findByDesignation("USER").map(r -> userService.countByRoleId(r.getId())).orElse(0L));
        stats.put("managerUsers", roleRepository.findByDesignation("MANAGER").map(r -> userService.countByRoleId(r.getId())).orElse(0L));
        stats.put("usersCreatedToday", userService.countCreatedToday());

        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@PathVariable String username) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", userService.existsByUsername(username));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@PathVariable String email) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", userService.existsByEmail(email));
        return ResponseEntity.ok(response);
    }
}