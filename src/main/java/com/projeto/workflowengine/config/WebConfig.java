package com.projeto.workflowengine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * O front-end (React) é servido pelo próprio Spring a partir de /static.
 * As rotas do React Router precisam devolver o index.html quando abertas direto no navegador.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/workflows").setViewName("forward:/index.html");
        registry.addViewController("/executions").setViewName("forward:/index.html");
    }
}
