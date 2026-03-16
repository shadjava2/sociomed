package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cd.senat.medical.util.Texts.up;

@Data
@Entity
@Table(
    name = "hopitaux",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_hopital_code", columnNames = {"code"}),
        @UniqueConstraint(name = "uk_hopital_nom_ville", columnNames = {"nom","ville"})
    },
    indexes = {
        @Index(name = "idx_hopital_actif", columnList = "actif"),
        @Index(name = "idx_hopital_ville", columnList = "ville"),
        @Index(name = "idx_hopital_categorie", columnList = "categorie")
    }
)
public class Hopital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String code;

    @NotBlank @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String nom;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CategorieHopital categorie;

    @Size(max = 255) private String adresse;
    @Size(max = 80)  private String commune;
    @Size(max = 80)  private String ville;
    @Size(max = 80)  private String province;
    @Size(max = 80)  private String pays;

    @Size(max = 80)  private String contactNom;
    @Size(max = 40)  private String contactTelephone;

    @Email @Size(max = 120) private String email;
    @Size(max = 160) private String siteWeb;

    @NotNull @Column(nullable = false)
    private Boolean actif = true;

    @Size(max = 50)
    private String numeroConvention;

    private LocalDateTime conventionDebut;
    private LocalDateTime conventionFin;

    @Digits(integer = 12, fraction = 2)
    private BigDecimal plafondMensuel;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "hopital", cascade = CascadeType.PERSIST, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<PriseEnCharge> prisesEnCharge = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = createdAt;
        if (actif == null) actif = Boolean.TRUE;
        if (categorie == null) categorie = CategorieHopital.PRIVE;

        normalizeUpper();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeUpper();
    }

    private void normalizeUpper() {
        code = up(code);
        nom = up(nom);
        adresse = up(adresse);
        commune = up(commune);
        ville = up(ville);
        province = up(province);
        pays = up(pays);
        contactNom = up(contactNom);
        contactTelephone = up(contactTelephone);
        email = up(email);
        siteWeb = up(siteWeb);
        numeroConvention = up(numeroConvention);
    }

    // Helpers (optionnels)
    public void addPriseEnCharge(PriseEnCharge pec) {
        if (pec == null) return;
        this.prisesEnCharge.add(pec);
        pec.setHopital(this);
    }

    public void removePriseEnCharge(PriseEnCharge pec) {
        if (pec == null) return;
        this.prisesEnCharge.remove(pec);
        pec.setHopital(null);
    }
}
