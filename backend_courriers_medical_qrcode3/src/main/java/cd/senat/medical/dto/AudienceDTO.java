package cd.senat.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

import cd.senat.medical.entity.StatutAudience;
import cd.senat.medical.entity.TypeAudience;

public class AudienceDTO {
    
    private Long id;
    
    @NotBlank
    private String titre;
    
    private String description;
    
    //@NotNull
    private LocalDateTime dateHeure;
    
   // @NotNull
    private Integer duree;
    
    //@NotBlank
   // private String lieu;
    
    @NotNull
    private StatutAudience statut;
    
    @NotNull
    private TypeAudience typeAudience;
    
    private String organisateur;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private List<ParticipantDTO> participants;
    
    // Constructeurs
    public AudienceDTO() {}
    
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
    
    public List<ParticipantDTO> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<ParticipantDTO> participants) {
        this.participants = participants;
    }
}