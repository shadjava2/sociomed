package cd.senat.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AnnexeDTO {
    
    private Long id;
    
    @NotBlank
    private String nom;
    
    @NotNull
    private Long taille;
    
    @NotBlank
    private String type;
    
    private String url;
    
    // Constructeurs
    public AnnexeDTO() {}
    
    public AnnexeDTO(String nom, Long taille, String type, String url) {
        this.nom = nom;
        this.taille = taille;
        this.type = type;
        this.url = url;
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
    
    public Long getTaille() {
        return taille;
    }
    
    public void setTaille(Long taille) {
        this.taille = taille;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
}