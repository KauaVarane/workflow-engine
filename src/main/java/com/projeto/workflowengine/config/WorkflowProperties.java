package com.projeto.workflowengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configurações do motor (prefixo "workflow." no application.properties).
 */
@ConfigurationProperties(prefix = "workflow")
public record WorkflowProperties(
        @DefaultValue("./workflow-output") String outputDir,
        @DefaultValue("true") boolean databaseActionEnabled,
        @DefaultValue("true") boolean schedulerEnabled,
        @DefaultValue("500") int maxStepsPerExecution,
        @DefaultValue("false") boolean seedDemoData) {
}
