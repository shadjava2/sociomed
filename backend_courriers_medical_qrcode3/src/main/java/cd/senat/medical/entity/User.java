package cd.senat.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank
    private String username;

    @Email
    @Column(unique = true, nullable = false)
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    private String fonction;

    private String telephone;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fkrole", referencedColumnName = "id", nullable = false)
    private Role role;

    @NotNull
    private Boolean active = true;

    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===============================
    // ✅ Google Authenticator (TOTP)
    // ===============================

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    // Secret Base32 (ex: "JBSWY3DPEHPK3PXP")
    @Column(name = "two_factor_secret", length = 64)
    private String twoFactorSecret;

    @Column(name = "two_factor_enabled_at")
    private LocalDateTime twoFactorEnabledAt;

    // Constructeurs
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.twoFactorEnabled == null) this.twoFactorEnabled = false;
    }

    public User(String username, String email, String password, String nom, String prenom, Role roleEntity) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.role = roleEntity;
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> auths = new ArrayList<>();
        if (role != null) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + role.getDesignation()));
            if (role.getPermissions() != null) {
                role.getPermissions().stream()
                    .filter(p -> p.getCoderbac() != null && !p.getCoderbac().isBlank())
                    .forEach(p -> auths.add(new SimpleGrantedAuthority(p.getCoderbac())));
            }
        }
        return auths;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getFonction() {
        return fonction;
    }

    public void setFonction(String fonction) {
        this.fonction = fonction;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ===============================
    // ✅ Getters/Setters TOTP
    // ===============================

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }

    public LocalDateTime getTwoFactorEnabledAt() {
        return twoFactorEnabledAt;
    }

    public void setTwoFactorEnabledAt(LocalDateTime twoFactorEnabledAt) {
        this.twoFactorEnabledAt = twoFactorEnabledAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();

        if (this.active == null) {
            this.active = true;
        }
        if (this.twoFactorEnabled == null) {
            this.twoFactorEnabled = false;
        }
    }
}
