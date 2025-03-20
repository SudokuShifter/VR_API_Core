package com.inlinegroup.vrcalculationbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        Executor executorService = Executors.newCachedThreadPool(); // Получить ядро ЦП сервера
        executor.setCorePoolSize(30); // Размер основного пула
//        executor.setMaxPoolSize(100); // Максимальное количество потоков
//        executor.setQueueCapacity(1000); // Уровень очереди
//        executor.setKeepAliveSeconds(1000); // Время простоя потока
//        executor.setThreadNamePrefix("task-asyn"); // Имя префикса потока
//        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy()); // Настройка политики отклонения

        return executor;
    }
}
