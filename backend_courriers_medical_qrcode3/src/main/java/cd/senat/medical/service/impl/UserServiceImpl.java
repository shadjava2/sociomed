package cd.senat.medical.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.senat.medical.dto.CreateUserRequest;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.dto.UserDTO;
import cd.senat.medical.entity.Role;
import cd.senat.medical.entity.User;
import cd.senat.medical.repository.RoleRepository;
import cd.senat.medical.repository.UserRepository;
import cd.senat.medical.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public PageResponse<UserDTO> getAllUsers(
            String searchTerm,
            Long roleId,
            Boolean active,
            Pageable pageable) {

        Page<User> usersPage = userRepository.findWithFilters(
                searchTerm, roleId, active, pageable
        );
        
        List<UserDTO> userDTOs = usersPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                userDTOs,
                usersPage.getTotalPages(),
                usersPage.getTotalElements(),
                usersPage.getNumber(),
                usersPage.getSize(),
                usersPage.isFirst(),
                usersPage.isLast(),
                usersPage.isEmpty()
        );
    }
    
    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        return convertToDTO(user);
    }
    
    @Override
    public UserDTO createUser(CreateUserRequest createUserRequest) {
        // Vérifier si l'utilisateur existe déjà
        if (userRepository.existsByUsername(createUserRequest.getUsername())) {
            throw new RuntimeException("Erreur: Le nom d'utilisateur est déjà pris!");
        }
        
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            throw new RuntimeException("Erreur: L'email est déjà utilisé!");
        }
        
        // Créer le nouvel utilisateur
        User user = new User();
        user.setUsername(createUserRequest.getUsername());
        user.setEmail(createUserRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        user.setNom(createUserRequest.getNom());
        user.setPrenom(createUserRequest.getPrenom());
        user.setFonction(createUserRequest.getFonction());
        user.setTelephone(createUserRequest.getTelephone());
        Role role = roleRepository.findById(createUserRequest.getRoleId())
                .orElseThrow(() -> new RuntimeException("Rôle introuvable avec l'ID: " + createUserRequest.getRoleId()));
        user.setRole(role);

        user = userRepository.save(user);
        return convertToDTO(user);
    }
    
    @Override
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        // Vérifier l'unicité du username et email (sauf pour l'utilisateur actuel)
        if (!existingUser.getUsername().equals(userDTO.getUsername()) && 
            userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("Erreur: Le nom d'utilisateur est déjà pris!");
        }
        
        if (!existingUser.getEmail().equals(userDTO.getEmail()) && 
            userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Erreur: L'email est déjà utilisé!");
        }
        
        // Mettre à jour les champs
        existingUser.setUsername(userDTO.getUsername());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setNom(userDTO.getNom());
        existingUser.setPrenom(userDTO.getPrenom());
        existingUser.setFonction(userDTO.getFonction());
        existingUser.setTelephone(userDTO.getTelephone());
        if (userDTO.getRoleId() != null) {
            Role role = roleRepository.findById(userDTO.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Rôle introuvable avec l'ID: " + userDTO.getRoleId()));
            existingUser.setRole(role);
        }
        existingUser.setActive(userDTO.getActive());
        
        existingUser = userRepository.save(existingUser);
        return convertToDTO(existingUser);
    }
    
    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + id);
        }
        userRepository.deleteById(id);
    }
    
    @Override
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        user.setActive(!user.getActive());
        userRepository.save(user);
    }
    
    @Override
    public long countByRoleId(Long roleId) {
        return userRepository.countByRole_Id(roleId);
    }
    
    @Override
    public long countByActive(Boolean active) {
        return userRepository.countByActive(active);
    }
    
    @Override
    public long countCreatedToday() {
        return userRepository.countCreatedToday();
    }
    
    @Override
    public long countTotal() {
        return userRepository.count();
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = modelMapper.map(user, UserDTO.class);
        if (user.getRole() != null) {
            dto.setRoleId(user.getRole().getId());
            dto.setRoleDesignation(user.getRole().getDesignation());
        }
        return dto;
    }
    @Override
    public String getFullnameByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        return userRepository.findByUsername(username)
                .map(u -> ((u.getNom() == null ? "" : u.getNom().trim()) + " " +
                           (u.getPrenom() == null ? "" : u.getPrenom().trim())).trim())
                .filter(s -> !s.isBlank())
                .orElse(null);
    }
}