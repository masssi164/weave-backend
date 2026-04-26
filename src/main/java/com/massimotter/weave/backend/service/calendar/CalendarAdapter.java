package com.massimotter.weave.backend.service.calendar;

import com.massimotter.weave.backend.model.calendar.CalendarEventResponse;
import com.massimotter.weave.backend.model.calendar.CreateCalendarEventRequest;
import com.massimotter.weave.backend.model.calendar.UpdateCalendarEventRequest;
import java.time.OffsetDateTime;
import java.util.List;

public interface CalendarAdapter {

    List<CalendarEventResponse> list(CalendarPrincipal principal, OffsetDateTime from, OffsetDateTime to)
            throws CalendarAdapterException;

    CalendarEventResponse create(CalendarPrincipal principal, CreateCalendarEventRequest request)
            throws CalendarAdapterException;

    CalendarEventResponse update(CalendarPrincipal principal, String id, UpdateCalendarEventRequest request)
            throws CalendarAdapterException;

    void delete(CalendarPrincipal principal, String id) throws CalendarAdapterException;
}
