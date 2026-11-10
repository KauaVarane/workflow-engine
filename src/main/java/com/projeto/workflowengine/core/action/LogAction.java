package com.projeto.workflowengine.core.action;

import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Parâmetros: message (aceita {{variáveis}}). */
@Slf4j
@Component
public class LogAction implements Action {

    @Override
    public void execute(ExecutionContext context) {
        Map<String, Object> params = ActionSupport.params(context);
        String message = ActionSupport.render(
                ActionSupport.string(params, "message", "Log padrão do workflow (sem mensagem configurada)"), context);

        log.info("[WORKFLOW LOG] {}", message);
        context.put("last_log_message", message);
    }

    @Override
    public ActionType getType() {
        return ActionType.LOG;
    }
}
