package com.projeto.workflowengine.core.trigger;

import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.TriggerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScheduleTrigger implements Trigger {
    @Override
    public boolean evaluate(ExecutionContext context) {
        log.info("Validando ScheduleTrigger: Verificando se o horário de execução é válido...");
        return true;
    }

    @Override
    public TriggerType getType() {
        return TriggerType.SCHEDULE;
    }
}
