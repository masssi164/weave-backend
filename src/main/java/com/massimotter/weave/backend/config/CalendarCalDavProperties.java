package com.massimotter.weave.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weave.calendar.caldav")
public record CalendarCalDavProperties(
        String baseUrl,
        String calendarPathTemplate,
        AuthMode authMode,
        String backendUsername,
        String backendToken,
        int requestTimeoutSeconds) {

    public CalendarCalDavProperties {
        baseUrl = trimToNull(baseUrl);
        calendarPathTemplate = defaultIfBlank(calendarPathTemplate,
                "/remote.php/dav/calendars/{user}/personal/");
        authMode = authMode == null ? AuthMode.BASIC : authMode;
        backendUsername = trimToNull(backendUsername);
        backendToken = trimToNull(backendToken);
        requestTimeoutSeconds = requestTimeoutSeconds <= 0 ? 10 : requestTimeoutSeconds;
    }

    public boolean isConfigured() {
        if (baseUrl == null || !calendarPathTemplate.contains("{user}")) {
            return false;
        }
        if (authMode == AuthMode.BEARER) {
            return backendToken != null;
        }
        return backendUsername != null && backendToken != null;
    }

    public enum AuthMode {
        BASIC,
        BEARER
    }

    private static String defaultIfBlank(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
