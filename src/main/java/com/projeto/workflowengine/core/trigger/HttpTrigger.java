package com.projeto.workflowengine.core.trigger;

import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.TriggerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpTrigger implements Trigger {
    @Override
    public boolean evaluate(ExecutionContext context) {
        log.info("Validando HttpTrigger: Forçado para TRUE para fins de teste.");
        return true;
    }

    @Override
    public TriggerType getType() {
        return TriggerType.HTTP;
    }
}
