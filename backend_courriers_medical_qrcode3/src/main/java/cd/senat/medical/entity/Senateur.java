package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import static cd.senat.medical.util.Texts.up;

@Data
@Entity
@Table(
    name = "senateurs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_senateur_identite",
            columnNames = {"nom","postnom","prenom","datenaiss"})
    },
    indexes = {
        @Index(name = "idx_senateur_nom", columnList = "nom,postnom,prenom"),
        @Index(name = "idx_senateur_statut", columnList = "statut")
    }
)
public class Senateur {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 45) private String nom;
    @Size(max = 45) private String postnom;
    @Size(max = 45) private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Genre genre;
    
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date datenaiss;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutSenateur statut; // EN_ACTIVITE / HONORAIRE

    private String telephone;
    private String legislature;
    private String email;
    private String adresse;
    private String photo;

    @OneToOne(mappedBy = "senateur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Conjoint conjoint;

    @OneToMany(mappedBy = "senateur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AttachesAgent> enfants;

    @PrePersist @PreUpdate
    private void normalizeUpper() {
        nom = up(nom);
        postnom = up(postnom);
        prenom = up(prenom);
        telephone = up(telephone);
        legislature = up(legislature);
        email = up(email);
        adresse = up(adresse);
    }
}
