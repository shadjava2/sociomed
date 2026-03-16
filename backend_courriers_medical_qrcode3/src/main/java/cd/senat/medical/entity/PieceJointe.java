package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

import static cd.senat.medical.util.Texts.up;

@Data
@Entity
@Table(
    name = "pieces_jointes",
    indexes = {
        @Index(name = "idx_pj_agent", columnList = "agent_id"),
        @Index(name = "idx_pj_senateur", columnList = "senateur_id"),
        @Index(name = "idx_pj_type", columnList = "type")
    }
)
public class PieceJointe {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Titre libre pour l’UI (ex: "Diplôme Licence 2015") */
    @Size(max = 120)
    private String titre;

    /** Description optionnelle */
    @Size(max = 255)
    private String description;

    /** Type métier (diplôme, acte, etc.) */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PieceJointeType type;

    /** ====== Métadonnées fichier ====== */
    @NotBlank
    @Size(max = 200)
    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;          // nom stocké sur disque (généré, unique)

    @NotBlank
    @Size(max = 200)
    @Column(name = "original_name", nullable = false, length = 200)
    private String originalName;      // nom original tel que fourni par l’utilisateur

    @NotBlank
    @Size(max = 100)
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;          // ex: application/pdf

    @Positive
    @Column(name = "size_bytes")
    private Long sizeBytes;           // taille en octets

    @Size(max = 64)
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;    // optionnel: pour détection doublons/intégrité

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "uploaded_at", nullable = false)
    private Date uploadedAt = new Date();

    /** ====== Liens parents (un seul à la fois) ====== */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id",
        foreignKey = @ForeignKey(name = "fk_pj_agent"))
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senateur_id",
        foreignKey = @ForeignKey(name = "fk_pj_senateur"))
    private Senateur senateur;

    /** Validation: exactement un parent (XOR) */
    @AssertTrue(message = "Associer soit un Agent, soit un Sénateur (un seul)")
    public boolean isExactlyOneParent() {
        return (agent != null) ^ (senateur != null);
    }

    @PrePersist @PreUpdate
    private void normalizeUpper() {
        titre = up(titre);
        description = up(description);
        originalName = up(originalName);
        mimeType = up(mimeType);
        fileName = up(fileName);
        checksumSha256 = up(checksumSha256);
    }
}
