package cd.senat.medical.service;

import cd.senat.medical.dto.PermissionDTO;

import java.util.List;

public interface PermissionService {

    List<PermissionDTO> findAll();
}
