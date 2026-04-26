package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.exception.ApiErrorException;
import com.massimotter.weave.backend.model.calendar.CalendarEventResponse;
import com.massimotter.weave.backend.model.calendar.CalendarEventsResponse;
import com.massimotter.weave.backend.model.calendar.CreateCalendarEventRequest;
import com.massimotter.weave.backend.model.calendar.UpdateCalendarEventRequest;
import com.massimotter.weave.backend.service.calendar.CalendarAdapter;
import com.massimotter.weave.backend.service.calendar.CalendarAdapterException;
import com.massimotter.weave.backend.service.calendar.CalendarPrincipal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CalendarFacadeService {

    private final ObjectProvider<CalendarAdapter> calendarAdapterProvider;

    public CalendarFacadeService(ObjectProvider<CalendarAdapter> calendarAdapterProvider) {
        this.calendarAdapterProvider = calendarAdapterProvider;
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
}
