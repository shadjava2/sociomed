package cd.senat.medical.service;

import java.util.List;

import cd.senat.medical.dto.AnnexeDTO;

public interface AnnexeService {
    List<AnnexeDTO> listByCourrier(Long courrierId);
    AnnexeDTO addToCourrier(Long courrierId, AnnexeDTO dto);
    void removeFromCourrier(Long courrierId, Long annexeId);
}
