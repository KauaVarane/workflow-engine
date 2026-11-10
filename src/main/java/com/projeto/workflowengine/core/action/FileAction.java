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
import java.util.Map;

/**
 * Grava um arquivo de texto SOMENTE dentro de workflow.output-dir.
 * Parâmetros: fileName (ou path), content, append (true/false). Aceita {{variáveis}}.
 */
@Slf4j
@Component
public class FileAction implements Action {

    private final WorkflowProperties properties;

    public FileAction(WorkflowProperties properties) {
        this.properties = properties;
    }

    @Override
    public void execute(ExecutionContext context) {
        Map<String, Object> params = ActionSupport.params(context);
        String fileName = ActionSupport.render(
                ActionSupport.string(params, "fileName", ActionSupport.string(params, "path", "workflow_output.txt")), context);
        String content = ActionSupport.render(
                ActionSupport.string(params, "content", "Arquivo gerado pelo Workflow Engine!"), context);
        boolean append = Boolean.parseBoolean(String.valueOf(params.getOrDefault("append", "false")));

        try {
            Path target = ActionSupport.sandboxPath(properties.outputDir(), fileName);
            if (append) {
                Files.writeString(target, content + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(target, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
            log.info("[FILE] Arquivo gravado em: {}", target);
            context.put("last_file_created", target.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar o arquivo: " + e.getMessage(), e);
        }
    }

    @Override
    public ActionType getType() {
        return ActionType.FILE;
    }
}
