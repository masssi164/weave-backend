package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.exception.ApiErrorException;
import com.massimotter.weave.backend.model.calendar.CalendarAccessModelResponse;
import com.massimotter.weave.backend.model.calendar.CalendarAccessPolicyResponse;
import com.massimotter.weave.backend.model.calendar.CalendarClientSetupOptionResponse;
import com.massimotter.weave.backend.model.calendar.CalendarClientSetupResponse;
import com.massimotter.weave.backend.model.calendar.CalendarCredentialReadinessResponse;
import com.massimotter.weave.backend.model.calendar.CalendarExternalEndpointsResponse;
import com.massimotter.weave.backend.model.calendar.CalendarEventResponse;
import com.massimotter.weave.backend.model.calendar.CalendarEventsResponse;
import com.massimotter.weave.backend.model.calendar.CalendarScopeResponse;
import com.massimotter.weave.backend.model.calendar.CalendarSetupCredentialListResponse;
import com.massimotter.weave.backend.model.calendar.CalendarSetupCredentialRequest;
import com.massimotter.weave.backend.model.calendar.CalendarSetupCredentialResponse;
import com.massimotter.weave.backend.model.calendar.CreateCalendarEventRequest;
import com.massimotter.weave.backend.model.calendar.UpdateCalendarEventRequest;
import com.massimotter.weave.backend.service.calendar.CalendarAdapter;
import com.massimotter.weave.backend.service.calendar.CalendarAdapterException;
import com.massimotter.weave.backend.service.calendar.AppleMobileConfigProfile;
import com.massimotter.weave.backend.service.calendar.AppleMobileConfigProfileRenderer;
import com.massimotter.weave.backend.service.calendar.CalendarPrincipal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CalendarFacadeService {

    private final ObjectProvider<CalendarAdapter> calendarAdapterProvider;
    private final String nextcloudBaseUrl;
    private final AppleMobileConfigProfileRenderer appleProfileRenderer;
    private final Map<String, CalendarSetupCredentialResponse> setupCredentials = new ConcurrentHashMap<>();

    public CalendarFacadeService(ObjectProvider<CalendarAdapter> calendarAdapterProvider) {
        this(calendarAdapterProvider, "https://files.weave.local");
    }

    @Autowired
    public CalendarFacadeService(
            ObjectProvider<CalendarAdapter> calendarAdapterProvider,
            @Value("${weave.platform.nextcloud-base-url:https://files.weave.local}") String nextcloudBaseUrl) {
        this.calendarAdapterProvider = calendarAdapterProvider;
        this.nextcloudBaseUrl = nextcloudBaseUrl == null || nextcloudBaseUrl.isBlank()
                ? "https://files.weave.local"
                : nextcloudBaseUrl.trim();
        this.appleProfileRenderer = new AppleMobileConfigProfileRenderer(this.nextcloudBaseUrl);
    }

    public CalendarEventsResponse list(OffsetDateTime from, OffsetDateTime to) {
        validateRange(from, to);
        try {
            return new CalendarEventsResponse(adapter("list-events").list(principal(), from, to));
        } catch (CalendarAdapterException exception) {
            throw apiError(exception, "list-events");
        }
    }

    public CalendarEventResponse create(CreateCalendarEventRequest request) {
        try {
            return adapter("create-event").create(principal(), request);
        } catch (CalendarAdapterException exception) {
            throw apiError(exception, "create-event");
        }
    }

    public CalendarEventResponse read(String id) {
        try {
            return adapter("read-event").read(principal(), id);
        } catch (CalendarAdapterException exception) {
            throw apiError(exception, "read-event");
        }
    }

    public CalendarEventResponse update(String id, UpdateCalendarEventRequest request) {
        try {
            return adapter("update-event").update(principal(), id, request);
        } catch (CalendarAdapterException exception) {
            throw apiError(exception, "update-event");
        }
    }

    public void delete(String id) {
        try {
            adapter("delete-event").delete(principal(), id);
        } catch (CalendarAdapterException exception) {
            throw apiError(exception, "delete-event");
        }
    }

    public CalendarClientSetupResponse clientSetup() {
        CalendarPrincipal principal = principal();
        String username = principal.nextcloudUserId();
        String discoveryUrl = davUrl("remote.php", "dav");
        String principalUrl = davUrl("remote.php", "dav", "principals", "users", username) + "/";
        String davx5Url = davx5Url();

        return new CalendarClientSetupResponse(
                CalendarScopeResponse.workspace(),
                accessModel(),
                credentialReadiness(),
                username,
                new CalendarExternalEndpointsResponse(nextcloudBaseUrl, discoveryUrl, principalUrl),
                "The backend never returns Nextcloud passwords, app passwords, bearer tokens, or static profile secrets. "
                        + "External clients must use a revocable per-client app password/login flow now, or a future "
                        + "Weave-issued scoped setup token once implemented.",
                List.of(
                        new CalendarClientSetupOptionResponse(
                                "apple",
                                "mobileconfig",
                                false,
                                null,
                                "Signed .mobileconfig download remains fail-closed until profile signing and revocable credentials are implemented.",
                                List.of(
                                        "iOS, iPadOS, and macOS can install a CalDAV configuration profile with host, port, SSL, principal URL, and username.",
                                        "Release 2 must not embed a permanent password or backend service credential in the profile.",
                                        "The backend route is reserved for a signed no-secret profile and currently returns 503 rather than serving an unsigned artifact.")),
                        new CalendarClientSetupOptionResponse(
                                "android",
                                "davx5",
                                true,
                                davx5Url,
                                null,
                                List.of(
                                        "Android has no universal native CalDAV account profile equivalent.",
                                        "DAVx5 can open a secret-free davx5:// setup URL and can use the Nextcloud login flow for per-client credentials.",
                                        "Webcal/ICS subscriptions are read-only and should remain a separate fallback.")),
                        new CalendarClientSetupOptionResponse(
                                "desktop",
                                "caldav-manual",
                                true,
                                discoveryUrl,
                                null,
                                List.of(
                                        "Use the CalDAV discovery or principal URL in clients such as Thunderbird, Apple Calendar, GNOME, or KDE calendar apps.",
                                        "Microsoft Outlook generally needs an add-in for CalDAV; read-only ICS/webcal can be offered later where acceptable.",
                                        "Use username " + username + " with a revocable per-client app password/login-flow credential.")),
                        new CalendarClientSetupOptionResponse(
                                "subscription",
                                "webcal-ics",
                                false,
                                null,
                                "A revocable read-only ICS/webcal feed token is not implemented yet.",
                                List.of(
                                        "ICS/webcal is one-way subscription/download, not full two-way CalDAV sync.",
                                        "It is useful for clients without CalDAV support once scoped feed tokens and revocation are available."))));
    }


    public AppleMobileConfigProfile appleMobileConfigProfile() {
        // Keep the download route present but unavailable until a real signing path is wired.
        // The unsigned renderer is covered by tests so the future signer has a no-secret input artifact.
        appleProfileRenderer.renderUnsignedNoSecretProfile(principal());
        throw new ApiErrorException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "calendar-apple-profile-unavailable",
                "Apple Calendar profile download is not available until signed no-secret profile generation is configured.",
                Map.of(
                        "module", "calendar",
                        "operation", "download-apple-mobileconfig",
                        "requiresSignedProfile", true,
                        "passwordIncluded", false,
                        "backendActorCredentialsExposed", false,
                        "blockers", List.of(
                                "Profile signing is not configured or implemented in this backend slice.",
                                "Revocable per-client CalDAV credential issuance remains a prerequisite for password-bearing profiles.",
                                "Unsigned profiles are deliberately not downloadable from the authenticated API.")));
    }

    public CalendarAccessPolicyResponse accessPolicy() {
        return new CalendarAccessPolicyResponse(
                accessModel(),
                List.of("workspace-calendar.read", "workspace-calendar.write", "client-setup.metadata"),
                List.of("private-user-calendar.read", "backend-actor-private-user-calendar.read"),
                List.of(
                        "Choose and document a private calendar access model: user sharing/provisioning, Nextcloud Login Flow/app password, delegated token exchange, or a Weave token bridge.",
                        "Add operator diagnostics proving private calendar templates are explicitly authorized.",
                        "Add revocation and audit tests before any private user CalDAV endpoint is enabled."),
                false);
    }

    public CalendarSetupCredentialListResponse setupCredentials() {
        CalendarPrincipal principal = principal();
        return new CalendarSetupCredentialListResponse(setupCredentials.values().stream()
                .filter(credential -> principal.subject().equals(credential.username()))
                .sorted(java.util.Comparator.comparing(CalendarSetupCredentialResponse::issuedAt))
                .toList());
    }

    public CalendarSetupCredentialResponse createSetupCredential(CalendarSetupCredentialRequest request) {
        CalendarPrincipal principal = principal();
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        String id = "cal_setup_" + UUID.randomUUID();
        CalendarSetupCredentialResponse credential = new CalendarSetupCredentialResponse(
                id,
                "active-no-secret-issued",
                principal.subject(),
                defaultIfBlank(request.clientType(), "caldav"),
                defaultIfBlank(request.label(), "Calendar client setup"),
                issuedAt,
                issuedAt.plusHours(24),
                null,
                false,
                false,
                List.of("DELETE /api/calendar/client-setup/credentials/" + id));
        setupCredentials.put(id, credential);
        return credential;
    }

    public CalendarSetupCredentialResponse revokeSetupCredential(String credentialId) {
        CalendarPrincipal principal = principal();
        CalendarSetupCredentialResponse current = setupCredentials.get(credentialId);
        if (current == null || !principal.subject().equals(current.username())) {
            throw new ApiErrorException(
                    HttpStatus.NOT_FOUND,
                    "calendar-setup-credential-not-found",
                    "Calendar setup credential was not found.",
                    Map.of("module", "calendar", "operation", "revoke-setup-credential"));
        }
        CalendarSetupCredentialResponse revoked = new CalendarSetupCredentialResponse(
                current.credentialId(),
                "revoked",
                current.username(),
                current.clientType(),
                current.label(),
                current.issuedAt(),
                current.expiresAt(),
                OffsetDateTime.now(ZoneOffset.UTC),
                false,
                false,
                List.of());
        setupCredentials.put(credentialId, revoked);
        return revoked;
    }

    private CalendarAccessModelResponse accessModel() {
        return new CalendarAccessModelResponse(
                "workspace-calendar",
                "workspace",
                false,
                "Private per-user CalDAV calendars are not exposed until provisioning, sharing, or delegated-token access is specified and tested.",
                "external clients use user-owned revocable per-client credentials; backend actor credentials are never issued to clients",
                List.of(
                        "The product calendar facade currently serves the Weave-managed workspace calendar scope.",
                        "Backend CalDAV configuration that targets arbitrary private user calendars must stay fail-closed.",
                        "External clients may use CalDAV discovery URLs, but credentials must come from a user-controlled revocable flow."));
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private CalendarCredentialReadinessResponse credentialReadiness() {
        return new CalendarCredentialReadinessResponse(
                "blocked_until_revocable_credentials",
                false,
                false,
                false,
                false,
                false,
                List.of(
                        "Signed Apple .mobileconfig generation is not implemented yet.",
                        "Weave-issued revocable per-client CalDAV credentials are not implemented yet.",
                        "Read-only ICS/webcal feed tokens are not implemented yet."));
    }

    private CalendarAdapter adapter(String operation) {
        CalendarAdapter adapter = calendarAdapterProvider.getIfAvailable();
        if (adapter == null) {
            throw adapterNotConfigured(operation);
        }
        return adapter;
    }

    private CalendarPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ApiErrorException(
                    HttpStatus.UNAUTHORIZED,
                    "unauthorized",
                    "Authentication is required.",
                    Map.of("module", "calendar"));
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String nextcloudUserId = firstNonBlank(jwt.getClaimAsString("preferred_username"), jwt.getSubject());
            return new CalendarPrincipal(jwt.getSubject(), nextcloudUserId);
        }
        return new CalendarPrincipal(authentication.getName(), authentication.getName());
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new ApiErrorException(
                    HttpStatus.BAD_REQUEST,
                    "validation-error",
                    "Request validation failed.",
                    Map.of("fields", Map.of("to", "to must be after from")));
        }
    }

    private ApiErrorException apiError(CalendarAdapterException exception, String fallbackOperation) {
        Map<String, Object> details = withDefaultDetails(exception.details(), fallbackOperation);
        return switch (exception.type()) {
            case NOT_CONFIGURED -> new ApiErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "nextcloud-adapter-not-configured",
                    "Calendar facade is available, but the downstream Nextcloud adapter is not configured yet.",
                    details);
            case INVALID_REQUEST -> new ApiErrorException(
                    HttpStatus.BAD_REQUEST,
                    "invalid-calendar-event-id",
                    exception.getMessage(),
                    details);
            case AUTH_FAILED -> new ApiErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "nextcloud-calendar-auth-failed",
                    "Calendar storage is unavailable because the backend actor is not authorized.",
                    details);
            case NOT_FOUND -> new ApiErrorException(
                    HttpStatus.NOT_FOUND,
                    "calendar-event-not-found",
                    "Calendar event was not found.",
                    details);
            case CONFLICT -> new ApiErrorException(
                    HttpStatus.CONFLICT,
                    "calendar-event-conflict",
                    "Calendar event changed in storage. Refresh and try again.",
                    details);
            case DOWNSTREAM_UNAVAILABLE, INVALID_RESPONSE -> new ApiErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "nextcloud-calendar-unavailable",
                    "Calendar storage is currently unavailable.",
                    details);
        };
    }

    private ApiErrorException adapterNotConfigured(String operation) {
        return new ApiErrorException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "nextcloud-adapter-not-configured",
                "Calendar facade is available, but the downstream Nextcloud adapter is not configured yet.",
                Map.of("module", "calendar", "operation", operation));
    }

    private Map<String, Object> withDefaultDetails(Map<String, Object> details, String operation) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("module", "calendar");
        merged.put("operation", operation);
        merged.putAll(details);
        return merged;
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private String davUrl(String... pathSegments) {
        return UriComponentsBuilder.fromUriString(nextcloudBaseUrl)
                .pathSegment(pathSegments)
                .build()
                .toUriString();
    }

    private String davx5Url() {
        URI uri = URI.create(nextcloudBaseUrl);
        String host = uri.getHost() == null ? "files.weave.local" : uri.getHost();
        int port = uri.getPort();
        String authority = port > 0 ? host + ":" + port : host;
        return "davx5://" + authority + "/remote.php/dav";
    }
}
