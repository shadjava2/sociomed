package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import static cd.senat.medical.util.Texts.up;

@Data
@Entity
@Table(
    name = "conjoints",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_conjoint_agent", columnNames = {"agent_id"}),
        @UniqueConstraint(name = "uk_conjoint_senateur", columnNames = {"senateur_id"}),
        // NOTE: si tu veux inclure lnaiss dans l’unicité, ajoute-la dans les 2 contraintes ci-dessous.
        @UniqueConstraint(name = "uk_conjoint_identite_agent",
            columnNames = {"nom","postnom","prenom","datenaiss","agent_id"}),
        @UniqueConstraint(name = "uk_conjoint_identite_senateur",
            columnNames = {"nom","postnom","prenom","datenaiss","senateur_id"})
    },
    indexes = {
        @Index(name = "idx_conjoint_nom", columnList = "nom,postnom,prenom")
    }
)
public class Conjoint {

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

    /** ⬅️ AJOUT ICI */
    @Size(max = 100)
    private String lnaiss;
    private String profession;
    private String telephone;
    private String email;
    private String photo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", unique = true,
        foreignKey = @ForeignKey(name = "fk_conjoint_agent"))
    private Agent agent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senateur_id", unique = true,
        foreignKey = @ForeignKey(name = "fk_conjoint_senateur"))
    private Senateur senateur;

    @AssertTrue(message = "Associer soit un Agent, soit un Sénateur (un seul)")
    public boolean hasExactlyOneOwner() {
        return (agent != null) ^ (senateur != null);
    }

    @PrePersist @PreUpdate
    private void normalizeUpper() {
        nom = up(nom);
        postnom = up(postnom);
        prenom = up(prenom);
        profession= up(profession);
        lnaiss = up(lnaiss);
        telephone = up(telephone);
        // garde l'email en minuscules (meilleure pratique)
        email = (email == null) ? null : email.trim().toLowerCase();
    }
}
