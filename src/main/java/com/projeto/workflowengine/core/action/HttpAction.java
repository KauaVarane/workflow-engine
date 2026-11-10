package com.projeto.workflowengine.core.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

/**
 * Chama uma API externa.
 * Parâmetros: url, method (GET/POST/PUT/DELETE), headers (objeto), body (objeto ou texto),
 * result_key (onde guardar a resposta; padrão "http_response"). Aceita {{variáveis}}.
 * Também grava "<result_key>_status" com o código HTTP.
 */
@Slf4j
@Component
public class HttpAction implements Action {

    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpAction() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(ExecutionContext context) {
        Map<String, Object> params = ActionSupport.params(context);

        String url = ActionSupport.render(ActionSupport.string(params, "url", null), context);
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new IllegalArgumentException("A URL é obrigatória e deve começar com http:// ou https://");
        }
        String method = ActionSupport.string(params, "method", "GET").toUpperCase();
        if (!ALLOWED_METHODS.contains(method)) {
            throw new IllegalArgumentException("Método HTTP não suportado: " + method);
        }
        String resultKey = ActionSupport.string(params, "result_key", "http_response");

        HttpHeaders headers = new HttpHeaders();
        Object rawHeaders = params.get("headers");
        if (rawHeaders instanceof Map<?, ?> map) {
            ((Map<String, Object>) map).forEach((k, v) ->
                    headers.add(k, ActionSupport.render(String.valueOf(v), context)));
        }

        Object body = "GET".equals(method) ? null : ActionSupport.renderDeep(params.get("body"), context);
        if (body != null && !headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        log.info("[HTTP] {} {}", method, url);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.valueOf(method), new HttpEntity<>(body, headers), String.class);

        context.put(resultKey, parseBody(response.getBody()));
        context.put(resultKey + "_status", response.getStatusCode().value());
        log.info("[HTTP] Resposta {} salva em '{}'", response.getStatusCode().value(), resultKey);
    }

    /** Se a resposta for JSON vira Map/List (acessível por {{chave.campo}}); senão fica como texto. */
    private Object parseBody(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception e) {
            return raw;
        }
    }

    @Override
    public ActionType getType() {
        return ActionType.HTTP;
    }
}
