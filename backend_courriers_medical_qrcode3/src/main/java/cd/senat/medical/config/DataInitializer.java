package cd.senat.medical.config;

import cd.senat.medical.entity.Permission;
import cd.senat.medical.entity.Role;
import cd.senat.medical.entity.User;
import cd.senat.medical.repository.PermissionRepository;
import cd.senat.medical.repository.RoleRepository;
import cd.senat.medical.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1) Permissions RBAC (menus + actions)
        List<String[]> permissionData = List.of(
            // Menus
            new String[]{"MENU_AGENTS", "Voir le menu Agents"},
            new String[]{"MENU_SENATEURS", "Voir le menu Sénateurs"},
            new String[]{"MENU_USERS", "Voir le menu Utilisateurs"},
            new String[]{"MENU_ROLES", "Voir le menu Rôles et droits"},
            new String[]{"MENU_HOPITAUX", "Voir le menu Hôpitaux"},
            new String[]{"MENU_PEC", "Voir le menu Prises en charge"},
            new String[]{"MENU_DASHBOARD", "Voir le menu Tableau de bord"},
            new String[]{"MENU_PARAMETRES", "Voir le menu Paramètres"},
            // Agents
            new String[]{"AGENT_CREATE", "Bouton Nouvel agent"},
            new String[]{"AGENT_VIEW", "Voir détail agent"},
            new String[]{"AGENT_PEC_CREATE", "Nouvelle prise en charge (agent)"},
            new String[]{"AGENT_EDIT", "Modifier agent"},
            new String[]{"AGENT_DELETE", "Supprimer agent"},
            // Conjoint
            new String[]{"CONJOINT_ADD", "Ajouter conjoint"},
            new String[]{"CONJOINT_PEC", "PEC conjoint"},
            new String[]{"CONJOINT_EDIT", "Modifier conjoint"},
            new String[]{"CONJOINT_DELETE", "Supprimer conjoint"},
            // Enfants
            new String[]{"ENFANT_ADD", "Ajouter enfant"},
            new String[]{"ENFANT_PEC", "PEC enfant"},
            new String[]{"ENFANT_EDIT", "Modifier enfant"},
            new String[]{"ENFANT_DELETE", "Supprimer enfant"},
            // Sénateurs
            new String[]{"SENATEUR_VIEW", "Voir sénateur"},
            new String[]{"SENATEUR_CREATE", "Créer sénateur"},
            new String[]{"SENATEUR_EDIT", "Modifier sénateur"},
            new String[]{"SENATEUR_DELETE", "Supprimer sénateur"},
            new String[]{"SENATEUR_PEC", "PEC sénateur"},
            // Utilisateurs
            new String[]{"USERS_READ", "Voir utilisateurs"},
            new String[]{"USERS_CREATE", "Créer utilisateur"},
            new String[]{"USERS_EDIT", "Modifier utilisateur"},
            new String[]{"USERS_DELETE", "Supprimer utilisateur"},
            // Rôles et droits
            new String[]{"ROLES_READ", "Voir rôles et droits"},
            new String[]{"ROLES_WRITE", "Créer / modifier rôles et permissions"},
            // Hôpitaux
            new String[]{"HOPITAUX_READ", "Voir hôpitaux"},
            new String[]{"HOPITAUX_CREATE", "Créer hôpital"},
            new String[]{"HOPITAUX_EDIT", "Modifier hôpital"},
            new String[]{"HOPITAUX_DELETE", "Supprimer hôpital"},
            // Prises en charge
            new String[]{"PEC_READ", "Voir PEC"},
            new String[]{"PEC_CREATE", "Créer PEC"},
            new String[]{"PEC_EDIT", "Modifier PEC"},
            new String[]{"PEC_DELETE", "Supprimer PEC"},
            // Tableau de bord & Paramètres
            new String[]{"DASHBOARD_VIEW", "Voir tableau de bord"},
            new String[]{"PARAMETRES_VIEW", "Voir paramètres"}
        );
        for (String[] row : permissionData) {
            String coderbac = row[0];
            String designation = row[1];
            if (!permissionRepository.existsByCoderbac(coderbac)) {
                permissionRepository.save(new Permission(designation, coderbac));
            }
        }
        Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());

        // 2) Rôles : ADMIN et Administrateur ont tous les droits
        for (String roleName : List.of("ADMIN", "Administrateur")) {
            Role r = roleRepository.findByDesignation(roleName).orElse(null);
            if (r == null) {
                r = new Role(roleName);
                r = roleRepository.save(r);
            }
            r = roleRepository.findByIdWithPermissions(r.getId()).orElse(r);
            r.setPermissions(allPermissions);
            roleRepository.save(r);
        }
        roleRepository.findByDesignation("USER").orElseGet(() -> roleRepository.save(new Role("USER")));
        roleRepository.findByDesignation("MANAGER").orElseGet(() -> roleRepository.save(new Role("MANAGER")));

        // 3) Utilisateur admin par défaut (rôle ADMIN)
        Role adminRole = roleRepository.findByDesignation("ADMIN").orElse(null);
        if (adminRole == null) adminRole = roleRepository.findByDesignation("Administrateur").orElse(null);
        if (!userRepository.existsByUsername("admin") && adminRole != null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@senat.fr");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNom("Administrateur");
            admin.setPrenom("Système");
            admin.setFonction("Administrateur Système");
            admin.setRole(adminRole);
            admin.setActive(true);
            userRepository.save(admin);
            System.out.println("Utilisateur admin créé avec succès! Username: admin, Password: admin123");
        }
    }
}
