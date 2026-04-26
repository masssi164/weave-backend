package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.exception.ApiErrorException;
import com.massimotter.weave.backend.model.calendar.CalendarEventResponse;
import com.massimotter.weave.backend.model.calendar.CalendarEventsResponse;
import com.massimotter.weave.backend.model.calendar.CreateCalendarEventRequest;
import com.massimotter.weave.backend.model.calendar.UpdateCalendarEventRequest;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CalendarFacadeService {

    public CalendarEventsResponse list(OffsetDateTime from, OffsetDateTime to) {
        throw adapterNotConfigured("list-events");
    }

    public CalendarEventResponse create(CreateCalendarEventRequest request) {
        throw adapterNotConfigured("create-event");
    }

    public CalendarEventResponse update(String id, UpdateCalendarEventRequest request) {
        throw adapterNotConfigured("update-event");
    }

    public void delete(String id) {
        throw adapterNotConfigured("delete-event");
    }

    private ApiErrorException adapterNotConfigured(String operation) {
        return new ApiErrorException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "nextcloud-adapter-not-configured",
                "Calendar facade is available, but the downstream Nextcloud adapter is not configured yet.",
                Map.of("module", "calendar", "operation", operation));
    }
}
