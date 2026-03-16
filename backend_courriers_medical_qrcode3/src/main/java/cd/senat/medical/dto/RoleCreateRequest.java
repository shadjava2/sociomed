package cd.senat.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleCreateRequest {

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 255)
    private String designation;

    public RoleCreateRequest() {}

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
