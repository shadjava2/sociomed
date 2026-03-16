package cd.senat.medical.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
    
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;
    
    @Email
    @NotBlank
    private String email;
    
    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
    
    @NotBlank
    private String nom;
    
    @NotBlank
    private String prenom;
    
    private String fonction;
    
    private String telephone;

    @NotNull
    private Long roleId;
    
    // Constructeurs
    public CreateUserRequest() {}
    
    // Getters et Setters
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
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
    
    public String getFonction() {
        return fonction;
    }
    
    public void setFonction(String fonction) {
        this.fonction = fonction;
    }
    
    public String getTelephone() {
        return telephone;
    }
    
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
    
    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}