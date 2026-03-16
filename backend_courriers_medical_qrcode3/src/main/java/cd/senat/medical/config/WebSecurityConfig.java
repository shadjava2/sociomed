package cd.senat.medical.config;

import cd.senat.medical.security.AuthEntryPointJwt;
import cd.senat.medical.security.AuthTokenFilter;
import cd.senat.medical.security.JwtUtils;
import cd.senat.medical.service.UserDetailsServiceImpl;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfig {

  private final CorsConfigurationSource corsConfigurationSource;

  /* ---------- Beans de base ---------- */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider(
      UserDetailsServiceImpl userDetailsService,
      PasswordEncoder passwordEncoder
  ) {
    var p = new DaoAuthenticationProvider();
    p.setUserDetailsService(userDetailsService);
    p.setPasswordEncoder(passwordEncoder);
    return p;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  @Bean
  public AuthTokenFilter authTokenFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
    return new AuthTokenFilter(jwtUtils, userDetailsService);
  }

  /* ---------- Règles de sécurité ---------- */
  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      AuthEntryPointJwt unauthorizedHandler,
      AuthTokenFilter authTokenFilter,
      DaoAuthenticationProvider authProvider
  ) throws Exception {

    http
      .cors(c -> c.configurationSource(corsConfigurationSource))
      .csrf(csrf -> csrf.disable())
      .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

      // ✅ Headers de sécurité (n'impacte pas tes APIs)
      .headers(h -> h
          .frameOptions(f -> f.sameOrigin())
          // Désactive le sniffing MIME
          .contentTypeOptions(cto -> {})
          // Politique de referrer plus safe
          .referrerPolicy(r -> r.policy(
              org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
          ))
      )

      .authorizeHttpRequests(auth -> auth

          // ✅ IMPORTANT: ne pas sécuriser les forwards/errors internes
          .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
          .requestMatchers("/error").permitAll()

          // Preflight
          .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

          // ✅ Public PEC (QR: verify/pdf/page)
          .requestMatchers(HttpMethod.GET, "/api/pec/public/**").permitAll()

          // ✅ Frontend public (Option B: /public/pec/:token) si servi par Spring
          .requestMatchers(HttpMethod.GET, "/public/**").permitAll()

          // Static (React build / assets)
          .requestMatchers(
              "/", "/index.html", "/favicon.ico", "/manifest.json",
              "/assets/**", "/images/**", "/static/**"
          ).permitAll()

          // Swagger
          .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

          // ✅ Auth: login/logout/public
          .requestMatchers("/api/auth/**").permitAll()

          // ✅ 2FA: protégé (JWT requis)
          .requestMatchers("/api/2fa/**").authenticated()

          // Health / autres publics
          .requestMatchers("/api/constitution-chat", "/api/health").permitAll()

          // Fichiers publics existants (inchangé)
          .requestMatchers(HttpMethod.GET,
              "/api/files/view/**",
              "/api/files/download/**",
              "/api/courriers/*/annexes/view/**",
              "/api/courriers/*/annexes/download/**"
          ).permitAll()

          // ====== MODULE MÉDICAL ======

          // Photos publiques (agents/sénateurs/conjoints/enfants)
          .requestMatchers(HttpMethod.GET, "/api/agents/photos/**").permitAll()
          .requestMatchers(HttpMethod.GET, "/api/senateurs/photos/**").permitAll()
          .requestMatchers(HttpMethod.GET, "/api/conjoints/photos/**").permitAll()
          .requestMatchers(HttpMethod.GET, "/api/enfants/photos/**").permitAll()

          // ENFANTS
          .requestMatchers(HttpMethod.GET, "/api/enfants/**").authenticated()
          .requestMatchers(HttpMethod.GET, "/api/agents/*/enfants").authenticated()
          .requestMatchers(HttpMethod.GET, "/api/senateurs/*/enfants").authenticated()
          .requestMatchers(HttpMethod.POST, "/api/agents/*/enfants").authenticated()
          .requestMatchers(HttpMethod.POST, "/api/senateurs/*/enfants").authenticated()
          .requestMatchers(HttpMethod.PUT,  "/api/enfants/**").authenticated()
          .requestMatchers(HttpMethod.DELETE,"/api/enfants/**").authenticated()

          // CONJOINTS
          .requestMatchers(HttpMethod.GET, "/api/conjoints/**").authenticated()
          .requestMatchers(HttpMethod.POST, "/api/agents/*/conjoint").authenticated()
          .requestMatchers(HttpMethod.POST, "/api/senateurs/*/conjoint").authenticated()
          .requestMatchers(HttpMethod.PUT,  "/api/conjoints/**").authenticated()
          .requestMatchers(HttpMethod.DELETE,"/api/conjoints/**").authenticated()

          // AGENTS
          .requestMatchers(HttpMethod.GET, "/api/agents").permitAll()
          .requestMatchers(HttpMethod.GET, "/api/agents/*").permitAll()
          .requestMatchers(HttpMethod.GET, "/api/agents/*/details").permitAll()
          .requestMatchers(HttpMethod.POST,   "/api/agents").authenticated()
          .requestMatchers(HttpMethod.POST,   "/api/agents/**").authenticated()
          .requestMatchers(HttpMethod.PUT,    "/api/agents/**").authenticated()
          .requestMatchers(HttpMethod.DELETE, "/api/agents/**").authenticated()

          // PEC
          .requestMatchers(HttpMethod.GET, "/api/pec/*/print").authenticated()
          .requestMatchers(HttpMethod.POST, "/api/pec/**").authenticated()
          .requestMatchers(HttpMethod.PUT,  "/api/pec/**").authenticated()
          .requestMatchers(HttpMethod.DELETE,"/api/pec/**").authenticated()

          // Utilisateurs, rôles, permissions : tout authentifié
          .requestMatchers("/api/users/**").authenticated()
          .requestMatchers("/api/roles/**").authenticated()
          .requestMatchers("/api/permissions/**").authenticated()

          // Le reste : authentifié
          .anyRequest().authenticated()
      );

    http.authenticationProvider(authProvider);
    http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
