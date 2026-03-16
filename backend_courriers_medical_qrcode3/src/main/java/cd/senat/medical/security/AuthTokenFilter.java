package cd.senat.medical.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import cd.senat.medical.service.UserDetailsServiceImpl;
import java.io.IOException;

// @Component  // inutile si le bean est créé dans WebSecurityConfig
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return HttpMethod.OPTIONS.matches(request.getMethod())
            || PATH_MATCHER.match("/api/auth/**", path)
            || PATH_MATCHER.match("/api/health", path) || PATH_MATCHER.match("/health", path)
            || PATH_MATCHER.match("/v3/api-docs/**", path)
            || PATH_MATCHER.match("/swagger-ui/**", path)
            || PATH_MATCHER.match("/swagger-ui.html", path)
            || PATH_MATCHER.match("/", path)
            || PATH_MATCHER.match("/assets/**", path)
            || PATH_MATCHER.match("/images/**", path)
            || PATH_MATCHER.match("/static/**", path);
        // ⚠️ Plus d’exclusion pour /api/agents : le filtre DOIT passer.
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String jwt = resolveToken(request);
                if (StringUtils.hasText(jwt) && jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    var auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            logger.error("JWT filter error: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth)) {
            String value = headerAuth.trim();
            if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return value.substring(7).trim();
            }
        }
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("jwt".equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                    return c.getValue().trim();
                }
            }
        }
        return null;
    }
}
