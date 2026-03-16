package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "annexes")
public class Annexe {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String nom;
    
    @NotNull
    private Long taille;
    
    @NotBlank
    private String type;
    
    private String url;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courrier_id")
    private Courrier courrier;
    
    // Constructeurs
    public Annexe() {}
    
    public Annexe(String nom, Long taille, String type, String url) {
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
    
    public Courrier getCourrier() {
        return courrier;
    }
    
    public void setCourrier(Courrier courrier) {
        this.courrier = courrier;
    }
}