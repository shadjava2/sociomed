package cd.senat.medical.service.impl;

import cd.senat.medical.dto.PermissionDTO;
import cd.senat.medical.dto.RoleCreateRequest;
import cd.senat.medical.dto.RoleDTO;
import cd.senat.medical.dto.RoleRightsUpdateRequest;
import cd.senat.medical.entity.Permission;
import cd.senat.medical.entity.Role;
import cd.senat.medical.repository.PermissionRepository;
import cd.senat.medical.repository.RoleRepository;
import cd.senat.medical.repository.UserRepository;
import cd.senat.medical.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleServiceImpl(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> findAll() {
        return roleRepository.findAllByOrderByDesignationAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDTO findById(Long id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable: " + id));
        return toDTOWithPermissions(role);
    }

    @Override
    public RoleDTO create(RoleCreateRequest request) {
        if (roleRepository.existsByDesignation(request.getDesignation().trim())) {
            throw new RuntimeException("Un rôle avec cette désignation existe déjà.");
        }
        Role role = new Role();
        role.setDesignation(request.getDesignation().trim());
        role = roleRepository.save(role);
        return toDTO(role);
    }

    @Override
    public RoleDTO update(Long id, String designation) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable: " + id));
        String d = designation != null ? designation.trim() : "";
        if (!d.equals(role.getDesignation()) && roleRepository.existsByDesignation(d)) {
            throw new RuntimeException("Un rôle avec cette désignation existe déjà.");
        }
        role.setDesignation(d);
        role = roleRepository.save(role);
        return toDTOWithPermissions(roleRepository.findByIdWithPermissions(id).orElse(role));
    }

    @Override
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable: " + id));
        if (userRepository.countByRole_Id(id) > 0) {
            throw new RuntimeException("Impossible de supprimer ce rôle : des utilisateurs y sont rattachés.");
        }
        roleRepository.delete(role);
    }

    @Override
    public RoleDTO updateRights(Long roleId, RoleRightsUpdateRequest request) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable: " + roleId));
        Set<Permission> newPerms = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> all = permissionRepository.findAllById(request.getPermissionIds());
            newPerms.addAll(all);
        }
        role.setPermissions(newPerms);
        role = roleRepository.save(role);
        return toDTOWithPermissions(role);
    }

    private RoleDTO toDTO(Role role) {
        RoleDTO dto = new RoleDTO(role.getId(), role.getDesignation());
        return dto;
    }

    private RoleDTO toDTOWithPermissions(Role role) {
        RoleDTO dto = new RoleDTO(role.getId(), role.getDesignation());
        if (role.getPermissions() != null) {
            dto.setPermissions(role.getPermissions().stream()
                    .map(p -> new PermissionDTO(p.getId(), p.getDesignation(), p.getCoderbac()))
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
