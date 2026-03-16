package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
    name = "abonnements",
    indexes = {
        @Index(name = "idx_abonnement_user", columnList = "user_id"),
        @Index(name = "idx_abonnement_status", columnList = "status")
    }
)
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Abonné
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_abonnement_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 16)
    private PlanType plan;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.PENDING_PAYMENT;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startAt;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime endAt;

    // Paiement (optionnel mais utile pour réconciliation)
    private String paymentRef;          // ID transaction PSP / opérateur
    private String currency = "USD";    // ou "CDF"
    private Long priceCents;            // prix payé en centimes

    // Renouvellement
    @NotNull
    @Column(nullable = false)
    private Boolean autoRenew = false;

    private LocalDateTime canceledAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Abonnement() {}

    public Abonnement(User user, PlanType plan, LocalDateTime startAt, LocalDateTime endAt) {
        this.user = user;
        this.plan = plan;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = SubscriptionStatus.PENDING_PAYMENT;
        this.autoRenew = false;
    }

    // Helpers
    public boolean isActiveNow() {
        LocalDateTime now = LocalDateTime.now();
        return status == SubscriptionStatus.ACTIVE && !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    // Getters/Setters
    // ... (génère avec Lombok si tu veux @Getter/@Setter) ...
    // (Pour concision, conserve ton style actuel et génère via IDE)
    
    // Idem pour tous les champs
    // getId(), setId(), getUser(), setUser(), etc.
}