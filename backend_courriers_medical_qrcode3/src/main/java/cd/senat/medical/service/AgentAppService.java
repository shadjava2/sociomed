package cd.senat.medical.service;

import cd.senat.medical.dto.AgentsDTO;
import cd.senat.medical.dto.PageResponse;
import cd.senat.medical.entity.Genre;
import org.springframework.data.domain.Pageable;

public interface AgentAppService {
  PageResponse<AgentsDTO.Summary> getAll(String q, Genre genre, String etat, String direction, String categorie, Pageable pageable);
  AgentsDTO.Detail getDetails(Long id);
  AgentsDTO.Detail getById(Long id); // version sans forcer les relations (utile si besoin)
  AgentsDTO.Detail create(AgentsDTO.CreateRequest req);
  AgentsDTO.Detail update(Long id, AgentsDTO.UpdateRequest req);
  void delete(Long id);

  long countByGenre(Genre genre);
  long countByEtat(String etat);
}
