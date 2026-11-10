package com.projeto.workflowengine.core.action;

import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Ramificação do fluxo.
 * Parâmetros:
 *  - variable: variável do contexto (aceita caminho, ex.: "resposta.status")
 *  - operator: EQUALS (padrão), NOT_EQUALS, CONTAINS, GREATER_THAN, LESS_THAN, EXISTS
 *  - expected_value: valor de comparação
 *  - success_step / failure_step: número do passo (1 = primeiro) para onde ir; -1 encerra o fluxo;
 *    vazio = segue para o próximo passo.
 * (Legado: success_index / failure_index, começando em 0.)
 */
@Slf4j
@Component
public class DecisionAction implements Action {

    public static final int END_FLOW = -1;

    @Override
    public void execute(ExecutionContext context) {
        Map<String, Object> params = ActionSupport.params(context);

        String variable = ActionSupport.string(params, "variable", null);
        if (variable == null) {
            throw new IllegalArgumentException("O campo 'variable' é obrigatório para a ação DECISION.");
        }
        String operator = ActionSupport.string(params, "operator", "EQUALS").toUpperCase();
        String expected = ActionSupport.render(ActionSupport.string(params, "expected_value", ""), context);
        Object actual = ActionSupport.lookup(context, variable);

        boolean result = evaluate(operator, actual, expected);
        log.info("[DECISION] {} {} '{}' (atual: {}) => {}", variable, operator, expected, actual, result);
        context.put("decision_result", result);

        Integer target = result
                ? target(params, "success_step", "success_index")
                : target(params, "failure_step", "failure_index");
        if (target != null) {
            context.put(ExecutionContext.NEXT_STEP_KEY, target);
        }
    }

    private boolean evaluate(String operator, Object actual, String expected) {
        return switch (operator) {
            case "EQUALS" -> actual != null && compare(actual, expected) == 0;
            case "NOT_EQUALS" -> actual == null || compare(actual, expected) != 0;
            case "CONTAINS" -> actual != null && ActionSupport.stringify(actual).contains(expected);
            case "EXISTS" -> actual != null && !ActionSupport.stringify(actual).isBlank();
            case "GREATER_THAN" -> numeric(actual, expected) > 0;
            case "LESS_THAN" -> numeric(actual, expected) < 0;
            default -> throw new IllegalArgumentException("Operador desconhecido: " + operator);
        };
    }

    /** Compara como número quando os dois lados são numéricos; senão como texto. */
    private int compare(Object actual, String expected) {
        String left = ActionSupport.stringify(actual);
        try {
            return new BigDecimal(left.trim()).compareTo(new BigDecimal(expected.trim()));
        } catch (NumberFormatException e) {
            return left.equals(expected) ? 0 : 1;
        }
    }

    private int numeric(Object actual, String expected) {
        if (actual == null) {
            throw new IllegalArgumentException("A variável ainda não existe no contexto.");
        }
        try {
            return new BigDecimal(ActionSupport.stringify(actual).trim()).compareTo(new BigDecimal(expected.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("GREATER_THAN/LESS_THAN exigem valores numéricos.");
        }
    }

    private Integer target(Map<String, Object> params, String stepKey, String legacyIndexKey) {
        Object step = params.get(stepKey);
        if (step != null && !String.valueOf(step).isBlank()) {
            return parseStep(step);
        }
        Object legacy = params.get(legacyIndexKey);
        if (legacy != null && !String.valueOf(legacy).isBlank()) {
            Integer index = parseStep(legacy);
            return index == END_FLOW ? END_FLOW : index + 1;
        }
        return null;
    }

    private Integer parseStep(Object value) {
        String text = String.valueOf(value).trim();
        if (text.equalsIgnoreCase("end") || text.equalsIgnoreCase("fim")) {
            return END_FLOW;
        }
        try {
            return new BigDecimal(text).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Passo de destino inválido: " + text);
        }
    }

    @Override
    public ActionType getType() {
        return ActionType.DECISION;
    }
}
