package cd.senat.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class RoleDTO {

    private Long id;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 255)
    private String designation;

    private List<PermissionDTO> permissions = new ArrayList<>();

    public RoleDTO() {}

    public RoleDTO(Long id, String designation) {
        this.id = id;
        this.designation = designation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public List<PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }
}
