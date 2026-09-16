package br.com.marcosbassetto.concursos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Executor do processamento de edital.
 *
 * O pool é pequeno e com fila limitada de propósito: extrair PDF é CPU-bound
 * e cada tarefa abre transação no banco. Sem limite, uma rajada de uploads
 * derrubaria o app em vez de apenas enfileirar.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EDITAL_EXECUTOR = "editalExecutor";

    @Bean(name = EDITAL_EXECUTOR)
    public Executor editalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("edital-");
        executor.initialize();
        return executor;
    }
}
