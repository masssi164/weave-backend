package com.massimotter.weave.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.massimotter.weave.backend.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        write(request, response, status, defaultCode(status), message);
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code,
            String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String requestId = requestId(request);
        response.setHeader("X-Request-Id", requestId);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", status.value());
        details.put("path", request.getRequestURI());
        details.put("error", status.getReasonPhrase());
        objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                code,
                message,
                details,
                requestId));
    }

    private String defaultCode(HttpStatus status) {
        return status.name().toLowerCase().replace('_', '-');
    }

    private String requestId(HttpServletRequest request) {
        String incoming = request.getHeader("X-Request-Id");
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }
}
