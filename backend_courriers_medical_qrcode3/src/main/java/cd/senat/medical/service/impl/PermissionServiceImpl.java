package cd.senat.medical.service.impl;

import cd.senat.medical.dto.PermissionDTO;
import cd.senat.medical.entity.Permission;
import cd.senat.medical.repository.PermissionRepository;
import cd.senat.medical.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<PermissionDTO> findAll() {
        return permissionRepository.findAllByOrderByCoderbacAsc().stream()
                .map(p -> new PermissionDTO(p.getId(), p.getDesignation(), p.getCoderbac()))
                .collect(Collectors.toList());
    }
}
