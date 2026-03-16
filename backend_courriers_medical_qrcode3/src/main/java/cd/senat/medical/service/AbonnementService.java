// src/main/java/fr/senat/courriersaudiences/service/AbonnementService.java
package cd.senat.medical.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import cd.senat.medical.entity.*;
import cd.senat.medical.repository.AbonnementRepository;
import cd.senat.medical.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AbonnementService {

    private final AbonnementRepository repo;
    private final UserRepository userRepo;

    public AbonnementService(AbonnementRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public boolean hasActiveSubscription(Long userId) {
        repo.expireAllEnded(LocalDateTime.now());
        return repo.existsCurrentActive(userId, LocalDateTime.now());
    }

    @Transactional
    public Abonnement createSubscription(Long userId, PlanType plan, boolean autoRenew, String currency, Long priceCents, String paymentRef) {
        // Expire d'abord ceux qui doivent l'être
        repo.expireAllEnded(LocalDateTime.now());

        if (repo.existsCurrentActive(userId, LocalDateTime.now())) {
            throw new IllegalStateException("Un abonnement actif existe déjà pour cet utilisateur.");
        }

        var user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = switch (plan) {
            case MONTHLY -> start.plusDays(30);
            case YEARLY -> start.plusDays(365);
        };

        var abo = new Abonnement(user, plan, start, end);
        abo.setStatus(SubscriptionStatus.PENDING_PAYMENT);
        abo.setAutoRenew(autoRenew);
        abo.setCurrency(currency != null ? currency : "USD");
        abo.setPriceCents(priceCents);
        abo.setPaymentRef(paymentRef);

        return repo.save(abo);
    }

    @Transactional
    public Abonnement markPaidAndActivate(Long abonnementId, String paymentRef) {
        var abo = repo.findById(abonnementId).orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable"));
        abo.setStatus(SubscriptionStatus.ACTIVE);
        if (paymentRef != null && (abo.getPaymentRef() == null || abo.getPaymentRef().isBlank())) {
            abo.setPaymentRef(paymentRef);
        }
        return repo.save(abo);
    }

    @Transactional
    public Abonnement cancel(Long abonnementId) {
        var abo = repo.findById(abonnementId).orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable"));
        abo.setStatus(SubscriptionStatus.CANCELED);
        abo.setCanceledAt(LocalDateTime.now());
        abo.setAutoRenew(false);
        return repo.save(abo);
    }

    @Transactional
    public int expireAll() {
        return repo.expireAllEnded(LocalDateTime.now());
    }

    public Abonnement getCurrent(Long userId) {
        return repo.findCurrentActive(userId, LocalDateTime.now()).orElse(null);
    }
}
