package cd.senat.medical.dto;

import cd.senat.medical.entity.Permission;
import cd.senat.medical.entity.Role;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JwtResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private List<String> permissions;

    public JwtResponse(String accessToken, Long id, String username, String email,
                      String nom, String prenom, Role roleEntity) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.nom = nom;
        this.prenom = prenom;
        this.role = roleEntity != null ? roleEntity.getDesignation() : null;
        this.permissions = roleEntity != null && roleEntity.getPermissions() != null
            ? roleEntity.getPermissions().stream()
                .map(Permission::getCoderbac)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toList())
            : Collections.emptyList();
    }
    
    // Getters et Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getNom() {
        return nom;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public String getPrenom() {
        return prenom;
    }
    
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }
    
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions != null ? permissions : Collections.emptyList();
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}