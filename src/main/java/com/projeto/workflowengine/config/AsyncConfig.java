package com.projeto.workflowengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "workflowExecutor")
    public Executor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Mínimo de threads sempre ativas
        executor.setMaxPoolSize(10); // Máximo de threads simultâneas
        executor.setQueueCapacity(100); // Fila de espera
        executor.setThreadNamePrefix("WF-Exec-");
        executor.initialize();
        return executor;
    }
}
