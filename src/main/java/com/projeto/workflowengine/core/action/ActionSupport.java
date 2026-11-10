package com.projeto.workflowengine.core.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.workflowengine.core.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Funções comuns às ações: ler os parâmetros do passo, resolver {{variáveis}} do contexto
 * e gravar arquivos somente dentro da pasta permitida.
 */
public final class ActionSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([\\w.\\-]+)\\s*}}");

    private ActionSupport() {
    }

    /** Lê os parâmetros do passo atual (JSON em texto ou Map) e devolve um Map. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> params(ExecutionContext ctx) {
        Object raw = ctx.get(ExecutionContext.PARAMS_KEY);
        if (raw == null) {
            return new LinkedHashMap<>();
        }
        if (raw instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        String text = raw.toString().trim();
        if (text.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(text, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Parâmetros inválidos (esperado um objeto JSON): " + e.getOriginalMessage());
        }
    }

    /** Valor do parâmetro como texto, ou o padrão quando ausente/vazio. */
    public static String string(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }

    /** Busca uma variável no contexto. Aceita caminho com pontos: "resposta.dados.0.nome". */
    public static Object lookup(ExecutionContext ctx, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.trim().split("\\.");
        Object current = ctx.get(parts[0]);
        for (int i = 1; i < parts.length && current != null; i++) {
            String part = parts[i];
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof List<?> list) {
                try {
                    int index = Integer.parseInt(part);
                    current = (index >= 0 && index < list.size()) ? list.get(index) : null;
                } catch (NumberFormatException e) {
                    current = null;
                }
            } else {
                current = null;
            }
        }
        return current;
    }

    /** Troca {{variavel}} pelo valor do contexto. Variável inexistente vira texto vazio. */
    public static String render(String template, ExecutionContext ctx) {
        if (template == null || !template.contains("{{")) {
            return template;
        }
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            Object value = lookup(ctx, matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : stringify(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Aplica render() em todos os textos de um Map/List (usado no corpo das requisições HTTP). */
    @SuppressWarnings("unchecked")
    public static Object renderDeep(Object value, ExecutionContext ctx) {
        if (value instanceof String text) {
            return render(text, ctx);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            ((Map<String, Object>) map).forEach((k, v) -> copy.put(k, renderDeep(v, ctx)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(item -> copy.add(renderDeep(item, ctx)));
            return copy;
        }
        return value;
    }

    public static String stringify(Object value) {
        if (value instanceof Map || value instanceof List) {
            try {
                return MAPPER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                return String.valueOf(value);
            }
        }
        return String.valueOf(value);
    }

    /** Resolve um arquivo dentro da pasta permitida (impede "../" e caminhos absolutos). */
    public static Path sandboxPath(String outputDir, String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo é obrigatório.");
        }
        Path base = Paths.get(outputDir).toAbsolutePath().normalize();
        Path target = base.resolve(fileName).normalize();
        if (!target.startsWith(base) || target.equals(base)) {
            throw new IllegalArgumentException("Caminho fora da pasta permitida: " + fileName);
        }
        Files.createDirectories(target.getParent());
        return target;
    }
}
