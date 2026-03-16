// src/main/java/cd/senat/medical/mapper/AttachesAgentMapper.java
package cd.senat.medical.mapper;

import cd.senat.medical.dto.AttachesAgentDTO;
import cd.senat.medical.entity.AttachesAgent;

import java.util.List;

public final class AttachesAgentMapper {

  private AttachesAgentMapper() {}

  // Entity -> DTO (Item)
  public static AttachesAgentDTO.Item toItem(AttachesAgent e) {
    if (e == null) return null;
    return new AttachesAgentDTO.Item(
        e.getId(),
        e.getNomEnfant(),
        e.getPostnomEnfant(),
        e.getPrenomEnfant(),
        e.getGenre(),
        e.getDatenaiss(),
        e.getCategorie(),
        e.getStat(),
        e.getReference(),
        e.getPhoto()
    );
  }

  public static List<AttachesAgentDTO.Item> toItems(List<AttachesAgent> list) {
    return list == null ? List.of() : list.stream().map(AttachesAgentMapper::toItem).toList();
  }

  // DTO Create -> Entity
  public static AttachesAgent fromCreate(AttachesAgentDTO.CreateRequest r) {
    if (r == null) return null;
    var e = new AttachesAgent();
    AttachesAgentDTO.apply(e, r);
    return e;
  }

  // DTO Update -> apply
  public static void applyUpdate(AttachesAgent target, AttachesAgentDTO.UpdateRequest r) {
    AttachesAgentDTO.apply(target, r);
  }
}
