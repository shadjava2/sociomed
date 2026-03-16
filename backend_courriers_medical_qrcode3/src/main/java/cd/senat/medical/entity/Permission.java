package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

/**
 * Permission (table permissions).
 * coderbac : code unique utilisé pour les contrôles d'accès (ex: AGENT_CREATE, MENU_VIEW).
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_coderbac", columnList = "coderbac", unique = true)
})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @Column(name = "designation")
    private String designation;

    @Size(max = 255)
    @Column(name = "coderbac", unique = true)
    private String coderbac;

    public Permission() {}

    public Permission(String designation, String coderbac) {
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
