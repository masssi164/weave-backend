package com.massimotter.weave.backend.config;

import java.util.List;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(
            OAuth2ResourceServerProperties resourceServerProperties,
            WeaveSecurityProperties weaveSecurityProperties) {
        String issuerUri = resourceServerProperties.getJwt().getIssuerUri();
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuerUri);
        if (weaveSecurityProperties.hasRequiredAudience()) {
            validator = new DelegatingOAuth2TokenValidator<>(
                    validator,
                    jwt -> hasRequiredAudience(jwt, weaveSecurityProperties.requiredAudience().trim()));
        }

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    private OAuth2TokenValidatorResult hasRequiredAudience(Jwt jwt, String requiredAudience) {
        List<String> audiences = jwt.getAudience();
        if (audiences != null && audiences.contains(requiredAudience)) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(
                error("invalid_token",
                        "The token is missing the required audience '" + requiredAudience + "'."));
    }

    private org.springframework.security.oauth2.core.OAuth2Error error(String code, String description) {
        return new org.springframework.security.oauth2.core.OAuth2Error(code, description, null);
    }
}
