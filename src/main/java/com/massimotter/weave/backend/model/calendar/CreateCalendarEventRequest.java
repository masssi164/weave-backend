package com.massimotter.weave.backend.model.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

@Schema(description = "Create a calendar event through the product calendar facade.")
public record CreateCalendarEventRequest(
        @Schema(description = "Event title.", example = "Planning")
        @NotBlank
        @Size(max = 255)
        String title,
        @Schema(description = "Plain-text event description.")
        @Size(max = 8192)
        String description,
        @Schema(description = "Event start timestamp.")
        @NotNull
        OffsetDateTime startsAt,
        @Schema(description = "Event end timestamp.")
        @NotNull
        OffsetDateTime endsAt,
        @Schema(description = "IANA timezone used for display and editing.", example = "Europe/Berlin")
        @NotBlank
        @Size(max = 128)
        String timezone,
        @Schema(description = "Event location.")
        @Size(max = 1024)
        String location,
        @Schema(description = "Whether the event is all-day.")
        boolean allDay) {

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        if (startsAt == null || endsAt == null) {
            return true;
        }
        return endsAt.isAfter(startsAt);
    }
}
