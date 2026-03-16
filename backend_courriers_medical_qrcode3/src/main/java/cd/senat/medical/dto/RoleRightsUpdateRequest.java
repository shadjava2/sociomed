package cd.senat.medical.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Liste des IDs de permissions à assigner au rôle.
 */
public class RoleRightsUpdateRequest {

    private List<Long> permissionIds = new ArrayList<>();

    public RoleRightsUpdateRequest() {}

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds != null ? permissionIds : new ArrayList<>();
    }
}
