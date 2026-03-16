package cd.senat.medical.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);
    
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String authHeader = request.getHeader("Authorization");

        logger.error("Unauthorized error: {} | {} {} | Authorization header present? {}",
                authException.getMessage(),
                method,
                uri,
                (authHeader != null && !authHeader.isBlank()));

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());
        body.put("uri", uri);
        body.put("method", method);

        // ⚠️ On ne renvoie pas le token au client (sécurité), juste un bool
        body.put("hasAuthorizationHeader", (authHeader != null && !authHeader.isBlank()));

        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }

}