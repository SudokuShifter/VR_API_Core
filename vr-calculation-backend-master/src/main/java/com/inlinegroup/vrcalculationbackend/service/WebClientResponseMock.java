package com.inlinegroup.vrcalculationbackend.service;

import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;

public class WebClientResponseMock {

    public static Mono<ClientResponse> createMockResponse(ClientRequest request) {
        String mockJson = "{\"message\": \"This is a mock response\"}";

        DataBuffer buffer = new DefaultDataBufferFactory().wrap(mockJson.getBytes(StandardCharsets.UTF_8));

        return Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(Flux.just(buffer)) 
                .build()
        );
    }
}