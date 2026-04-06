package com.massimotter.weave.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces caller binding by validating the {@code azp} (authorized party) claim in JWT tokens.
 *
 * <p>When the allowlist is non-empty, any token whose {@code azp} claim is absent or not in the
 * allowlist is rejected with HTTP 401. When the allowlist is empty, enforcement is disabled for
 * backwards-compatible operation (no allowlist configured → any caller accepted).
 *
 * <p>This filter runs after {@code BearerTokenAuthenticationFilter} so that the token is already
 * decoded and the {@code JwtAuthenticationToken} is available in the {@code SecurityContext}.
 */
class AzpValidationFilter extends OncePerRequestFilter {

    private static final String ERROR_BODY =
            "{\"error\":\"invalid_token\","
            + "\"error_description\":\"The token azp claim is absent or not in the configured allowlist.\"}";

    private final List<String> allowedAzp;

    AzpValidationFilter(List<String> allowedAzp) {
        this.allowedAzp = allowedAzp;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Enforcement is disabled when the allowlist is empty — pass through unchanged.
        if (allowedAzp.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String azp = jwtAuth.getToken().getClaimAsString("azp");
            if (azp == null || !allowedAzp.contains(azp)) {
                rejectRequest(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void rejectRequest(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate",
                "Bearer error=\"invalid_token\","
                + " error_description=\"The token azp claim is absent or not permitted.\"");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ERROR_BODY);
    }
}
