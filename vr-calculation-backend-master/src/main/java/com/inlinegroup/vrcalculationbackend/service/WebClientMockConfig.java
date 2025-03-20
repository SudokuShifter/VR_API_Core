package com.inlinegroup.vrcalculationbackend.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientMockConfig {

    @Bean
    @Primary
    public WebClient mockWebClient() {
        ExchangeFunction mockExchangeFunction = request -> WebClientResponseMock.createMockResponse(request);
        
        return WebClient.builder()
            .exchangeFunction(mockExchangeFunction)
            .build();
    }
}