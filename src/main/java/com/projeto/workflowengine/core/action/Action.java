
package com.projeto.workflowengine.core.action;

import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.ActionType;

public interface Action {
    void execute(ExecutionContext context);
    ActionType getType();
}

