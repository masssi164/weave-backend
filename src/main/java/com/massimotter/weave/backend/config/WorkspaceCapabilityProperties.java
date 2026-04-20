package com.massimotter.weave.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weave.workspace")
public record WorkspaceCapabilityProperties(
        Capability shellAccess,
        Capability chat,
        Capability files,
        Capability calendar,
        Capability boards) {

    public WorkspaceCapabilityProperties {
        shellAccess = defaultCapability(shellAccess, true, null);
        chat = defaultCapability(chat, true, null);
        files = defaultCapability(files, true, null);
        calendar = defaultCapability(calendar, false, null);
        boards = defaultCapability(boards, false, null);
    }

    private static Capability defaultCapability(Capability capability, boolean enabled, String dependencyUrl) {
        if (capability == null) {
            return new Capability(enabled, dependencyUrl);
        }
        return new Capability(capability.enabled(), capability.dependencyUrl());
    }

    public record Capability(boolean enabled, String dependencyUrl) {
    }
}
