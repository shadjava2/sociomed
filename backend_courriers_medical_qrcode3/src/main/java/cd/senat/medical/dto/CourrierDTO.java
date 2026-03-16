package cd.senat.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonFormat;

import cd.senat.medical.entity.TypeCourrier;

import java.time.LocalDateTime;
import java.util.List;

public class CourrierDTO {

    private Long id;
    private String ref;

    @NotNull
    private TypeCourrier typeCourrier;

    @NotBlank
    private String priorite;

  

    @NotBlank
    private String expediteur;

    @NotBlank
    private String destinataire;

    @NotBlank
    private String objet;

    private String contenu;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd['T'HH:mm[:ss]]")
    private LocalDateTime dateReception;

    @NotNull
    private Boolean traite = false;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd['T'HH:mm[:ss]]")
    private LocalDateTime dateCreation;

    @NotNull
    private Boolean urgent = false;

    private List<AnnexeDTO> annexes;

    public CourrierDTO() {}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public TypeCourrier getTypeCourrier() {
		return typeCourrier;
	}

	public void setTypeCourrier(TypeCourrier typeCourrier) {
		this.typeCourrier = typeCourrier;
	}

	public String getPriorite() {
		return priorite;
	}

	public void setPriorite(String priorite) {
		this.priorite = priorite;
	}



	public String getExpediteur() {
		return expediteur;
	}

	public void setExpediteur(String expediteur) {
		this.expediteur = expediteur;
	}

	public String getDestinataire() {
		return destinataire;
	}

	public void setDestinataire(String destinataire) {
		this.destinataire = destinataire;
	}

	public String getObjet() {
		return objet;
	}

	public void setObjet(String objet) {
		this.objet = objet;
	}

	public String getContenu() {
		return contenu;
	}

	public void setContenu(String contenu) {
		this.contenu = contenu;
	}

	public LocalDateTime getDateReception() {
		return dateReception;
	}

	public void setDateReception(LocalDateTime dateReception) {
		this.dateReception = dateReception;
	}

	public Boolean getTraite() {
		return traite;
	}

	public void setTraite(Boolean traite) {
		this.traite = traite;
	}

	public LocalDateTime getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(LocalDateTime dateCreation) {
		this.dateCreation = dateCreation;
	}

	public Boolean getUrgent() {
		return urgent;
	}

	public void setUrgent(Boolean urgent) {
		this.urgent = urgent;
	}

	public List<AnnexeDTO> getAnnexes() {
		return annexes;
	}

	public void setAnnexes(List<AnnexeDTO> annexes) {
		this.annexes = annexes;
	}

    // Getters / Setters (inchangés)
    // ...
    
    
}
