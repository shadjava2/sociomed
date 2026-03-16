package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "audiences")
public class Audience {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String titre;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    //@NotNull
    private LocalDateTime dateHeure;
    
    @NotNull
    private Integer duree; // en minutes
    
  
    private String lieu;
    
    @Enumerated(EnumType.STRING)
    @NotNull
    private StatutAudience statut;
    
    @Enumerated(EnumType.STRING)
    @NotNull
    private TypeAudience typeAudience;
    
    private String organisateur;
    
    @NotNull
    private LocalDateTime createdAt;
    
    @NotNull
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "audience", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Participant> participants;
    
    // Constructeurs
    public Audience() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Audience(String titre, String description, LocalDateTime dateHeure, 
                   Integer duree, String lieu, StatutAudience statut, 
                   TypeAudience typeAudience, String organisateur) {
        this();
        this.titre = titre;
        this.description = description;
        this.dateHeure = dateHeure;
        this.duree = duree;
        this.lieu = lieu;
        this.statut = statut;
        this.typeAudience = typeAudience;
        this.organisateur = organisateur;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitre() {
        return titre;
    }
    
    public void setTitre(String titre) {
        this.titre = titre;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getDateHeure() {
        return dateHeure;
    }
    
    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }
    
    public Integer getDuree() {
        return duree;
    }
    
    public void setDuree(Integer duree) {
        this.duree = duree;
    }
    
    public String getLieu() {
        return lieu;
    }
    
    public void setLieu(String lieu) {
        this.lieu = lieu;
    }
    
    public StatutAudience getStatut() {
        return statut;
    }
    
    public void setStatut(StatutAudience statut) {
        this.statut = statut;
    }
    
    public TypeAudience getTypeAudience() {
        return typeAudience;
    }
    
    public void setTypeAudience(TypeAudience typeAudience) {
        this.typeAudience = typeAudience;
    }
    
    public String getOrganisateur() {
        return organisateur;
    }
    
    public void setOrganisateur(String organisateur) {
        this.organisateur = organisateur;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<Participant> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }
}