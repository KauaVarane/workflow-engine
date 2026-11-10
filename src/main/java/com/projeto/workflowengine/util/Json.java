package com.projeto.workflowengine.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** Conversão entre o JSON guardado no banco (texto) e Map. */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {
    }

    /** Texto JSON -> Map. Vazio/nulo -> Map vazio. Lança IllegalArgumentException se não for um objeto JSON. */
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Parâmetros inválidos (esperado um objeto JSON): " + e.getOriginalMessage());
        }
    }

    /** Map -> texto JSON ("{}" quando vazio). */
    public static String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Parâmetros não puderam ser convertidos para JSON: " + e.getOriginalMessage());
        }
    }

    /** Versão tolerante usada ao LER do banco: se estiver corrompido devolve Map vazio. */
    public static Map<String, Object> toMapSafe(String json) {
        try {
            return toMap(json);
        } catch (IllegalArgumentException e) {
            return new LinkedHashMap<>();
        }
    }
}
