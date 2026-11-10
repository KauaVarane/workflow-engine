package com.projeto.workflowengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa a API de ponta a ponta: CRUD + execução real do motor (com DECISION, variáveis e retry). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    private JsonNode call(org.springframework.test.web.servlet.RequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? null : mapper.readTree(body);
    }

    private JsonNode postJson(String url, String json, int expected) throws Exception {
        return call(post(url).contentType(MediaType.APPLICATION_JSON).content(json), expected);
    }

    private JsonNode waitForFinish(long logId) throws Exception {
        for (int i = 0; i < 60; i++) {
            JsonNode log = call(get("/api/logs/" + logId), 200);
            if (!"RUNNING".equals(log.get("status").asText())) {
                return log;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Execução não terminou a tempo");
    }

    @Test
    void crudCompleto() throws Exception {
        JsonNode created = postJson("/api/workflows", """
                {"name":"  Meu fluxo ","triggerType":"SCHEDULE","intervalMinutes":5,
                 "actions":[{"actionType":"LOG","parameters":{"message":"oi"}},
                            {"actionType":"NOTIFICATION","parameters":{"message":"fim"},"maxRetries":1}]}
                """, 201);
        long id = created.get("id").asLong();
        assertThat(created.get("name").asText()).isEqualTo("Meu fluxo");
        assertThat(created.get("intervalMinutes").asInt()).isEqualTo(5);
        assertThat(created.get("actions")).hasSize(2);
        assertThat(created.get("actions").get(1).get("sequenceOrder").asInt()).isEqualTo(2);

        JsonNode updated = call(put("/api/workflows/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Renomeado","triggerType":"HTTP","actions":[{"actionType":"LOG","parameters":{"message":"só um"}}]}
                """), 200);
        assertThat(updated.get("name").asText()).isEqualTo("Renomeado");
        assertThat(updated.get("intervalMinutes").isNull()).isTrue();
        assertThat(updated.get("actions")).hasSize(1);

        JsonNode off = call(patch("/api/workflows/" + id + "/active").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"), 200);
        assertThat(off.get("active").asBoolean()).isFalse();

        JsonNode copy = postJson("/api/workflows/" + id + "/duplicate", "{}", 201);
        assertThat(copy.get("name").asText()).contains("Renomeado");
        assertThat(copy.get("active").asBoolean()).isFalse();

        assertThat(call(get("/api/workflows"), 200).size()).isGreaterThanOrEqualTo(2);
        call(delete("/api/workflows/" + id), 204);
        call(get("/api/workflows/" + id), 404);
    }

    @Test
    void validacoesRetornam400() throws Exception {
        postJson("/api/workflows", "{\"name\":\"\",\"triggerType\":\"HTTP\"}", 400);
        postJson("/api/workflows", "{\"name\":\"x\",\"triggerType\":\"INEXISTENTE\"}", 400);
        postJson("/api/workflows", """
                {"name":"x","triggerType":"HTTP","actions":[{"actionType":"HTTP","parameters":{"url":"ftp://x"}}]}
                """, 400);
        postJson("/api/workflows", """
                {"name":"x","triggerType":"HTTP","actions":[{"actionType":"DECISION","parameters":{"variable":"a","success_step":9}}]}
                """, 400);
    }

    @Test
    void execucaoComDecisaoVariaveisERetry() throws Exception {
        // 1 LOG grava last_log_message | 2 DECISION compara com o contexto | 3 FILE (pulado) | 4 NOTIFICATION usa {{...}}
        JsonNode wf = postJson("/api/workflows", """
                {"name":"Fluxo de teste","triggerType":"HTTP","actions":[
                  {"actionType":"LOG","parameters":{"message":"Olá {{workflowName}}"}},
                  {"actionType":"DECISION","parameters":{"variable":"last_log_message","operator":"CONTAINS",
                       "expected_value":"Fluxo","success_step":4,"failure_step":3}},
                  {"actionType":"FILE","parameters":{"fileName":"nao-deve-existir.txt","content":"x"}},
                  {"actionType":"NOTIFICATION","parameters":{"message":"Recebi: {{last_log_message}}"}}]}
                """, 201);
        long wfId = wf.get("id").asLong();

        long logId = postJson("/api/workflows/" + wfId + "/execute", "{}", 202).get("logId").asLong();
        JsonNode log = waitForFinish(logId);
        assertThat(log.get("status").asText()).isEqualTo("SUCCESS");

        JsonNode steps = call(get("/api/logs/" + logId + "/details"), 200);
        assertThat(steps).hasSize(3); // passo 3 foi pulado pela decisão
        assertThat(steps.get(1).get("actionType").asText()).isEqualTo("DECISION");
        assertThat(steps.get(1).get("message").asText()).contains("passo 4");
        assertThat(steps.get(2).get("contextSnapshot").asText()).contains("Recebi: Ol");

        JsonNode stats = call(get("/api/stats"), 200);
        assertThat(stats.get("success").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void falhaGravaStatusFailedEMensagemLonga() throws Exception {
        // HTTP em porta fechada: falha depois de 1 retry; a mensagem de erro não pode deixar o log em RUNNING
        JsonNode wf = postJson("/api/workflows", """
                {"name":"Fluxo que falha","triggerType":"HTTP","actions":[
                  {"actionType":"HTTP","maxRetries":1,"parameters":{"url":"http://localhost:1/nada"}}]}
                """, 201);
        long logId = postJson("/api/workflows/" + wf.get("id").asLong() + "/execute", "{}", 202).get("logId").asLong();

        JsonNode log = waitForFinish(logId);
        assertThat(log.get("status").asText()).isEqualTo("FAILED");
        assertThat(log.get("message").asText()).isNotBlank();
        assertThat(call(get("/api/logs/" + logId + "/details"), 200).get(0).get("status").asText()).isEqualTo("FAILED");

        // excluir o workflow remove também os logs e os passos (antes dava erro de chave estrangeira)
        call(delete("/api/workflows/" + wf.get("id").asLong()), 204);
        call(get("/api/logs/" + logId), 404);
    }

    @Test
    void arquivoNaoSaiDaPastaPermitida() throws Exception {
        JsonNode wf = postJson("/api/workflows", """
                {"name":"Fuga de pasta","triggerType":"HTTP","actions":[
                  {"actionType":"FILE","parameters":{"fileName":"../../fora.txt","content":"x"}}]}
                """, 201);
        long logId = postJson("/api/workflows/" + wf.get("id").asLong() + "/execute", "{}", 202).get("logId").asLong();
        JsonNode log = waitForFinish(logId);
        assertThat(log.get("status").asText()).isEqualTo("FAILED");
        assertThat(log.get("message").asText()).contains("fora da pasta");
    }

    @Test
    void acaoDatabaseAceitaSomenteUmComandoDml() throws Exception {
        JsonNode wf = postJson("/api/workflows", """
                {"name":"SQL","triggerType":"HTTP","actions":[
                  {"actionType":"DATABASE","parameters":{"query":"select count(*) from workflow"}},
                  {"actionType":"DATABASE","parameters":{"query":"drop table workflow"}}]}
                """, 201);
        long logId = postJson("/api/workflows/" + wf.get("id").asLong() + "/execute", "{}", 202).get("logId").asLong();
        JsonNode log = waitForFinish(logId);
        assertThat(log.get("status").asText()).isEqualTo("FAILED");
        JsonNode steps = call(get("/api/logs/" + logId + "/details"), 200);
        assertThat(steps.get(0).get("status").asText()).isEqualTo("SUCCESS");
        assertThat(steps.get(1).get("message").asText()).contains("permitido");
    }
}
