package com.projeto.workflowengine.core.trigger;

import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.TriggerType;

public interface Trigger {
    boolean evaluate(ExecutionContext context);
    TriggerType getType();
}
