package com.projeto.workflowengine.core.action;

import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Parâmetros: user, message (aceitam {{variáveis}}). */
@Slf4j
@Component
public class NotificationAction implements Action {

    @Override
    public void execute(ExecutionContext context) {
        Map<String, Object> params = ActionSupport.params(context);
        String user = ActionSupport.render(ActionSupport.string(params, "user", "Usuário"), context);
        String message = ActionSupport.render(
                ActionSupport.string(params, "message", "O workflow foi processado com sucesso!"), context);

        log.info("[NOTIFICATION] Alerta para {}: {}", user, message);
        context.put("last_notification", message);
    }

    @Override
    public ActionType getType() {
        return ActionType.NOTIFICATION;
    }
}
