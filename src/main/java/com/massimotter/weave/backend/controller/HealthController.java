package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness;
import com.massimotter.weave.backend.service.WorkspaceCapabilityService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final WorkspaceCapabilityService workspaceCapabilityService;

    public HealthController(WorkspaceCapabilityService workspaceCapabilityService) {
        this.workspaceCapabilityService = workspaceCapabilityService;
    }

    @GetMapping("/api/health/live")
    public Map<String, String> live() {
        return Map.of("status", "up");
    }

    @GetMapping("/api/health/ready")
    public ResponseEntity<Map<String, String>> ready() {
        WorkspaceCapabilityReadiness readiness = workspaceCapabilityService.snapshot().shellAccess().readiness();
        if (readiness == WorkspaceCapabilityReadiness.READY) {
            return ResponseEntity.ok(Map.of("status", "up"));
        }
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "blocked", "reason", "auth_contract_incomplete"));
    }
}
