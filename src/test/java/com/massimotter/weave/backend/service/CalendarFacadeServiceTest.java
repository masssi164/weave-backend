package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.exception.ApiErrorException;
import com.massimotter.weave.backend.model.calendar.CalendarEventResponse;
import com.massimotter.weave.backend.model.calendar.CreateCalendarEventRequest;
import com.massimotter.weave.backend.service.calendar.CalendarAdapter;
import com.massimotter.weave.backend.service.calendar.CalendarAdapterException;
import com.massimotter.weave.backend.service.calendar.CalendarPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarFacadeServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void delegatesListToAdapterWithNextcloudUserClaim() {
        AtomicReference<CalendarPrincipal> capturedPrincipal = new AtomicReference<>();
        OffsetDateTime startsAt = OffsetDateTime.parse("2026-04-26T10:00:00+02:00");
        OffsetDateTime endsAt = OffsetDateTime.parse("2026-04-26T11:00:00+02:00");
        CalendarEventResponse event = new CalendarEventResponse(
                "event-id", "Planning", null, startsAt, endsAt, "Europe/Berlin", null, false, "etag");
        CalendarAdapter adapter = new StubCalendarAdapter() {
            @Override
            public List<CalendarEventResponse> list(CalendarPrincipal principal, OffsetDateTime from, OffsetDateTime to) {
                capturedPrincipal.set(principal);
                return List.of(event);
            }
        };
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt(), null));

        var response = service(adapter).list(startsAt.minusDays(1), endsAt.plusDays(1));

        assertThat(response.events()).containsExactly(event);
        assertThat(capturedPrincipal.get().subject()).isEqualTo("user-123");
        assertThat(capturedPrincipal.get().nextcloudUserId()).isEqualTo("massimo");
    }

    @Test
    void rejectsInvalidListRangeBeforeCallingAdapter() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-04-26T10:00:00+02:00");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt(), null));

        assertThatThrownBy(() -> service(new StubCalendarAdapter()).list(timestamp, timestamp))
                .isInstanceOf(ApiErrorException.class)
                .extracting("code")
                .isEqualTo("validation-error");
    }

    @Test
    void mapsAdapterConflictToStableApiError() {
        CalendarAdapter adapter = new StubCalendarAdapter() {
            @Override
            public CalendarEventResponse create(CalendarPrincipal principal, CreateCalendarEventRequest request) {
                throw new CalendarAdapterException(CalendarAdapterException.Type.CONFLICT, "conflict");
            }
        };
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt(), null));
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "Planning",
                null,
                OffsetDateTime.parse("2026-04-26T10:00:00+02:00"),
                OffsetDateTime.parse("2026-04-26T11:00:00+02:00"),
                "Europe/Berlin",
                null,
                false);

        assertThatThrownBy(() -> service(adapter).create(request))
                .isInstanceOf(ApiErrorException.class)
                .satisfies(error -> {
                    ApiErrorException apiError = (ApiErrorException) error;
                    assertThat(apiError.status().value()).isEqualTo(409);
                    assertThat(apiError.code()).isEqualTo("calendar-event-conflict");
                    assertThat(apiError.details()).containsEntry("module", "calendar");
                    assertThat(apiError.details()).containsEntry("operation", "create-event");
                });
    }

    private CalendarFacadeService service(CalendarAdapter adapter) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("calendarAdapter", adapter);
        return new CalendarFacadeService(beanFactory.getBeanProvider(CalendarAdapter.class));
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .claim("preferred_username", "massimo")
                .build();
    }

    private static class StubCalendarAdapter implements CalendarAdapter {
        @Override
        public List<CalendarEventResponse> list(CalendarPrincipal principal, OffsetDateTime from, OffsetDateTime to) {
            throw new AssertionError("unexpected list call");
        }

        @Override
        public CalendarEventResponse create(CalendarPrincipal principal, CreateCalendarEventRequest request) {
            throw new AssertionError("unexpected create call");
        }

        @Override
        public CalendarEventResponse update(com.massimotter.weave.backend.service.calendar.CalendarPrincipal principal,
                String id, com.massimotter.weave.backend.model.calendar.UpdateCalendarEventRequest request) {
            throw new AssertionError("unexpected update call");
        }

        @Override
        public void delete(CalendarPrincipal principal, String id) {
            throw new AssertionError("unexpected delete call");
        }
    }
}
