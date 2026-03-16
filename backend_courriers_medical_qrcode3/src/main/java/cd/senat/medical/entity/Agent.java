package cd.senat.medical.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import static cd.senat.medical.util.Texts.up;

@Data
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
@Entity
@Table(
    name = "agents",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_agent_identite",
            columnNames = {"nom","postnom","prenom","datenaiss","lnaiss"}),
        @UniqueConstraint(name = "uk_agent_tel", columnNames = {"telephone"}),
        @UniqueConstraint(name = "uk_agent_email", columnNames = {"email"})
    },
    indexes = {
        @Index(name = "idx_agents_nom", columnList = "nom,postnom,prenom"),
        @Index(name = "idx_agents_direction", columnList = "direction")
    }
)
public class Agent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(max = 45) private String nom;
    @Size(max = 45) private String postnom;
    @Size(max = 45) private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Genre genre; // M/F

    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date datenaiss;

    @NotNull(message = "lnaiss obligatoire")
    private String lnaiss;

    private String etatc;
    private String village;
    private String photo;
    private String groupement;
    private String secteur;
    private String territoire;
    private String district;
    private String province;
    private String nationalite;

    private String telephone;
    private String email;
    private String adresse;

    @Size(max = 100) private String direction;
    @Size(max = 15)  private String etat;

    /** Catégorie agent : Personnel d'appoint, Agent Administratif, Cadre Administratif */
    @Column(name = "categorie")
    private String categorie;

    @NotNull(message = "stat obligatoire")
    private String stat;

    // ⚠️ on ignore en JSON pour éviter LAZY + boucles dans la LISTE
    @JsonIgnore
    @OneToOne(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Conjoint conjoint;

    @JsonIgnore
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AttachesAgent> enfants;

    @PrePersist @PreUpdate
    private void normalizeUpper() {
        nom = up(nom);
        postnom = up(postnom);
        prenom = up(prenom);
        lnaiss = up(lnaiss);
        etatc = up(etatc);
        village = up(village);
        groupement = up(groupement);
        secteur = up(secteur);
        territoire = up(territoire);
        district = up(district);
        province = up(province);
        nationalite = up(nationalite);
        telephone = (telephone == null) ? null : telephone.trim(); // évite upper sur tel
       // email = (email == null) ? null : email.trim().toLowerCase(); // emails en lowercase
        adresse = up(adresse);
        direction = up(direction);
        etat = up(etat);
        stat = up(stat);
    }
}
