package com.projeto.workflowengine.core;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Memória compartilhada entre os passos de UMA execução.
 * Cada ação lê e escreve aqui (ex.: HTTP guarda a resposta; DECISION lê uma variável).
 */
@Getter
public class ExecutionContext {

    /** Parâmetros (JSON) do passo que está sendo executado agora. */
    public static final String PARAMS_KEY = "current_action_parameters";

    /** Preenchido pela DECISION: próximo passo (1-based) ou -1 para encerrar o fluxo. */
    public static final String NEXT_STEP_KEY = "__next_step";

    private final Map<String, Object> data = new LinkedHashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public void remove(String key) {
        data.remove(key);
    }

    @Override
    public String toString() {
        return "ExecutionContext{data=" + data + "}";
    }
}
