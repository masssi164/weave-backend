package com.massimotter.weave.backend.model.calendar;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Calendar ownership scope exposed by the Weave calendar facade.")
public record CalendarScopeResponse(
        @Schema(description = "Stable scope identifier.", example = "workspace")
        String type,
        @Schema(description = "Human-readable scope label.", example = "Weave workspace calendar")
        String label) {

    public static CalendarScopeResponse workspace() {
        return new CalendarScopeResponse("workspace", "Weave workspace calendar");
    }
}
