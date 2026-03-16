package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

import static cd.senat.medical.util.Texts.up;

@Data
@Entity
@Table(
    name = "attaches_agents",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_enfant_identite_agent",
            columnNames = {"nom_enfant","postnom_enfant","prenom_enfant","datenaiss","agent_id"}),
        @UniqueConstraint(name = "uk_enfant_identite_senateur",
            columnNames = {"nom_enfant","postnom_enfant","prenom_enfant","datenaiss","senateur_id"})
    },
    indexes = {
        @Index(name = "idx_attache_nom", columnList = "nom_enfant,postnom_enfant,prenom_enfant"),
        @Index(name = "idx_attache_parent_agent", columnList = "agent_id"),
        @Index(name = "idx_attache_parent_senateur", columnList = "senateur_id")
    }
)
public class AttachesAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // ✅ Champs Java en camelCase, colonnes SQL conservées en snake_case
    @NotEmpty
    @Size(max = 45)
    @Column(name = "nom_enfant", length = 45, nullable = false)
    private String nomEnfant;

    @Column(length = 255)
    private String reference;

    @NotEmpty
    @Size(max = 45)
    @Column(name = "postnom_enfant", length = 45, nullable = false)
    private String postnomEnfant;

    @Size(max = 45)
    @Column(name = "prenom_enfant", length = 45)
    private String prenomEnfant;

    @Size(max = 45)
    @Column(name = "stat", length = 45)
    private String stat;

   // @Column(length = 255)
    private String photo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 1, nullable = false)
    private Genre genre; // M/F

    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "datenaiss", nullable = false)
    private Date datenaiss;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CategorieEnfant categorie; // LEGITIME / ADOPTIF

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id",
        foreignKey = @ForeignKey(name = "fk_attache_agent"))
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senateur_id",
        foreignKey = @ForeignKey(name = "fk_attache_senateur"))
    private Senateur senateur;

    @AssertTrue(message = "Associer soit un Agent, soit un Sénateur (un seul)")
    public boolean isExactlyOneParent() {
        return (agent != null) ^ (senateur != null);
    }

    @PrePersist
    @PreUpdate
    private void normalizeUpper() {
        nomEnfant = up(nomEnfant);
        postnomEnfant = up(postnomEnfant);
        prenomEnfant = up(prenomEnfant);
        stat = up(stat);
        reference = up(reference);
    }


}
