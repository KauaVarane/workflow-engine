package com.projeto.workflowengine.core.action;

import com.projeto.workflowengine.config.WorkflowProperties;
import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executa UM comando SQL (SELECT/INSERT/UPDATE/DELETE) com parâmetros nomeados (:nome).
 * Parâmetros: query, queryParams (objeto).
 * SELECT guarda o resultado em "db_result"; escrita guarda "db_rows_affected".
 * Pode ser desligada com workflow.database-action-enabled=false.
 */
@Slf4j
@Component
public class DatabaseAction implements Action {

    private static final Set<String> ALLOWED_COMMANDS = Set.of("select", "insert", "update", "delete");

    private final EntityManager entityManager;
    private final WorkflowProperties properties;

    public DatabaseAction(EntityManager entityManager, WorkflowProperties properties) {
        this.entityManager = entityManager;
        this.properties = properties;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void execute(ExecutionContext context) {
        if (!properties.databaseActionEnabled()) {
            throw new IllegalStateException("A ação DATABASE está desativada (workflow.database-action-enabled=false).");
        }

        Map<String, Object> params = ActionSupport.params(context);
        String sql = ActionSupport.string(params, "query", null);
        if (sql == null) {
            throw new IllegalArgumentException("A query SQL é obrigatória para a ação DATABASE.");
        }
        sql = sql.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (sql.contains(";")) {
            throw new IllegalArgumentException("Use apenas um comando SQL por ação.");
        }
        String command = sql.split("\\s+", 2)[0].toLowerCase();
        if (!ALLOWED_COMMANDS.contains(command)) {
            throw new IllegalArgumentException("Comando SQL não permitido: " + command
                    + " (permitidos: SELECT, INSERT, UPDATE, DELETE)");
        }

        Query query = entityManager.createNativeQuery(sql);
        Object rawParams = params.get("queryParams");
        if (rawParams instanceof Map<?, ?> map) {
            ((Map<String, Object>) map).forEach(query::setParameter);
        }

        log.info("[DATABASE] {}", sql);
        if (command.equals("select")) {
            List<?> rows = query.getResultList();
            context.put("db_result", simplify(rows));
            context.put("db_rows", rows.size());
        } else {
            context.put("db_rows_affected", query.executeUpdate());
        }
    }

    /** 1 linha com 1 coluna vira valor simples; senão vira lista de linhas. */
    private Object simplify(List<?> rows) {
        List<Object> converted = new ArrayList<>();
        for (Object row : rows) {
            converted.add(row instanceof Object[] array ? Arrays.asList(array) : row);
        }
        if (converted.size() == 1 && !(converted.get(0) instanceof List)) {
            return converted.get(0);
        }
        return converted;
    }

    @Override
    public ActionType getType() {
        return ActionType.DATABASE;
    }
}
