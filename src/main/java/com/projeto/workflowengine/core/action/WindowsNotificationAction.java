package com.projeto.workflowengine.core.action;

import com.projeto.workflowengine.config.WorkflowProperties;
import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Simula a fila de notificações do sistema gravando em notifications.log (dentro da pasta de saída).
 * Parâmetros: title, message (aceitam {{variáveis}}).
 */
@Slf4j
@Component
public class WindowsNotificationAction implements Action {

    private final WorkflowProperties properties;

    public WindowsNotificationAction(WorkflowProperties properties) {
        this.properties = properties;
    }

    @Override
    public void execute(ExecutionContext context) {
        Map<String, Object> params = ActionSupport.params(context);
        String title = ActionSupport.render(ActionSupport.string(params, "title", "Alerta do Sistema"), context);
        String message = ActionSupport.render(ActionSupport.string(params, "message", "O workflow foi executado!"), context);

        try {
            Path file = ActionSupport.sandboxPath(properties.outputDir(), "notifications.log");
            String line = "[" + LocalDateTime.now() + "] [" + title + "] " + message + System.lineSeparator();
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("[NOTIFICATION] {} - {}", title, message);
            context.put("last_notification", message);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar a notificação: " + e.getMessage(), e);
        }
    }

    @Override
    public ActionType getType() {
        return ActionType.WINDOWS_NOTIFICATION;
    }
}
