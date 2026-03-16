package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(
    name = "courriers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_courrier_annee_type_seq", columnNames = {"annee", "type_courrier", "sequence"}),
        @UniqueConstraint(name = "uk_courrier_ref", columnNames = {"ref"})
    }
)
public class Courrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String ref;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "type_courrier", length = 16, nullable = false)
    private TypeCourrier typeCourrier;

    @NotBlank
    private String priorite;

    //@NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd['T'HH:mm[:ss]]")
    private LocalDateTime dateEnvoi;

    @NotBlank
    private String expediteur;

    @NotBlank
    private String destinataire;

    @NotBlank
    private String objet;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd['T'HH:mm[:ss]]")
    private LocalDateTime dateReception;

    @NotNull
    private Boolean traite = false;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd['T'HH:mm[:ss]]")
    private LocalDateTime dateCreation;

    @NotNull
    private Boolean urgent = false;

    @OneToMany(mappedBy = "courrier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Annexe> annexes;

    // === NOUVEAU : pilotage de la référence ===
    @Column(nullable = false)
    private Integer annee;     // ex : 2025

    @Column(nullable = false)
    private Integer sequence;  // ex : 1, 2, 3...

    public Courrier() {
        this.dateCreation = LocalDateTime.now();
    }

    public Courrier(String ref, TypeCourrier typeCourrier, String priorite,
                    LocalDateTime dateEnvoi, String expediteur, String destinataire,
                    String objet, String contenu) {
        this();
        this.ref = ref;
        this.typeCourrier = typeCourrier;
        this.priorite = priorite;
        this.dateEnvoi = dateEnvoi;
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.objet = objet;
        this.contenu = contenu;
    }

    @PrePersist
    public void prePersist() {
        if (this.dateCreation == null) this.dateCreation = LocalDateTime.now();
        if (this.traite == null) this.traite = false;
        if (this.urgent == null) this.urgent = false;

        // Construire ref si absente (le service aura déjà posé annee/sequence/typeCourrier)
        if (this.ref == null || this.ref.isBlank()) {
            String prefix = (this.typeCourrier == TypeCourrier.RECU) ? "CR" : "CE";
            // Format : CR-2025-0001/SENAT
            this.ref = String.format("%s-%d-%04d/%s", prefix, this.annee, this.sequence, "SENAT");
        }
    }

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public TypeCourrier getTypeCourrier() { return typeCourrier; }
    public void setTypeCourrier(TypeCourrier typeCourrier) { this.typeCourrier = typeCourrier; }

    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public String getExpediteur() { return expediteur; }
    public void setExpediteur(String expediteur) { this.expediteur = expediteur; }

    public String getDestinataire() { return destinataire; }
    public void setDestinataire(String destinataire) { this.destinataire = destinataire; }

    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateReception() { return dateReception; }
    public void setDateReception(LocalDateTime dateReception) { this.dateReception = dateReception; }

    public Boolean getTraite() { return traite; }
    public void setTraite(Boolean traite) { this.traite = traite; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public Boolean getUrgent() { return urgent; }
    public void setUrgent(Boolean urgent) { this.urgent = urgent; }

    public List<Annexe> getAnnexes() { return annexes; }
    public void setAnnexes(List<Annexe> annexes) { this.annexes = annexes; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
}
