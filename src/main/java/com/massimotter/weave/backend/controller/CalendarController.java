package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.model.ApiErrorResponse;
import com.massimotter.weave.backend.model.calendar.CalendarEventResponse;
import com.massimotter.weave.backend.model.calendar.CalendarEventsResponse;
import com.massimotter.weave.backend.model.calendar.CreateCalendarEventRequest;
import com.massimotter.weave.backend.model.calendar.UpdateCalendarEventRequest;
import com.massimotter.weave.backend.service.CalendarFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Calendar", description = "Authenticated product calendar facade backed by Nextcloud CalDAV.")
@SecurityRequirement(name = "bearer-jwt")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token.",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Bearer token is missing the weave:workspace scope.",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Downstream calendar adapter is not configured or unavailable.",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class CalendarController {

    private final CalendarFacadeService calendarFacadeService;

    public CalendarController(CalendarFacadeService calendarFacadeService) {
        this.calendarFacadeService = calendarFacadeService;
    }

    @GetMapping("/api/calendar/events")
    @Operation(summary = "List calendar events")
    @ApiResponse(responseCode = "200", description = "Calendar event listing.",
            content = @Content(schema = @Schema(implementation = CalendarEventsResponse.class)))
    public CalendarEventsResponse list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to) {
        return calendarFacadeService.list(from, to);
    }

    @PostMapping("/api/calendar/events")
    @Operation(summary = "Create a calendar event")
    @ApiResponse(responseCode = "200", description = "Created calendar event.",
            content = @Content(schema = @Schema(implementation = CalendarEventResponse.class)))
    public CalendarEventResponse create(@Valid @RequestBody CreateCalendarEventRequest request) {
        return calendarFacadeService.create(request);
    }

    @PatchMapping("/api/calendar/events/{id}")
    @Operation(summary = "Update a calendar event")
    @ApiResponse(responseCode = "200", description = "Updated calendar event.",
            content = @Content(schema = @Schema(implementation = CalendarEventResponse.class)))
    public CalendarEventResponse update(
            @PathVariable @Size(max = 2048) String id,
            @Valid @RequestBody UpdateCalendarEventRequest request) {
        return calendarFacadeService.update(id, request);
    }

    @DeleteMapping("/api/calendar/events/{id}")
    @Operation(summary = "Delete a calendar event")
    public ResponseEntity<Void> delete(@PathVariable @Size(max = 2048) String id) {
        calendarFacadeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
