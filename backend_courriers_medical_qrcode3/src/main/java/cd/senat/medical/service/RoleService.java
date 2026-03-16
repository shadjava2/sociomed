package cd.senat.medical.service;

import cd.senat.medical.dto.RoleCreateRequest;
import cd.senat.medical.dto.RoleDTO;
import cd.senat.medical.dto.RoleRightsUpdateRequest;

import java.util.List;

public interface RoleService {

    List<RoleDTO> findAll();

    RoleDTO findById(Long id);

    RoleDTO create(RoleCreateRequest request);

    RoleDTO update(Long id, String designation);

    void delete(Long id);

    RoleDTO updateRights(Long roleId, RoleRightsUpdateRequest request);
}
