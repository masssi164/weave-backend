package com.massimotter.weave.backend;

import com.massimotter.weave.backend.config.NextcloudFilesProperties;
import com.massimotter.weave.backend.config.PlatformContractProperties;
import com.massimotter.weave.backend.config.WeaveSecurityProperties;
import com.massimotter.weave.backend.config.WorkspaceCapabilityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        NextcloudFilesProperties.class,
        PlatformContractProperties.class,
        WeaveSecurityProperties.class,
        WorkspaceCapabilityProperties.class
})
public class WeaveBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeaveBackendApplication.class, args);
    }
}
