package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "participants")
public class Participant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String nom;
    
    @NotBlank
    private String prenom;
    
    private String fonction;
    
    @Email
    @NotBlank
    private String email;
    
    private String telephone;
    
    private Boolean present;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audience_id")
    private Audience audience;
    
    // Constructeurs
    public Participant() {}
    
    public Participant(String nom, String prenom, String fonction, String email, String telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.fonction = fonction;
        this.email = email;
        this.telephone = telephone;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getTelephone() {
        return telephone;
    }
    
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
    
    public Boolean getPresent() {
        return present;
    }
    
    public void setPresent(Boolean present) {
        this.present = present;
    }
    
    public Audience getAudience() {
        return audience;
    }
    
    public void setAudience(Audience audience) {
        this.audience = audience;
    }
}