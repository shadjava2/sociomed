package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

import static cd.senat.medical.util.Texts.up;

/**
 * Métadonnées d’un document stocké physiquement dans 'uploads/'.
 * Propriétaire: EXACTEMENT un (Agent OU Sénateur).
 */
@Data
@Entity
@Table(
    name = "documents_personnes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_doc_stored_filename", columnNames = {"storedFilename"})
    },
    indexes = {
        @Index(name = "idx_doc_agent", columnList = "agent_id"),
        @Index(name = "idx_doc_senateur", columnList = "senateur_id"),
        @Index(name = "idx_doc_type", columnList = "type")
    }
)
public class DocumentPersonne {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "agent_id",
            foreignKey = @ForeignKey(name = "fk_doc_agent"))
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "senateur_id",
            foreignKey = @ForeignKey(name = "fk_doc_senateur"))
    private Senateur senateur;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TypeDocument type;

    @NotBlank @Size(max = 255)
    private String originalFilename;

    @NotBlank @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String storedFilename;       // UUID + extension

    @NotBlank @Size(max = 255)
    @Column(nullable = false)
    private String relativeDirectory;    // ex: "agents/42" ou "senateurs/7"

    @Size(max = 120)
    private String contentType;          // "APPLICATION/PDF", "IMAGE/JPEG", ...

    @PositiveOrZero
    private Long size;                   // en octets

    @Size(max = 64)
    private String sha256;               // checksum optionnel

    @Size(max = 255)
    private String label;

    @NotNull @Column(nullable = false)
    private Boolean actif = true;

    @NotNull @Column(nullable = false)
    private LocalDateTime createdAt;

    @NotNull @Column(nullable = false)
    private LocalDateTime updatedAt;

    // EXACTEMENT un propriétaire
    @AssertTrue(message = "Associer EXACTEMENT un propriétaire (Agent ou Sénateur)")
    public boolean hasExactlyOneOwner() {
        return (agent != null) ^ (senateur != null);
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = createdAt;
        if (actif == null) actif = Boolean.TRUE;
        normalizeUpper();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeUpper();
    }

    private void normalizeUpper() {
        originalFilename  = up(originalFilename);
        storedFilename    = up(storedFilename);
        relativeDirectory = up(relativeDirectory);
        contentType       = up(contentType);
        sha256            = up(sha256);
        label             = up(label);
    }

    @Transient
    public String getRelativePath() {
        String base = (relativeDirectory == null) ? "" : relativeDirectory;
        if (!base.isBlank() && !base.endsWith("/")) base = base + "/";
        return base + storedFilename;
    }
}
