package cd.senat.medical.dto;

public class PermissionDTO {

    private Long id;
    private String designation;
    private String coderbac;

    public PermissionDTO() {}

    public PermissionDTO(Long id, String designation, String coderbac) {
        this.id = id;
        this.designation = designation;
        this.coderbac = coderbac;
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

    public String getCoderbac() {
        return coderbac;
    }

    public void setCoderbac(String coderbac) {
        this.coderbac = coderbac;
    }
}
