package com.projeto.workflowengine.dto;

import com.projeto.workflowengine.core.entity.enums.TriggerType;
import lombok.Data;
import java.util.List;

@Data
public class WorkflowUpdateDTO {
    private String name;
    private TriggerType triggerType;
}
