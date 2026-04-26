package com.massimotter.weave.backend.model.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Calendar facade event listing response.")
public record CalendarEventsResponse(List<CalendarEventResponse> events) {
}
